// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.automata

import kotlin.math.roundToInt
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
import net.automatalib.visualization.VisualizationHelper.CommonAttrs.COLOR
import net.automatalib.visualization.VisualizationHelper.CommonAttrs.LABEL
import tools.aqua.hpm.util.average
import tools.aqua.hpm.util.standardDeviation

interface FrequencyProbabilisticTimedInputOutputAutomaton<S, I, T, O> :
    UniversalAutomaton<S, I, T, TimedOutput<O>, FrequencyAndProbability>,
    Frequency<T>,
    ExitTimes<S>,
    Probabilistic<T>,
    StateOutput<S, O> {

  override fun getStateProperty(state: S): TimedOutput<O> =
      getStateOutput(state).withExitTimes(getExitTimes(state))

  override fun getTransitionProperty(transition: T): FrequencyAndProbability =
      getTransitionFrequency(transition).withProbability(getTransitionProbability(transition))

  override fun transitionGraphView(
      inputs: Collection<I>
  ): UniversalGraph<
      S,
      TransitionEdge<I, T>,
      TimedOutput<O>,
      TransitionEdge.Property<I, FrequencyAndProbability>,
  > = FrequencyProbabilisticTimedAutomatonGraphView(this, inputs)

  fun renderTransitionGraphView(
      inputs: Collection<I>,
      transitionFullColorMin: Double = 0.0,
      transitionRenderMin: Double = 0.0,
  ): UniversalGraph<
      S,
      TransitionEdge<I, T>,
      TimedOutput<O>,
      TransitionEdge.Property<I, FrequencyAndProbability>,
  > =
      FrequencyProbabilisticTimedAutomatonGraphView(
          this,
          inputs,
          true,
          transitionFullColorMin,
          transitionRenderMin,
      )
}

open class FrequencyProbabilisticTimedAutomatonGraphView<
    S,
    I,
    T,
    O,
    A : FrequencyProbabilisticTimedInputOutputAutomaton<S, I, T, O>,
>(
    automaton: A,
    inputs: Collection<I>,
    private val simplify: Boolean = false,
    private val transitionFullColorMin: Double = 0.0,
    private val transitionRenderMin: Double = 0.0,
) :
    UniversalAutomatonGraphView<S, I, T, TimedOutput<O>, FrequencyAndProbability, A>(
        automaton,
        inputs,
    ) {
  override fun getVisualizationHelper(): VisualizationHelper<S, TransitionEdge<I, T>> =
      FrequencyProbabilisticTimedAutomatonVisualizationHelper(
          automaton,
          simplify,
          transitionFullColorMin,
          transitionRenderMin,
      )
}

open class FrequencyProbabilisticTimedAutomatonVisualizationHelper<
    S,
    I,
    T,
    O,
    A : FrequencyProbabilisticTimedInputOutputAutomaton<S, I, T, O>,
>(
    automaton: A,
    private val simplify: Boolean = false,
    private val transitionFullColorMin: Double = 0.0,
    private val transitionRenderMin: Double = 0.0,
) : AutomatonVisualizationHelper<S, I, T, A>(automaton) {

  override fun getNodeProperties(node: S, properties: MutableMap<String, String>): Boolean {
    super.getNodeProperties(node, properties)
    val output = automaton.getStateOutput(node)
    val exitTimes = automaton.getExitTimes(node)

    val timeData =
        if (simplify) {
          "tAvg=${exitTimes.average()} / tSD=${exitTimes.standardDeviation()}"
        } else {
          "t=${exitTimes.joinToString(", ", "[", "]")}"
        }

    properties[LABEL] = "$output / $timeData"
    return true
  }

  override fun getEdgeProperties(
      src: S,
      edge: TransitionEdge<I, T>,
      tgt: S,
      properties: MutableMap<String, String>,
  ): Boolean {
    super.getEdgeProperties(src, edge, tgt, properties)
    val freq = automaton.getTransitionFrequency(edge.transition)
    val prob = automaton.getTransitionProbability(edge.transition)

    val probData =
        if (simplify) {
          "p=${"%.2f".format(prob)}"
        } else {
          "n=$freq / p=$prob"
        }

    properties[LABEL] = "${edge.input} / $probData"

    if (prob in transitionRenderMin..<transitionFullColorMin) {
      val saturation = (prob - transitionRenderMin) / (transitionFullColorMin - transitionRenderMin)
      properties[COLOR] = "#${(255 * saturation).roundToInt().toByte().toHexString().repeat(3)}"
    }

    return prob >= transitionRenderMin
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
    setExitTimes(state, property.times)
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
