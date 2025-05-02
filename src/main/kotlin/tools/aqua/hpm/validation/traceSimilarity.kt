// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.validation

import kotlin.time.Duration
import tools.aqua.hpm.automata.DeterministicFrequencyProbabilisticTimedInputOutputAutomaton
import tools.aqua.hpm.data.SimpleTrace
import tools.aqua.hpm.data.TimedIOTrace
import tools.aqua.hpm.data.inputs
import tools.aqua.hpm.data.states
import tools.aqua.hpm.data.times
import tools.aqua.hpm.data.transitions
import tools.aqua.hpm.util.average
import tools.aqua.hpm.util.fTestSimilarity
import tools.aqua.hpm.util.hoeffdingSimilarity

fun <S, I, T, O> DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<S, I, T, O>
    .computeBestTraceSimilarity(
    alphabet: Iterable<I>,
    word: TimedIOTrace<I, O>,
    frequencyWeight: Double = 0.5,
): Double {
  require(frequencyWeight in 0.0..1.0) { "frequency weight must be in [0, 1]" }

  return getMatchingPaths(word).maxOf {
    computeTraceSimilarity(alphabet, word, it, frequencyWeight)
  }
}

fun <S, I, T, O> DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<S, I, T, O>
    .computeTraceSimilarity(
    alphabet: Iterable<I>,
    word: TimedIOTrace<I, O>,
    trace: SimpleTrace<S, T>,
    frequencyWeight: Double = 0.5,
): Double {
  require(frequencyWeight in 0.0..1.0) { "frequency weight must be in [0, 1]" }

  val observedFrequencies = trace.transitions.groupingBy { it }.eachCount()
  val observedTotalFrequencies =
      (trace.states.dropLast(1) zip word.inputs).groupingBy { it }.eachCount()
  val observedTimings =
      (trace.states.dropLast(1) zip word.times)
          .groupingBy { (state, _) -> state }
          .fold(emptyList<Duration>()) { timings, observation ->
            val (_, timing) = observation
            timings + timing
          }

  val averageHoeffdingSimilarity =
      states
          .flatMap { state ->
            alphabet.flatMap { input ->
              val totalFrequency = getTransitions(state, input).sumOf(::getTransitionFrequency)

              getTransitions(state, input).map { transition ->
                hoeffdingSimilarity(
                    getTransitionFrequency(transition),
                    totalFrequency,
                    observedFrequencies[transition] ?: 0,
                    observedTotalFrequencies[state to input] ?: 0)
              }
            }
          }
          .average()

  val averageFSimilarity =
      states
          .map { state ->
            val totalFrequency =
                alphabet.sumOf { input ->
                  getTransitions(state, input).sumOf(::getTransitionFrequency)
                }

            fTestSimilarity(
                getExitTime(state),
                totalFrequency,
                observedTimings[state]?.average() ?: Duration.ZERO,
                observedTimings[state]?.size ?: 0)
          }
          .average()

  return averageHoeffdingSimilarity * frequencyWeight + averageFSimilarity * (1.0 - frequencyWeight)
}
