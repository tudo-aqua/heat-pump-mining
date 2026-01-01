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
import com.github.ajalt.clikt.parameters.types.path
import kotlin.io.path.createParentDirectories
import tools.aqua.hpm.merge.SplitMode
import tools.aqua.hpm.merge.selectAndMerge
import tools.aqua.rereso.log.LogArchive
import tools.aqua.rereso.util.smartDecode
import tools.aqua.rereso.util.smartEncode

class SelectAndMerge : CliktCommand() {
  val input by
      option("-i", "--input")
          .path(mustExist = true, canBeDir = false, mustBeReadable = true)
          .required()
  val output by option("-o", "--output").path(canBeDir = false).required()
  val events by option("-e", "--events").varargValues().default(emptyList())
  val splitEvent by option("-s", "--split-event").required()
  val splitMode by
      option()
          .switch(
              "-f" to SplitMode.ONLY_FIRST,
              "--use-only-first" to SplitMode.ONLY_FIRST,
              "-a" to SplitMode.EACH_FROM_START,
              "--use-all-overlapping" to SplitMode.EACH_FROM_START,
              "-n" to SplitMode.EACH_NON_OVERLAPPING,
              "--use-all-non-overlapping" to SplitMode.EACH_NON_OVERLAPPING,
          )
          .default(SplitMode.EACH_NON_OVERLAPPING)

  override fun run() {
    val archive = input.smartDecode<LogArchive>()
    val result = archive.selectAndMerge(events.toSet() + splitEvent, splitEvent, splitMode)
    output.createParentDirectories().smartEncode(result)
  }
}
