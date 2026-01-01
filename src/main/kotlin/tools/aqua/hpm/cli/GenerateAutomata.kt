// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.varargValues
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.clikt.parameters.types.path
import kotlin.io.path.div
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.random.Random
import kotlin.time.Duration
import tools.aqua.hpm.automata.DFTPIOAGenerator
import tools.aqua.hpm.util.ComparableUnit
import tools.aqua.hpm.util.writeDot

class GenerateAutomata : CliktCommand() {
  val output by option("-o", "--output").path().required()
  val nAutomata by option("-n", "--automata").int().default(1).check { it >= 1 }
  val alphabet by option("-a", "--alphabet").varargValues().required()
  val minSize by option("-q", "--min-size").int().required().check { it >= alphabet.size }
  val maxSize by option("-Q", "--max-size").int().required().check { it >= minSize }
  val minExitTime by
      option("-t", "--min-exit-time").convert { Duration.Companion.parse(it) }.required()
  val maxExitTime by
      option("-T", "--max-exit-time")
          .convert { Duration.Companion.parse(it) }
          .required()
          .check { it >= minExitTime }
  val seed by option("-s", "--seed").long().default(0)

  override fun run() {
    val generator =
        DFTPIOAGenerator(
            minSize..maxSize,
            ComparableUnit,
            alphabet,
            minExitTime..maxExitTime,
            Random(seed),
        )

    generator.asSequence().take(nAutomata).forEachIndexed { idx, automaton ->
      val name = output.parent / "${output.nameWithoutExtension}-$idx.${output.extension}"
      automaton.writeDot(name, setOf(ComparableUnit))
    }
  }
}
