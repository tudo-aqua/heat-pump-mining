// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.automata

import net.automatalib.automaton.MutableAutomaton
import net.automatalib.automaton.UniversalAutomaton
import net.automatalib.automaton.concept.MutableProbabilistic
import net.automatalib.automaton.concept.MutableStateOutput
import net.automatalib.automaton.concept.Probabilistic
import net.automatalib.automaton.concept.StateOutput
import net.automatalib.automaton.graph.TransitionEdge
import net.automatalib.automaton.graph.UniversalAutomatonGraphView
import net.automatalib.automaton.visualization.AutomatonVisualizationHelper
import net.automatalib.graph.UniversalGraph
import net.automatalib.visualization.VisualizationHelper
import net.automatalib.visualization.VisualizationHelper.CommonAttrs.LABEL

interface FrequencyProbabilisticTimedInputOutputAutomaton<S, I, T, O> :
    UniversalAutomaton<S, I, T, TimedOutput<O>, FrequencyAndProbability>,
    Frequency<T>,
    ExitTime<S>,
    Probabilistic<T>,
    StateOutput<S, O> {

  override fun getStateProperty(state: S): TimedOutput<O> =
      getStateOutput(state).withExitTime(getExitTime(state))

  override fun getTransitionProperty(transition: T): FrequencyAndProbability =
      getTransitionFrequency(transition).withProbability(getTransitionProbability(transition))

  override fun transitionGraphView(
      inputs: Collection<I>
  ): UniversalGraph<
      S,
      TransitionEdge<I, T>,
      TimedOutput<O>,
      TransitionEdge.Property<I, FrequencyAndProbability>> =
      FrequencyProbabilisticTimedAutomatonGraphView(this, inputs)
}

open class FrequencyProbabilisticTimedAutomatonGraphView<
    S, I, T, O, A : FrequencyProbabilisticTimedInputOutputAutomaton<S, I, T, O>>(
    automaton: A,
    inputs: Collection<I>
) :
    UniversalAutomatonGraphView<S, I, T, TimedOutput<O>, FrequencyAndProbability, A>(
        automaton, inputs) {
  override fun getVisualizationHelper(): VisualizationHelper<S, TransitionEdge<I, T>> =
      FrequencyProbabilisticTimedAutomatonVisualizationHelper(automaton)
}

open class FrequencyProbabilisticTimedAutomatonVisualizationHelper<
    S, I, T, O, A : FrequencyProbabilisticTimedInputOutputAutomaton<S, I, T, O>>(automaton: A) :
    AutomatonVisualizationHelper<S, I, T, A>(automaton) {

  override fun getNodeProperties(node: S, properties: MutableMap<String, String>): Boolean {
    val output = automaton.getStateOutput(node)
    val exitTime = automaton.getExitTime(node)
    properties[LABEL] = "$output / t=$exitTime"
    return true
  }

  override fun getEdgeProperties(
      src: S,
      edge: TransitionEdge<I, T>,
      tgt: S,
      properties: MutableMap<String, String>
  ): Boolean {
    val freq = automaton.getTransitionFrequency(edge.transition)
    val prob = automaton.getTransitionProbability(edge.transition)
    properties[LABEL] = "${edge.input} / n=$freq / p=$prob"
    return true
  }
}

interface MutableFrequencyProbabilisticTimedInputOutputAutomaton<S, I, T, O> :
    FrequencyProbabilisticTimedInputOutputAutomaton<S, I, T, O>,
    MutableAutomaton<S, I, T, TimedOutput<O>, FrequencyAndProbability>,
    MutableFrequency<T>,
    MutableExitTime<S>,
    MutableProbabilistic<T>,
    MutableStateOutput<S, O> {
  override fun setStateProperty(state: S, property: TimedOutput<O>) {
    setStateOutput(state, property.output)
    setExitTime(state, property.time)
  }

  override fun setTransitionProperty(transition: T, property: FrequencyAndProbability) {
    setTransitionFrequency(transition, property.frequency)
    setTransitionProbability(transition, property.probability)
  }
}

interface DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<S, I, T, O> :
    FrequencyProbabilisticTimedInputOutputAutomaton<S, I, T, O> {
  fun getInitialState(): S?

  override fun getInitialStates(): Set<S> = setOfNotNull(getInitialState())

  fun getTransition(state: S, input: I, output: O): T?
}

interface MutableDeterministicFrequencyProbabilisticTimedInputOutputAutomaton<S, I, T, O> :
    DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<S, I, T, O>,
    MutableFrequencyProbabilisticTimedInputOutputAutomaton<S, I, T, O> {

  override fun getInitialStates(): Set<S> =
      super<DeterministicFrequencyProbabilisticTimedInputOutputAutomaton>.getInitialStates()

  override fun setInitial(state: S, initial: Boolean) {
    if (initial) {
      if (getInitialState() == null) {
        setInitialState(state)
      } else {
        require(state == getInitialState()) { "only one initial state permitted" }
      }
    } else if (state == getInitialState()) {
      setInitialState(null)
    }
  }

  fun setInitialState(state: S?)

  override fun setTransitions(state: S, input: I, transitions: Collection<T>) {
    transitions.forEach { setTransition(state, input, getStateOutput(getSuccessor(it)), it) }
  }

  fun setTransition(state: S, input: I, output: O, transition: T?)
}
