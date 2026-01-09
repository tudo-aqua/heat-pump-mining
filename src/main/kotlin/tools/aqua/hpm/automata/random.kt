// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.automata

import kotlin.collections.forEach
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.time.Duration
import tools.aqua.hpm.util.nextDuration

class DFTPIOAGenerator<I, O>(
    private val sizeRange: IntRange,
    private val input: I,
    private val outputs: Collection<O>,
    private val exitTimeRange: ClosedRange<Duration>,
    private val random: Random,
) : Iterator<DefaultDeterministicFrequencyProbabilisticTimedInputOutputAutomaton<I, O>> {

  init {
    require(sizeRange.first >= outputs.size) { "must generate at least one state per output" }
  }

  override fun hasNext(): Boolean = true

  override fun next(): DefaultDeterministicFrequencyProbabilisticTimedInputOutputAutomaton<I, O> {
    val automaton = DefaultDeterministicFrequencyProbabilisticTimedInputOutputAutomaton<I, O>()

    val statesByOutput = outputs.associateWith { mutableListOf<DFPTIOState>() }

    // select first output for the initial state
    val initialTimedOutput = outputs.first().randomTimed()
    statesByOutput.getValue(initialTimedOutput.output) +=
        automaton.addInitialState(initialTimedOutput)

    (outputs - initialTimedOutput.output).forEach { output ->
      // add states for each non-initial output
      statesByOutput.getValue(output) += automaton.addState(output.randomTimed())
    }

    repeat(random.nextInt(sizeRange) - outputs.size) {
      // add additional states with random outputs
      val timedOutput = randomOutput()
      statesByOutput.getValue(timedOutput.output) += automaton.addState(timedOutput)
    }

    automaton.states.forEach { state ->
      // ensure determinism by adding one transition per output
      randomProbabilityOutputs().forEach { (output, probability) ->
        // randomly select an eligible target
        val next = statesByOutput.getValue(output).random(random)
        automaton.addTransition(state, input, next, probability)
      }
    }

    return automaton
  }

  private fun randomOutput() =
      outputs.random(random).withExitTimes(listOf(random.nextDuration(exitTimeRange)))

  private fun O.randomTimed() = withExitTimes(listOf(random.nextDuration(exitTimeRange)))

  private fun randomProbabilityOutputs() =
      outputs
          .map { output -> output to random.nextDouble() }
          .let { denormalized ->
            val sum = denormalized.sumOf { (_, weight) -> weight }
            denormalized.map { (output, weight) ->
              val probability = weight / sum
              // reasonable estimate of frequency observations
              val frequency = ((Int.MAX_VALUE / 2) * probability).roundToInt()
              output to frequency.withProbability(probability.toFloat())
            }
          }
}
