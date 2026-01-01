// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.automata

import java.util.WeakHashMap
import kotlin.time.Duration
import net.automatalib.automaton.concept.StateIDs
import tools.aqua.hpm.util.ThreeStageHashMap
import tools.aqua.hpm.util.set

class DFPTIOState

class DFPTIOTransition

class DefaultDeterministicFrequencyProbabilisticTimedInputOutputAutomaton<I, O> :
    MutableDeterministicFrequencyProbabilisticTimedInputOutputAutomaton<
        DFPTIOState,
        I,
        DFPTIOTransition,
        O,
    >,
    StateIDs<DFPTIOState> {

  private var initial: DFPTIOState? = null
  private val states = mutableListOf<DFPTIOState>()
  private val outputs = WeakHashMap<DFPTIOState, O>()
  private val exitTimes = WeakHashMap<DFPTIOState, Collection<Duration>>()

  private val transitions = ThreeStageHashMap<DFPTIOState, I, O, DFPTIOTransition>()
  private val frequencies = WeakHashMap<DFPTIOTransition, Int>()
  private val probabilities = WeakHashMap<DFPTIOTransition, Float>()
  private val targets = WeakHashMap<DFPTIOTransition, DFPTIOState>()

  override fun getInitialState(): DFPTIOState? = initial

  override fun setInitialState(state: DFPTIOState?) {
    require(state == null || state in states)
    initial = state
  }

  override fun getStates(): Collection<DFPTIOState> = states

  override fun addState(property: TimedOutput<O>?): DFPTIOState {
    requireNotNull(property)
    return DFPTIOState().also {
      states += it
      outputs[it] = property.output
      exitTimes[it] = property.times
    }
  }

  override fun getState(id: Int): DFPTIOState = states[id]

  override fun getStateId(state: DFPTIOState): Int = states.indexOf(state).also { check(it >= 0) }

  override fun stateIDs(): StateIDs<DFPTIOState> = this

  override fun getTransitions(state: DFPTIOState, input: I): Collection<DFPTIOTransition> =
      transitions[state, input]

  override fun getTransition(state: DFPTIOState, input: I, output: O): DFPTIOTransition? =
      transitions[state, input, output]

  override fun createTransition(
      successor: DFPTIOState,
      properties: FrequencyAndProbability,
  ): DFPTIOTransition =
      DFPTIOTransition().also {
        frequencies[it] = properties.frequency
        probabilities[it] = properties.probability
        targets[it] = successor
      }

  override fun setTransition(
      state: DFPTIOState,
      input: I,
      output: O,
      transition: DFPTIOTransition?,
  ) {
    val output = outputs.getValue(targets.getValue(transition))
    if (transition != null) {
      transitions[state, input, output] = transition
    } else {
      transitions.remove(state, input, output)
    }
  }

  override fun removeAllTransitions(state: DFPTIOState) {
    transitions.remove(state)
  }

  override fun getSuccessor(transition: DFPTIOTransition): DFPTIOState =
      targets.getValue(transition)

  override fun getStateOutput(state: DFPTIOState): O = outputs.getValue(state)

  override fun setStateOutput(state: DFPTIOState, output: O) {
    outputs[state] = output
  }

  override fun getExitTimes(state: DFPTIOState): Collection<Duration> = exitTimes.getValue(state)

  override fun setExitTimes(state: DFPTIOState, times: Collection<Duration>) {
    exitTimes[state] = times
  }

  override fun getTransitionProbability(transition: DFPTIOTransition): Float =
      probabilities.getValue(transition)

  override fun setTransitionProbability(transition: DFPTIOTransition, probability: Float) {
    probabilities[transition] = probability
  }

  override fun getTransitionFrequency(transition: DFPTIOTransition): Int =
      frequencies.getValue(transition)

  override fun setTransitionFrequency(transition: DFPTIOTransition, frequency: Int) {
    frequencies[transition] = frequency
  }

  override fun clear() {
    initial = null
    states.clear()
    transitions.clear()
  }
}
