// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.nullableFlag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.varargValues
import com.github.ajalt.clikt.parameters.types.path
import kotlin.io.path.createParentDirectories
import kotlin.time.Duration
import kotlinx.datetime.LocalTime
import tools.aqua.hpm.merge.selectAndMergeFromKey
import tools.aqua.hpm.merge.selectAndMergeSingle
import tools.aqua.hpm.merge.selectAndMergeWithStartOfDay
import tools.aqua.hpm.merge.selectAndMergeWithTimeInterval
import tools.aqua.rereso.log.LogArchive
import tools.aqua.rereso.util.smartDecode
import tools.aqua.rereso.util.smartEncode

class SelectAndMerge : CliktCommand() {
  sealed interface Mode

  data object Single : Mode

  data class KeyEvent(val event: String, val keepPrefix: Boolean) : Mode

  data class SOD(val sod: LocalTime) : Mode

  data class Interval(val interval: Duration) : Mode

  val input by
      option("-i", "--input")
          .path(mustExist = true, canBeDir = false, mustBeReadable = true)
          .required()
  val output by option("-o", "--output").path(canBeDir = false).required()
  val events by option("-e", "--events").varargValues().default(emptyList())
  val splitMode by
      mutuallyExclusiveOptions(
              option("--single").nullableFlag().convert { Single },
              option("--key-event").convert { KeyEvent(it, false) },
              option("--key-event-with-prefix").convert { KeyEvent(it, true) },
              option("--start-of-day-utc").convert { SOD(LocalTime.parse(it)) },
              option("--interval").convert { Interval(Duration.parse(it)) },
          )
          .required()

  override fun run() {
    val archive = input.smartDecode<LogArchive>()
    val eventSet = events.toSet()
    val result =
        when (val mode = splitMode) {
          Single -> archive.selectAndMergeSingle(eventSet)
          is KeyEvent ->
              archive.selectAndMergeFromKey(eventSet + mode.event, mode.event, mode.keepPrefix)
          is SOD -> archive.selectAndMergeWithStartOfDay(eventSet, mode.sod)
          is Interval -> archive.selectAndMergeWithTimeInterval(eventSet, mode.interval)
        }
    output.createParentDirectories().smartEncode(result)
  }
}
