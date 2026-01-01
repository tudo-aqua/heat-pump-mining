// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.options.varargValues
import com.github.ajalt.clikt.parameters.types.defaultStdout
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.outputStream
import com.github.ajalt.clikt.parameters.types.path
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import java.util.Optional
import kotlin.jvm.optionals.getOrNull
import kotlin.time.DurationUnit.DAYS
import kotlin.time.DurationUnit.HOURS
import kotlin.time.DurationUnit.MICROSECONDS
import kotlin.time.DurationUnit.MILLISECONDS
import kotlin.time.DurationUnit.MINUTES
import kotlin.time.DurationUnit.NANOSECONDS
import kotlin.time.DurationUnit.SECONDS
import tools.aqua.hpm.data.toRows
import tools.aqua.hpm.util.summarizeResults

class SummarizeResults : CliktCommand() {
  val basedir by option("-d", "--directory").path(mustExist = true, canBeFile = false).required()
  val cases by option("-c", "--cases").varargValues().required()
  val rounds by option("-r", "--rounds").int().required()
  val setups by option("-s", "--setups").varargValues().required()
  val learningSizes by option("-l", "--learning-sizes").int().varargValues().required()

  val timeUnit by
      option()
          .switch(
              "--iso-duration" to Optional.empty(),
              "--nanoseconds" to Optional.of(NANOSECONDS),
              "--microseconds" to Optional.of(MICROSECONDS),
              "--milliseconds" to Optional.of(MILLISECONDS),
              "--seconds" to Optional.of(SECONDS),
              "--minutes" to Optional.of(MINUTES),
              "--hours" to Optional.of(HOURS),
              "--days" to Optional.of(DAYS),
          )
          .default(Optional.empty())

  val output by option("-o", "--output").outputStream(truncateExisting = true).defaultStdout()

  override fun run() {
    csvWriter { lineTerminator = "\n" }
        .open(output) {
          writeRows(
              summarizeResults(basedir, cases, rounds, setups, learningSizes)
                  .toRows(timeUnit.getOrNull())
          )
        }
  }
}
