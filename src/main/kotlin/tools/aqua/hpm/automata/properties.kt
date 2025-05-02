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

interface ExitTime<S> {
  fun getExitTime(state: S): Duration
}

interface MutableExitTime<S> {
  fun setExitTime(state: S, time: Duration)
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
  val time: Duration
  val output: O
}

data class SimpleTimedOutput<O>(override val time: Duration, override val output: O) :
    TimedOutput<O>

fun <O> O.withExitTime(time: Duration): SimpleTimedOutput<O> = SimpleTimedOutput(time, this)
