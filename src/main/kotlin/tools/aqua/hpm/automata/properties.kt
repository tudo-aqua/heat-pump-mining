// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.automata

import kotlin.time.Duration
import net.automatalib.automaton.concept.MutableProbabilistic
import net.automatalib.automaton.concept.Probabilistic

interface Frequency<T> : Probabilistic<T> {
  fun getTransitionFrequency(transition: T): Int
}

interface MutableFrequency<T> : Frequency<T>, MutableProbabilistic<T> {
  fun setTransitionFrequency(transition: T, frequency: Int)
}

interface ExitTimes<S> {
  fun getExitTimes(state: S): Collection<Duration>
}

interface MutableExitTime<S> {
  fun setExitTimes(state: S, times: Collection<Duration>)
}

interface FrequencyAndProbability {
  val frequency: Int
  val probability: Float
}

data class SimpleFrequencyAndProbability(
    override val frequency: Int,
    override val probability: Float,
) : FrequencyAndProbability

fun Int.withProbability(probability: Float): SimpleFrequencyAndProbability =
    SimpleFrequencyAndProbability(this, probability)

interface TimedOutput<O> {
  val times: Collection<Duration>
  val output: O
}

data class SimpleTimedOutput<O>(override val times: Collection<Duration>, override val output: O) :
    TimedOutput<O>

fun <O> O.withExitTimes(times: Collection<Duration>): SimpleTimedOutput<O> =
    SimpleTimedOutput(times, this)
