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
import kotlin.io.path.useLines
import tools.aqua.hpm.subcsl.requireChronological
import tools.aqua.hpm.subcsl.satisfiesSubCSL
import tools.aqua.hpm.subcsl.toSubCSLFormula
import tools.aqua.rereso.log.LogArchive
import tools.aqua.rereso.util.smartDecode

class EvaluateSubCSL : CliktCommand("evaluate-sub-csl") {
  val formulas by
      option("-f", "--sub-csl")
          .path(mustExist = true, canBeDir = false, mustBeReadable = true)
          .required()
  val traces by
      option("-t", "--traces")
          .path(mustExist = true, canBeDir = false, mustBeReadable = true)
          .required()
  val output by option("-o", "--output").outputStream(truncateExisting = true).defaultStdout()

  override fun run() {
    val traceDB = traces.smartDecode<LogArchive>()

    formulas.useLines { lines ->
      val global =
          lines
              .map { line ->
                val subCSL = line.toSubCSLFormula()
                val nSatisfied =
                    traceDB.logs.count { it.requireChronological() satisfiesSubCSL subCSL }
                println("$nSatisfied / ${traceDB.logs.size}")
                nSatisfied.toDouble() / traceDB.logs.size.toDouble()
              }
              .average()
      println("GLOBAL: $global")
    }
  }
}
