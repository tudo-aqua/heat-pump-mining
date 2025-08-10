// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.traces

import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit.NANOSECONDS
import org.apache.commons.math3.distribution.ExponentialDistribution
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.random.Well19937c
import tools.aqua.hpm.automata.DeterministicFrequencyProbabilisticTimedInputOutputAutomaton
import tools.aqua.hpm.data.TimedIO
import tools.aqua.hpm.data.TimedIOTrace
import tools.aqua.hpm.util.selectWeighted

fun <I, O> DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<*, I, *, O>.generateTraces(
    n: Int,
    meanLength: Double,
    lengthSD: Double,
    input: I,
    random: Random
): List<TimedIOTrace<I, O>> {
  check(lengthSD >= 0) { "standard deviation of length must be positive, is $lengthSD" }
  val lengths =
      if (lengthSD > 0) {
            NormalDistribution(Well19937c(random.nextInt()), meanLength, lengthSD)
                .sample(n)
                .asList()
          } else {
            List(n) { meanLength }
          }
          .map { it.roundToInt().coerceAtLeast(0) }
  return lengths.map { generateTrace(it, input, random) }
}

fun <I, O> DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<*, I, *, O>.generateTrace(
    length: Int,
    input: I,
    random: Random
): TimedIOTrace<I, O> = generateTrace(length, mapOf(input to 1), random)

fun <S, I, T, O> DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<S, I, T, O>
    .generateTrace(
    length: Int,
    weightedInputs: Map<I, Number>,
    random: Random
): TimedIOTrace<I, O> {
  val initial = initialStates.random(random)
  var state = initial
  val tail =
      List(length) {
        val time =
            ExponentialDistribution(
                    Well19937c(random.nextInt()), getExitTime(state).toDouble(NANOSECONDS))
                .sample()
                .nanoseconds
        val input = random.selectWeighted(weightedInputs)
        val transitions = getTransitions(state, input).associateWith(::getTransitionProbability)
        state = random.selectWeighted(transitions).let(::getSuccessor)
        TimedIO(time, input, getStateOutput(state))
      }
  return TimedIOTrace(getStateOutput(initial), tail)
}
