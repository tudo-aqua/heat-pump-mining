// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.rtioalergia

import kotlin.time.Duration
import tools.aqua.hpm.automata.DeterministicFrequencyProbabilisticTimedInputOutputAutomaton
import tools.aqua.hpm.data.TimedIOTrace
import tools.aqua.hpm.util.TwoStageHashMap

private class PTATransition<I, O>(
    override val source: PTANode<I, O>,
    override val input: I,
    output: O,
) : FrequencyTransition<PTANode<I, O>, I> {
  override val target: PTANode<I, O> = PTANode.child(this, output)

  private var frequencyMutable = 0
  override val frequency: Int
    get() = frequencyMutable

  fun recordTransition() {
    frequencyMutable++
  }
}

private class PTANode<I, O>
private constructor(
    override val accessTransition: PTATransition<I, O>?,
    override val output: O,
) : TimedFrequencyTreeNode<I, O, PTANode<I, O>> {

  private val timingsMutable = mutableListOf<Duration>()
  override val timings: Collection<Duration> = timingsMutable

  companion object {
    fun <I, O> child(accessTransition: PTATransition<I, O>, output: O): PTANode<I, O> =
        PTANode(accessTransition, output)

    fun <I, O> root(output: O): PTANode<I, O> = PTANode(null, output)
  }

  private val transitionsMutable = TwoStageHashMap<I, O, PTATransition<I, O>>()
  override val transitions: Collection<FrequencyTransition<PTANode<I, O>, I>> = transitionsMutable

  fun recordInput(
      time: Duration,
      input: I,
      output: O,
  ): PTANode<I, O> {
    timingsMutable += time
    return transitionsMutable
        .computeIfAbsent(input, output) { PTATransition(this, input, output) }
        .apply { recordTransition() }
        .target
  }

  override fun getTransitions(input: I): Collection<FrequencyTransition<PTANode<I, O>, I>> =
      transitionsMutable[input]

  override fun getTransitionOrNull(input: I, output: O): FrequencyTransition<PTANode<I, O>, I>? =
      transitionsMutable[input, output]

  override fun toString(): String = "PTANode@${hashCode()}[output=$output]"
}

class TimedFrequencyPTA<I, O>(rootOutput: O) :
    DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<
        TimedFrequencyTreeNode<I, O, *>,
        I,
        FrequencyTransition<TimedFrequencyTreeNode<I, O, *>, I>,
        O,
    > {

  private val rootMutable: PTANode<I, O> = PTANode.root(rootOutput)

  fun addWord(word: TimedIOTrace<I, O>) {
    require(word.head == rootMutable.output) {
      "divergent initial state output; expected ${rootMutable.output}, but got ${word.head}"
    }
    word.tail.fold(rootMutable) { node, io ->
      val (time, input, output) = io
      node.recordInput(time, input, output)
    }
  }

  override fun getInitialState(): TimedFrequencyTreeNode<I, O, *> = rootMutable

  override fun getStates(): Collection<TimedFrequencyTreeNode<I, O, *>> =
      DeepRecursiveFunction<
          Pair<PTANode<I, O>, MutableSet<PTANode<I, O>>>,
          MutableSet<PTANode<I, O>>,
      > { (node, collect) ->
        collect += node
        node.transitions.forEach { callRecursive(it.target to collect) }
        collect
      }(rootMutable to mutableSetOf())

  override fun getSuccessor(
      transition: FrequencyTransition<TimedFrequencyTreeNode<I, O, *>, I>
  ): TimedFrequencyTreeNode<I, O, *> = transition.target

  override fun getStateOutput(state: TimedFrequencyTreeNode<I, O, *>): O = state.output

  override fun getExitTimes(state: TimedFrequencyTreeNode<I, O, *>): Collection<Duration> =
      state.timings

  override fun getTransitions(
      state: TimedFrequencyTreeNode<I, O, *>,
      input: I,
  ): Collection<FrequencyTransition<TimedFrequencyTreeNode<I, O, *>, I>> =
      state.getTransitions(input)

  override fun getTransition(
      state: TimedFrequencyTreeNode<I, O, *>,
      input: I,
      output: O,
  ): FrequencyTransition<TimedFrequencyTreeNode<I, O, *>, I>? =
      state.getTransitionOrNull(input, output)

  override fun getTransitionFrequency(
      transition: FrequencyTransition<TimedFrequencyTreeNode<I, O, *>, I>
  ): Int = transition.frequency

  override fun getTransitionProbability(
      transition: FrequencyTransition<TimedFrequencyTreeNode<I, O, *>, I>
  ): Float = transition.probability
}
