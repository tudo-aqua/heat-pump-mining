// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.data

import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import net.automatalib.automaton.Automaton
import org.apache.commons.math3.stat.StatUtils.mean
import tools.aqua.hpm.data.EvaluationResult.Companion.NA
import tools.aqua.hpm.data.HittingPredictionResult.Companion.DNF
import tools.aqua.hpm.util.allSame
import tools.aqua.hpm.util.average
import tools.aqua.hpm.util.component10
import tools.aqua.hpm.util.component11
import tools.aqua.hpm.util.component12
import tools.aqua.hpm.util.component13
import tools.aqua.hpm.util.component14
import tools.aqua.hpm.util.component6
import tools.aqua.hpm.util.component7
import tools.aqua.hpm.util.component8
import tools.aqua.hpm.util.component9
import tools.aqua.hpm.util.countTransitions
import tools.aqua.hpm.util.standardDeviation
import tools.aqua.hpm.util.standardDeviationOrNull
import tools.aqua.hpm.util.subListFrom
import tools.aqua.rereso.util.implies

data class RevisionScoreList(val results: List<RevisionScoreResult>) :
    List<RevisionScoreResult> by results {
  companion object {
    const val LOG_NAME: String = "log-name"
    const val REVISION_SCORE: String = "revision-score"
    const val PATH_LIKELINESS: String = "path-likeliness"
    val header: List<String> = listOf(LOG_NAME, REVISION_SCORE, PATH_LIKELINESS)
  }

  init {
    require(results.isNotEmpty())
  }
}

fun RevisionScoreList.toRows(): List<List<String>> =
    listOf(RevisionScoreList.header) + results.map { it.toRow() }

fun List<List<String>>.toRevisionScoreList(): RevisionScoreList {
  require(first() == RevisionScoreList.header)
  return RevisionScoreList(subListFrom(1).map { it.toRevisionScoreResult() })
}

data class RevisionScoreResult(
    val logName: String,
    val revisionScore: Double,
    val pathLikeliness: Double,
) {
  init {
    require(revisionScore in 0.0..1.0) { "revision score must be in [1, 0], is $revisionScore" }
    require(pathLikeliness in 0.0..1.0) { "path likeliness must be in [1, 0], is $pathLikeliness" }
  }
}

fun RevisionScoreResult.toRow(): List<String> =
    listOf(logName, revisionScore.toString(), pathLikeliness.toString())

fun List<String>.toRevisionScoreResult(): RevisionScoreResult {
  require(size == 3)
  val (logName, revisionScore, pathLikeliness) = this
  return RevisionScoreResult(logName, revisionScore.toDouble(), pathLikeliness.toDouble())
}

data class HittingPredictionList(val results: List<HittingPredictionResult>) :
    List<HittingPredictionResult> by results {
  companion object {
    const val LOG_NAME: String = "log-name"
    const val NO_SINK_STATES: String = "no-sink-states"
    const val PREDICTION_DELTAS_PREFIX: String = "prediction-deltas-"

    fun header(size: Int): List<String> =
        listOf(LOG_NAME, NO_SINK_STATES) + List(size) { "$PREDICTION_DELTAS_PREFIX$it" }
  }

  init {
    require(results.isNotEmpty())
    require(results.map { it.hasNoSinkStates }.allSame())
  }

  val predictionSize: Int
    get() = results.maxOf { it.predictionDeltas.size }
}

val HittingPredictionList.hasNoSinkStates: Boolean
  get() = results.first().hasNoSinkStates

fun HittingPredictionList.header(): List<String> = HittingPredictionList.header(predictionSize)

fun HittingPredictionList.toRows(): List<List<String>> =
    listOf(header()) + results.map { it.toRow(predictionSize) }

fun List<List<String>>.toHittingPredictionList(): HittingPredictionList =
    HittingPredictionList(subListFrom(1).map { it.toHittingPredictionResult() }).also {
      require(first() == it.header())
    }

data class HittingPredictionResult(
    val logName: String,
    val hasNoSinkStates: Boolean,
    val predictionDeltas: List<Duration?>,
) {
  companion object {
    const val DNF: String = "DNF"
  }

  init {
    require(hasSinkStates implies predictionDeltas.isEmpty())
  }
}

