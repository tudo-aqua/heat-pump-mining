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
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createParentDirectories
import kotlin.random.Random
import kotlin.time.Duration
import tools.aqua.hpm.subcsl.SubCSLFormulaGenerator

class GenerateSubCSL : CliktCommand("generate-sub-csl") {
  val output by option("-o", "--output").path(canBeDir = false).required()
  val nFormulas by option("-n", "--formulas").int().default(1).check { it >= 1 }
  val alphabet by option("-a", "--alphabet").varargValues().required()
  val minDuration by
      option("-d", "--min-duration").convert { Duration.Companion.parse(it) }.required()
  val maxDuration by
      option("-D", "--max-duration")
          .convert { Duration.Companion.parse(it) }
          .required()
          .check { it >= minDuration }
  val seed by option("-s", "--seed").long().default(0)

  override fun run() {
    val generator = SubCSLFormulaGenerator(alphabet, minDuration..maxDuration, Random(seed))

    output.createParentDirectories().bufferedWriter().use { out ->
      generator.asSequence().take(nFormulas).forEach { out.appendLine(it.toString()) }
    }
  }
}
