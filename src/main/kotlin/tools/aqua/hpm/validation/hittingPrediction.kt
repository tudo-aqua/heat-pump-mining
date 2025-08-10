// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.validation

import java.util.TreeSet
import java.util.stream.Collectors
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.ZERO
import tools.aqua.hpm.automata.DeterministicFrequencyProbabilisticTimedInputOutputAutomaton
import tools.aqua.hpm.data.TimedIOTrace
import tools.aqua.hpm.data.lastState
import tools.aqua.hpm.data.prefixes
import tools.aqua.hpm.data.withAbsoluteTimes
import tools.aqua.hpm.util.takeEvenlySpaced

data class HittingInformation<I, O>(val word: TimedIOTrace<I, O>, val hittingTime: Duration)

private fun <S, I, O> DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<S, I, *, O>
    .predictHittingTimes(
    prefixes: Collection<TimedIOTrace<I, O>>,
    predictions: Map<S, Duration>,
): List<HittingInformation<I, O>?> {
  return prefixes.map { prefix ->
    getMostLikelyViterbiPath(prefix)?.lastState?.let {
      HittingInformation(prefix, predictions.getValue(it))
    }
  }
}

private fun <I, O> TimedIOTrace<I, O>.actualHittingTimes(
    prefixes: Iterable<TimedIOTrace<I, O>>,
    targetOutputs: Set<O>
): List<HittingInformation<I, O>> {
  val hits =
      tail
          .withAbsoluteTimes()
          .filter { (_, io) -> io.output in targetOutputs }
          .mapTo(TreeSet()) { (abs, _) -> abs }
  if (head in targetOutputs) hits += ZERO

  return prefixes.map {
    val end = it.tail.withAbsoluteTimes().lastOrNull()?.first ?: ZERO
    val next = hits.ceiling(end)
    HittingInformation(it, if (next != null) next - end else INFINITE)
  }
}

private fun <S, I, O> DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<S, I, *, O>
    .hittingTimeDelta(
    word: TimedIOTrace<I, O>,
    sampleRate: Double,
    maxSamples: Int,
    predictions: Map<S, Duration>,
    targetOutputs: Set<O>
): List<HittingInformation<I, O>?> {
  check(sampleRate > 0 && sampleRate <= 1)

  val prefixes =
      word.prefixes.let {
        it.takeEvenlySpaced(
            (it.size * sampleRate).roundToInt().coerceIn(1, min(it.size, maxSamples)))
      }

  val estimated = predictHittingTimes(prefixes, predictions)
  val correct = word.actualHittingTimes(prefixes, targetOutputs)
  check(estimated.size == correct.size)

  return (estimated zip correct)
      .filter { (_, c) -> c.hittingTime != INFINITE }
      .map { (e, c) ->
        if (e == null) null
        else {
          check(e.word == c.word)
          HittingInformation(e.word, e.hittingTime - c.hittingTime)
        }
      }
}

fun <S, I, O> DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<S, I, *, O>
    .hittingTimeDelta(
    words: Collection<TimedIOTrace<I, O>>,
    sampleRate: Double,
    maxSamples: Int,
    input: I,
    targetOutputs: Set<O>,
    parallel: Boolean,
): Map<TimedIOTrace<I, O>, List<HittingInformation<I, O>?>>? {
  val targetStates = states.filterTo(mutableSetOf()) { getStateOutput(it) in targetOutputs }

  val predictions =
      try {
        computeMeanHittingTimes(input, targetStates, parallel)
      } catch (_: UnconnectedAutomatonException) {
        return null
      }

  return words
      .let { if (parallel) it.parallelStream() else it.stream() }
      .map { word ->
        word to hittingTimeDelta(word, sampleRate, maxSamples, predictions, targetOutputs)
      }
      .collect(Collectors.toList())
      .toMap()
}
