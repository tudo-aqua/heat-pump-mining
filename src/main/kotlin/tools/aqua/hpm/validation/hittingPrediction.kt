// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.validation

import java.util.TreeSet
import java.util.stream.Collectors
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import tools.aqua.hpm.automata.DeterministicFrequencyProbabilisticTimedInputOutputAutomaton
import tools.aqua.hpm.data.TimedIOTrace
import tools.aqua.hpm.data.lastState
import tools.aqua.hpm.data.prefixes
import tools.aqua.hpm.data.subTraceTo
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
    val next = checkNotNull(hits.ceiling(end))
    HittingInformation(it, next - end)
  }
}

private fun <S, I, O> DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<S, I, *, O>
    .hittingTimeDelta(
    word: TimedIOTrace<I, O>,
    samples: Int?,
    predictions: Map<S, Duration>,
    targetOutputs: Set<O>
): List<HittingInformation<I, O>?> {
  val shortenedWord =
      word.tail.indexOfLast { it.output in targetOutputs }.let { word.subTraceTo(it + 1) }

  val prefixes =
      shortenedWord.prefixes.let { if (samples != null) it.takeEvenlySpaced(samples) else it }

  val estimated = predictHittingTimes(prefixes, predictions)
  val correct = word.actualHittingTimes(prefixes, targetOutputs)
  check(estimated.size == correct.size)

  return (estimated zip correct).map { (e, c) ->
    if (e == null) {
      null
    } else {
      check(e.word == c.word)
      HittingInformation(e.word, e.hittingTime - c.hittingTime)
    }
  }
}

fun <S, I, O> DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<S, I, *, O>
    .hittingTimeDelta(
    words: Collection<TimedIOTrace<I, O>>,
    samples: Int?,
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
      .map { word -> word to hittingTimeDelta(word, samples, predictions, targetOutputs) }
      .collect(Collectors.toList())
      .toMap()
}
