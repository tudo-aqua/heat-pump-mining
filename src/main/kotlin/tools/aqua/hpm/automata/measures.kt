// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.automata

import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import tools.aqua.hpm.util.average
import tools.aqua.hpm.util.standardDeviationOrNull
import tools.aqua.rereso.util.mapToSet

val <S, O> DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<S, *, *, O>.outputs: Set<O>
  get() = states.mapToSet { getStateOutput(it) }

fun <S, O> DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<S, *, *, O>
    .getStatesWithOutput(output: O): Collection<S> = states.filter { getStateOutput(it) == output }

fun <S> DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<S, *, *, *>
    .getTimingDispersion(state: S): Double = getExitTimes(state).let { times -> times.dispersion }

private val Collection<Duration>.dispersion: Double
  get() {
    val stddev = standardDeviationOrNull() ?: ZERO
    val average = average()
    return if (stddev == ZERO && average == ZERO) 0.0 else stddev / average
  }
