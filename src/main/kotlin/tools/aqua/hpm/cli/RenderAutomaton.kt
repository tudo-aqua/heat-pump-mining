// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.path
import kotlin.io.path.createParentDirectories
import tools.aqua.hpm.automata.dfptioParser
import tools.aqua.hpm.util.component1
import tools.aqua.hpm.util.component2
import tools.aqua.hpm.util.writeRenderDot

class RenderAutomaton : CliktCommand() {
  val input by
      option("-i", "--input")
          .path(mustExist = true, canBeDir = false, mustBeReadable = true)
          .required()
  val output by option("-o", "--output").path(canBeDir = false).required()

  val fullColorMin by
      option("-f", "--full-color-min").double().default(0.0).check { it in 0.0..1.0 }
  val renderMin by option("-r", "--render-min").double().default(0.0).check { it in 0.0..1.0 }

  override fun run() {
    val (alphabet, automaton) = dfptioParser.readModel(input.toFile())
    automaton.writeRenderDot(output.createParentDirectories(), alphabet, fullColorMin, renderMin)
  }
}