val HittingPredictionResult.hasSinkStates: Boolean
  get() = !hasNoSinkStates
val HittingPredictionResult.predictionErrors: Int
  get() = predictionDeltas.count { it == null }
val HittingPredictionResult.errorFreePredictions: List<Duration>
  get() = predictionDeltas.filterNotNull()
val HittingPredictionResult.predictionErrorRate: Double
  get() = predictionErrors.toDouble() / predictionDeltas.size

fun HittingPredictionResult.toRow(size: Int): List<String> =
    listOf(logName, hasNoSinkStates.toString()) +
        predictionDeltas.map { it?.toIsoString() ?: DNF } +
        List(size - predictionDeltas.size) { "" }

fun List<String>.toHittingPredictionResult(): HittingPredictionResult {
  require(size >= 2)
  val (logName, predictionErrors) = this
  return HittingPredictionResult(
      logName,
      predictionErrors.toBoolean(),
      subListFrom(2)
          .filter { it.isNotEmpty() }
          .map { if (it == DNF) null else Duration.parseIsoString(it) },
  )
}

data class AverageWithStddev<T>(val average: T, val standardDeviation: T) {
  companion object {
    fun fromNumbersOrNull(numbers: Iterable<Number>): AverageWithStddev<Double>? =
        numbers
            .map { it.toDouble() }
            .ifEmpty { null }
            ?.toDoubleArray()
            ?.let { AverageWithStddev(mean(it), standardDeviation(it)) }

    fun fromNumbers(numbers: Iterable<Number>): AverageWithStddev<Double> =
        requireNotNull(fromNumbersOrNull(numbers))

    fun fromDurationsOrNull(durations: Iterable<Duration>): AverageWithStddev<Duration>? =
        durations
            .toList()
            .ifEmpty { null }
            ?.let { AverageWithStddev(it.average(), it.standardDeviationOrNull() ?: ZERO) }

    fun fromDurations(durations: Iterable<Duration>): AverageWithStddev<Duration> =
        requireNotNull(fromDurationsOrNull(durations))
  }
}

data class EvaluationResultList(val results: List<EvaluationResult>) :
    List<EvaluationResult> by results {
  init {
    require(results.isNotEmpty())
  }

  companion object {
    const val AVERAGE: String = "avg"
    const val STDDEV: String = "stddev"

    const val CASE: String = "case"
    const val SETUP: String = "setup"
    const val TRAINING_SIZE: String = "training-size"
    const val STATES: String = "states"
    const val TRANSITIONS: String = "transitions"
    const val REVISION_SCORE: String = "revision-score"
    const val SINK_STATE_AUTOMATA: String = "sink-automata"
    const val HITTING_TIME_ERROR_RATE: String = "hitting-time-error-rate"
    const val HITTING_TIME_DELTA: String = "hitting-time-delta"

    val header: List<String> =
        listOf(CASE, SETUP, TRAINING_SIZE) +
            listOf(STATES, TRANSITIONS, REVISION_SCORE).flatMap {
              listOf("$it-$AVERAGE", "$it-$STDDEV")
            } +
            listOf(SINK_STATE_AUTOMATA) +
            listOf(HITTING_TIME_ERROR_RATE, HITTING_TIME_DELTA).flatMap {
              listOf("$it-$AVERAGE", "$it-$STDDEV")
            }
  }
}

fun EvaluationResultList.toRows(deltaUnit: DurationUnit?): List<List<String>> =
    listOf(EvaluationResultList.header) + results.map { it.toRow(deltaUnit) }

fun List<List<String>>.toEvaluationResultList(deltaUnit: DurationUnit?): EvaluationResultList {
  require(first() == EvaluationResultList.header)
  return EvaluationResultList(subListFrom(1).map { it.toEvaluationResult(deltaUnit) })
}

