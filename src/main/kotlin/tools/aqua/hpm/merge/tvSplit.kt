// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.merge

import kotlin.math.roundToInt
import kotlin.random.Random
import tools.aqua.hpm.util.prefixes
import tools.aqua.hpm.util.subListFrom
import tools.aqua.hpm.util.subListTo
import tools.aqua.rereso.log.LogArchive
import tools.aqua.rereso.log.Split.TRAINING
import tools.aqua.rereso.log.Split.VALIDATION
import tools.aqua.rereso.util.mapToSet

data class TrainingAndValidation(
    val trainingArchives: Collection<LogArchive>,
    val validationArchive: LogArchive
)

fun LogArchive.validationSplit(
    rounds: Int,
    validationShare: Double = 0.5,
    seed: Long = 0
): Collection<TrainingAndValidation> {
  val random = Random(seed)
  require(rounds > 0) { "rounds must be at least one, is $rounds" }
  require(validationShare > 0 && validationShare < 1) {
    "validation share must be strictly between 0 and 1, is $validationShare"
  }

  val nValidation = (logs.size * validationShare).roundToInt().coerceIn(1, logs.size - 1)

  return List(rounds) {
    val logsShuffled = logs.shuffled(random)

    val validationLogs =
        logsShuffled.subListTo(nValidation).mapToSet { it.copy(split = VALIDATION) }
    val validation = copy(name = "$name (validation)", logs = validationLogs)

    val trainingLogs =
        logsShuffled
            .subListFrom(nValidation)
            .prefixes
            .filter { it.isNotEmpty() }
            .map { logs -> logs.mapToSet { it.copy(split = TRAINING) } }
    val training = trainingLogs.map { copy(name = "$name (training)", logs = it) }

    TrainingAndValidation(training, validation)
  }
}
