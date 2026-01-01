// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.clikt.parameters.types.path
import kotlin.io.path.createParentDirectories
import kotlin.random.Random
import kotlin.time.DurationUnit.DAYS
import kotlin.time.DurationUnit.HOURS
import kotlin.time.DurationUnit.MICROSECONDS
import kotlin.time.DurationUnit.MILLISECONDS
import kotlin.time.DurationUnit.MINUTES
import kotlin.time.DurationUnit.NANOSECONDS
import kotlin.time.DurationUnit.SECONDS
import tools.aqua.hpm.automata.dfptioParser
import tools.aqua.hpm.data.toLog
import tools.aqua.hpm.timing.exponential
import tools.aqua.hpm.timing.normal
import tools.aqua.hpm.traces.generateTraces
import tools.aqua.rereso.log.LogArchive
import tools.aqua.rereso.util.smartEncode

class GenerateTraces : CliktCommand() {
  val dot by
      option("-a", "--automaton")
          .path(mustExist = true, canBeDir = false, mustBeReadable = true)
          .required()
  val output by option("-o", "--output").path(canBeDir = false).required()
  val nTraces by option("-n", "--traces").int().default(1).check { it >= 1 }
  val meanLength by option("-l", "--mean-length").double().default(100.0).check { it > 0 }
  val lengthSD by option("-L", "--length-stddev").double().default(0.0).check { it >= 0 }
  val distribution by
      option()
          .switch(
              "--normal-distribution" to normal,
              "--exponential-distribution" to exponential,
          )
          .required()
  val precision by
      option()
          .switch(
              "--nanoseconds" to NANOSECONDS,
              "--microseconds" to MICROSECONDS,
              "--milliseconds" to MILLISECONDS,
              "--seconds" to SECONDS,
              "--minutes" to MINUTES,
              "--hours" to HOURS,
              "--days" to DAYS,
          )
          .required()
  val seed by option("-s", "--seed").long().default(0)

  override fun run() {
    val data = dfptioParser.readModel(dot.toFile())
    val automaton = data.model
    val alphabet = data.alphabet

    val random = Random(seed)

    val result =
        LogArchive(
            "Traced from $dot",
            automaton
                .generateTraces(
                    nTraces,
                    meanLength,
                    lengthSD,
                    alphabet.single(),
                    distribution,
                    precision,
                    random,
                )
                .withIndex()
                .mapTo(mutableSetOf()) { (idx, trace) -> trace.toLog().copy(name = "Trace $idx") },
        )
    output.createParentDirectories().smartEncode(result)
  }
}
