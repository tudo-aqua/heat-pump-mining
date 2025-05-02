// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.validation

import tools.aqua.hpm.automata.DeterministicFrequencyProbabilisticTimedInputOutputAutomaton
import tools.aqua.hpm.data.SimpleTrace
import tools.aqua.hpm.data.TimedIOTrace
import tools.aqua.hpm.data.lastState
import tools.aqua.hpm.data.plus

fun <S, I, T, O> DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<S, I, T, O>
    .getMatchingPaths(word: TimedIOTrace<I, O>): List<SimpleTrace<S, T>> {
  val initial = states.filter { getStateOutput(it) == word.head }.map { SimpleTrace<S, T>(it) }

  return word.tail.fold(initial) { traces, io ->
    val (_, input, output) = io
    traces.mapNotNull { trace ->
      val state = trace.lastState
      getTransition(state, input, output)?.let { transition ->
        trace + (transition to getSuccessor(transition))
      }
    }
  }
}

fun <S, I, T, O> DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<S, I, T, O>
    .getViterbiPaths(
    word: TimedIOTrace<I, O>,
    normalizeLikelihood: Boolean = true,
): List<Pair<SimpleTrace<S, T>, Double>> {
  val initial =
      states
          .filter { getStateOutput(it) == word.head }
          .let { matching -> matching.map { SimpleTrace<S, T>(it) to 1.0 / matching.size } }

  return word.tail.fold(initial) { probabilities, io ->
    val (time, input, output) = io
    probabilities.mapNotNull { (trace, probability) ->
      val state = trace.lastState
      getTransition(state, input, output)?.let { transition ->
        val likelihood =
            if (normalizeLikelihood) {
              getTransitionLikelihood(state, transition, time)
            } else {
              getNonNormalizedTransitionLikelihood(state, transition, time)
            }

        trace + (transition to getSuccessor(transition)) to likelihood * probability
      }
    }
  }
}

fun <S, I, T, O> DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<S, I, T, O>
    .getMostLikelyViterbiPath(word: TimedIOTrace<I, O>): SimpleTrace<S, T> =
    getViterbiPaths(word).maxBy { (_, probability) -> probability }.first
