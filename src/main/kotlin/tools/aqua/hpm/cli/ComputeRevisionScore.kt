// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.types.defaultStdout
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.outputStream
import com.github.ajalt.clikt.parameters.types.path
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import java.util.stream.Collectors
import tools.aqua.hpm.automata.dfptioParser
import tools.aqua.hpm.cli.ComputeRevisionScore.Mode.GLOBAL
import tools.aqua.hpm.cli.ComputeRevisionScore.Mode.SINGLE_BEST
import tools.aqua.hpm.cli.ComputeRevisionScore.Mode.SINGLE_ROOTED
import tools.aqua.hpm.data.rooted
import tools.aqua.hpm.data.toTimedIOTrace
import tools.aqua.hpm.util.standardDeviation
import tools.aqua.hpm.validation.computeBestRevisionScore
import tools.aqua.hpm.validation.computeGlobalRevisionScore
import tools.aqua.hpm.validation.computeRootedRevisionScore
import tools.aqua.rereso.log.LogArchive
import tools.aqua.rereso.util.smartDecode

class ComputeRevisionScore : CliktCommand() {
  enum class Mode {
    SINGLE_BEST,
    SINGLE_ROOTED,
    GLOBAL,
  }

  val dot by
      option("-a", "--automaton")
          .path(mustExist = true, canBeDir = false, mustBeReadable = true)
          .required()
  val input by
      option("-i", "--input")
          .path(mustExist = true, canBeDir = false, mustBeReadable = true)
          .required()
  val output by option("-o", "--output").outputStream(truncateExisting = true).defaultStdout()

  val initOutput by option("-I", "--init")
  val mode by
      option()
          .switch(
              "--single-best" to SINGLE_BEST,
              "--single-rooted" to SINGLE_ROOTED,
              "--global" to GLOBAL,
          )
          .default(GLOBAL)
  val parallel by option("-p", "--parallel").flag("--single-threaded")

  val frequencyWeight by
      option("-f", "--frequency-weight").double().default(0.5).check { it in 0.0..1.0 }

  override fun run() {
    val data = dfptioParser.readModel(dot.toFile())
    val automaton = data.model
    val alphabet = data.alphabet

    val archive =
        input.smartDecode<LogArchive>().let {
          if (initOutput != null) it.rooted(initOutput!!) else it
        }

    val inputSymbol = alphabet.single()

    val result =
        if (mode != GLOBAL) {
          val singleResults =
              archive.logs
                  .let { if (parallel) it.parallelStream() else it.stream() }
                  .map { log ->
                    val trace = log.toTimedIOTrace(inputSymbol)
                    val score =
                        if (mode == SINGLE_BEST)
                            automaton.computeBestRevisionScore(alphabet, trace, frequencyWeight)
                        else automaton.computeRootedRevisionScore(alphabet, trace, frequencyWeight)
                    log.name to score
                  }
                  .collect(Collectors.toList())
          singleResults +
              listOf(
                  "<average>" to singleResults.map { (_, score) -> score }.average(),
                  "<stddev>" to singleResults.map { (_, score) -> score }.standardDeviation(),
              )
        } else {
          val result =
              automaton.computeGlobalRevisionScore(
                  alphabet,
                  archive.logs.map { it.toTimedIOTrace(inputSymbol) },
                  frequencyWeight,
              )
          listOf("<global>" to result)
        }

    csvWriter { lineTerminator = "\n" }
        .open(output) {
          writeRow("log", "score")
          result.forEach { (name, value) -> writeRow(name, value) }
        }
  }
}
