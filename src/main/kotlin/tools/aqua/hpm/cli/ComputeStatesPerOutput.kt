// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.defaultStdout
import com.github.ajalt.clikt.parameters.types.outputStream
import com.github.ajalt.clikt.parameters.types.path
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import tools.aqua.hpm.automata.dfptioParser
import tools.aqua.hpm.automata.getStatesWithOutput
import tools.aqua.hpm.automata.outputs

class ComputeStatesPerOutput : CliktCommand() {
  val dot by
      option("-a", "--automaton")
          .path(mustExist = true, canBeDir = false, mustBeReadable = true)
          .required()
  val output by option("-o", "--output").outputStream(truncateExisting = true).defaultStdout()

  override fun run() {
    val data = dfptioParser.readModel(dot.toFile())
    val automaton = data.model
    val alphabet = data.alphabet

    csvWriter { lineTerminator = "\n" }
        .open(output) {
          writeRow("output", "nStates")
          automaton.outputs.forEach { output ->
            writeRow(output, automaton.getStatesWithOutput(output).size)
          }
        }
  }
}
