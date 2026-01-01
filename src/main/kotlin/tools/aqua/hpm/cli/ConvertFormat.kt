// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import kotlin.io.path.createParentDirectories
import tools.aqua.hpm.data.importSplitNibe2Data
import tools.aqua.rereso.util.smartEncode

class ConvertFormat : CliktCommand() {
  val input by
      option("-i", "--input")
          .path(mustExist = true, canBeDir = false, mustBeReadable = true)
          .required()
  val output by option("-o", "--output").path(canBeDir = false).required()

  override fun run() {
    val archive = importSplitNibe2Data(input)
    output.createParentDirectories().smartEncode(archive)
  }
}