data class EvaluationResult(
    val case: String,
    val setup: String,
    val trainingSize: Int,
    val states: AverageWithStddev<Double>,
    val transitions: AverageWithStddev<Double>,
    val revisionScore: AverageWithStddev<Double>,
    val sinkStateAutomata: Int,
    val hittingTimeErrorRate: AverageWithStddev<Double>?,
    val hittingTimeDelta: AverageWithStddev<Duration>?,
) {

  companion object {
    const val NA: String = "???"

    fun <I> fromData(
        case: String,
        setup: String,
        trainingSize: Int,
        automata: Collection<Automaton<*, I, *>>,
        inputs: Set<I>,
        revisionScores: Collection<RevisionScoreList>,
        hittingTimes: Collection<HittingPredictionList>,
    ): EvaluationResult {
      val states = AverageWithStddev.fromNumbers(automata.map { it.size() })
      val transitions = AverageWithStddev.fromNumbers(automata.map { it.countTransitions(inputs) })
      val revisionScore =
          AverageWithStddev.fromNumbers(
              revisionScores.flatMap { it.map(RevisionScoreResult::revisionScore) }
          )
      val sinkStateAutomata = hittingTimes.count { it.first().hasSinkStates }
      val hittingTimeErrorRate =
          AverageWithStddev.fromNumbersOrNull(
              hittingTimes
                  .filter { it.hasNoSinkStates }
                  .flatMap { it.map(HittingPredictionResult::predictionErrorRate) }
          )
      val hittingTimeDelta =
          AverageWithStddev.fromDurationsOrNull(
              hittingTimes
                  .filter { it.hasNoSinkStates }
                  .flatMap { it.flatMap(HittingPredictionResult::errorFreePredictions) }
          )
      return EvaluationResult(
          case,
          setup,
          trainingSize,
          states,
          transitions,
          revisionScore,
          sinkStateAutomata,
          hittingTimeErrorRate,
          hittingTimeDelta,
      )
    }
  }
}

fun EvaluationResult.toRow(deltaUnit: DurationUnit?): List<String> =
    listOf(
        case,
        setup,
        "$trainingSize",
        "${states.average}",
        "${states.standardDeviation}",
        "${transitions.average}",
        "${transitions.standardDeviation}",
        "${revisionScore.average}",
        "${revisionScore.standardDeviation}",
        "$sinkStateAutomata",
        "${hittingTimeErrorRate?.average ?: NA}",
        "${hittingTimeErrorRate?.standardDeviation ?: NA}",
        hittingTimeDelta?.average?.let {
          if (deltaUnit != null) "${it.toDouble(deltaUnit)}" else it.toIsoString()
        } ?: NA,
        hittingTimeDelta?.standardDeviation?.let {
          if (deltaUnit != null) "${it.toDouble(deltaUnit)}" else it.toIsoString()
        } ?: NA,
    )

fun List<String>.toEvaluationResult(deltaUnit: DurationUnit?): EvaluationResult {
  require(size == 14)
  val (
      case,
      setup,
      trainingSize,
      statesAverage,
      statesStddev,
      transitionsAverage,
      transitionsStddev,
      revisionScoreAverage,
      revisionScoreStddev,
      sinkStateAutomata,
      hittingTimeErrorsAverage,
      hittingTimeErrorsStddev,
      hittingTimeDeltaAverage,
      hittingTimeDeltaStddev) =
      this
  return EvaluationResult(
      case,
      setup,
      trainingSize.toInt(),
      AverageWithStddev(statesAverage.toDouble(), statesStddev.toDouble()),
      AverageWithStddev(transitionsAverage.toDouble(), transitionsStddev.toDouble()),
      AverageWithStddev(revisionScoreAverage.toDouble(), revisionScoreStddev.toDouble()),
      sinkStateAutomata.toInt(),
      if (hittingTimeErrorsAverage == NA || hittingTimeErrorsStddev == NA) null
      else
          AverageWithStddev(
              hittingTimeErrorsAverage.toDouble(),
              hittingTimeErrorsStddev.toDouble(),
          ),
      if (hittingTimeDeltaAverage == NA || hittingTimeDeltaStddev == NA) null
      else
          AverageWithStddev(
              if (deltaUnit != null) hittingTimeDeltaAverage.toDouble().toDuration(deltaUnit)
              else Duration.parseIsoString(hittingTimeDeltaAverage),
              if (deltaUnit != null) hittingTimeDeltaStddev.toDouble().toDuration(deltaUnit)
              else Duration.parseIsoString(hittingTimeDeltaStddev),
          ),
  )
}
