// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.varargValues
import com.github.ajalt.clikt.parameters.types.defaultStdout
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.outputStream
import com.github.ajalt.clikt.parameters.types.path
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import tools.aqua.hpm.automata.dfptioParser
import tools.aqua.hpm.data.HittingPredictionList
import tools.aqua.hpm.data.HittingPredictionResult
import tools.aqua.hpm.data.rooted
import tools.aqua.hpm.data.toRows
import tools.aqua.hpm.data.toTimedIOTrace
import tools.aqua.hpm.validation.hittingTimeDelta
import tools.aqua.rereso.log.LogArchive
import tools.aqua.rereso.util.smartDecode

class ComputeHittingTimes : CliktCommand() {
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

  val parallel by option("-p", "--parallel").flag("--single-threaded")
  val events by option("-e", "--events").varargValues().required()
  val samples by option("-s", "--samples").int().check { it > 0 }

  override fun run() {
    val data = dfptioParser.readModel(dot.toFile())
    val automaton = data.model
    val alphabet = data.alphabet

    val archive =
        input.smartDecode<LogArchive>().let {
          if (initOutput != null) it.rooted(initOutput!!) else it
        }

    val inputSymbol = alphabet.single()

    val traces = archive.logs.map { it.toTimedIOTrace(inputSymbol) }
    val deltas = automaton.hittingTimeDelta(traces, samples, inputSymbol, events.toSet(), parallel)

    val result =
        if (deltas == null) {
              archive.logs.map { HittingPredictionResult("${it.name}", false, emptyList()) }
            } else {
              (archive.logs zip deltas.values).map { (log, hittingInformation) ->
                HittingPredictionResult(
                    "${log.name}",
                    true,
                    hittingInformation.map { it?.hittingTime },
                )
              }
            }
            .let(::HittingPredictionList)

    csvWriter { lineTerminator = "\n" }.open(output) { writeRows(result.toRows()) }
  }
}
