// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.validation

import kotlin.math.exp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import tools.aqua.hpm.automata.DeterministicFrequencyProbabilisticTimedInputOutputAutomaton

/**
 * Timed Transition likelihood using Perkin's definition.
 *
 * [Perkins, T. J. (2009). Maximum likelihood trajectories for continuous-time markov chains. In Y.
 * Bengio, D. Schuurmans, J. Lafferty, C. Williams, & A. Culotta (Eds.), Advances in Neural
 * Information Processing Systems (pp. 1437–1445). Curran Associates,
 * Inc.](https://proceedings.neurips.cc/paper/2009/hash/afda332245e2af431fb7b672a68b659d-Abstract.html)
 */
fun <S, T> DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<S, *, T, *>
    .getTransitionLikelihood(state: S, transition: T, time: Duration): Double =
    1.seconds / getExitTime(state) * getNonNormalizedTransitionLikelihood(state, transition, time)

fun <S, T> DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<S, *, T, *>
    .getNonNormalizedTransitionLikelihood(state: S, transition: T, time: Duration): Double =
    exp(-time / getExitTime(state)) * getTransitionProbability(transition)
