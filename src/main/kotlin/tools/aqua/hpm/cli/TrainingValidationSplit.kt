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
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.clikt.parameters.types.path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.name
import tools.aqua.hpm.merge.validationSplit
import tools.aqua.rereso.log.LogArchive
import tools.aqua.rereso.util.smartDecode
import tools.aqua.rereso.util.smartEncode

class TrainingValidationSplit : CliktCommand("tv-split") {
  val input by
      option("-i", "--input")
          .path(mustExist = true, canBeDir = false, mustBeReadable = true)
          .required()
  val output by
      option("-o", "--output-template").path(canBeDir = false).required().check {
        it.name.contains("{round}") && it.name.contains("{set}")
      }
  val rounds by option("-r", "--rounds").int().default(1)
  val validationShare by
      option("-v", "--validation-share").double().default(0.5).check { it > 0 && it < 1 }
  val seed by option("-s", "--seed").long().default(0)

  override fun run() {
    val archive = input.smartDecode<LogArchive>()
    val result = archive.validationSplit(rounds, validationShare, seed)
    result.withIndex().forEach { (round, tv) ->
      val roundName = output.name.replace("{round}", "$round")
      val (training, validation) = tv
      training.forEach {
        output
            .resolveSibling(roundName.replace("{set}", "training-${it.logs.size}"))
            .createParentDirectories()
            .smartEncode(it)
      }
      output
          .resolveSibling(roundName.replace("{set}", "validation"))
          .createParentDirectories()
          .smartEncode(validation)
    }
  }
}
