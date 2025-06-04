// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.rtioalergia

import de.learnlib.datastructure.pta.config.DefaultProcessingOrders
import de.learnlib.datastructure.pta.config.DefaultProcessingOrders.CANONICAL_ORDER
import de.learnlib.datastructure.pta.config.DefaultProcessingOrders.FIFO_ORDER
import de.learnlib.datastructure.pta.config.DefaultProcessingOrders.LEX_ORDER
import de.learnlib.datastructure.pta.config.DefaultProcessingOrders.LIFO_ORDER
import java.util.ArrayDeque as JArrayDeque
import java.util.Collections.asLifoQueue
import java.util.Comparator.comparing
import java.util.PriorityQueue
import kotlin.time.Duration
import tools.aqua.hpm.automata.DeterministicFrequencyProbabilisticTimedInputOutputAutomaton
import tools.aqua.hpm.util.TwoStageHashMap
import tools.aqua.hpm.util.fTest
import tools.aqua.hpm.util.hoeffdingTest

/**
 * An I/O access string for a given red or blue state. The string consists of the [head], i.e., the
 * initial state's output, and the [tail], describing the remaining alternating input-output
 * sequence. This is compared according to string comparison rules, i.e., shorter strings are
 * smaller.
 */
private data class AccessString<I : Comparable<I>, O : Comparable<O>>(
    val head: O,
    val tail: List<Pair<I, O>> = emptyList()
) : Comparable<AccessString<I, O>> {
  override fun compareTo(other: AccessString<I, O>): Int {
    (tail.size - other.tail.size).let { if (it != 0) return it }
    head.compareTo(other.head).let { if (it != 0) return it }

    (tail zip other.tail).forEach { (ours, theirs) ->
      val (ourInput, ourOutput) = ours
      val (theirInput, theirOutput) = theirs
      ourInput.compareTo(theirInput).let { if (it != 0) return it }
      ourOutput.compareTo(theirOutput).let { if (it != 0) return it }
    }

    return 0
  }

  /** Lexicographic comparison, where longer strings are smaller. */
  fun lexCompareTo(other: AccessString<I, O>): Int =
      (other.tail.size - tail.size).let { if (it != 0) it else compareTo(other) }

  /** Append [element] to this string, returning the longer string. */
  operator fun plus(element: Pair<I, O>): AccessString<I, O> =
      copy(
          tail = tail + element,
      )

  override fun toString(): String =
      (listOf(head) + tail.flatMap { (i, o) -> listOf(i, o) }).joinToString(
          prefix = "[", postfix = "]")
}

@ConsistentCopyVisibility
private data class AlergiaTransition<I : Comparable<I>, O : Comparable<O>>
private constructor(
    private val mergedTransitions: MutableList<FrequencyTransition<*, I>>,
    override val source: AlergiaState<I, O>,
    override val target: AlergiaState<I, O>,
) : FrequencyMergedTransition<AlergiaState<I, O>, I> {

  init {
    require(mergedTransitions.isNotEmpty())
  }

  constructor(
      originalTransition: FrequencyTransition<*, I>,
      source: AlergiaState<I, O>,
      target: AlergiaState<I, O>,
  ) : this(mutableListOf(originalTransition), source, target)

  override val input: I = mergedTransitions.first().input
  val output: O = target.output

  override val originalFrequency: Int = mergedTransitions.first().frequency
  override val frequency: Int
    get() = mergedTransitions.sumOf { it.frequency }

  init {
    source.addTransition(this)
  }

  fun mergeInto(target: AlergiaTransition<I, O>): AlergiaTransition<I, O> {
    require(target != this) { "self-merge of $this" }
    require(target.input == input)
    this.source.removeTransition(this)
    target.mergedTransitions += mergedTransitions
    return target
  }

  fun moveTo(
      source: AlergiaState<I, O> = this.source,
      target: AlergiaState<I, O> = this.target
  ): AlergiaTransition<I, O> {
    require(source != this.source || target != this.target) { "self-move of $this" }
    this.source.removeTransition(this)
    return AlergiaTransition(mergedTransitions, source, target)
  }
}

private class AlergiaState<I : Comparable<I>, O : Comparable<O>>(
    private val originalNode: TimedFrequencyTreeNode<I, O, *>,
) : TimedFrequencyMergedNode<I, O, AlergiaState<I, O>> {
  private val mergedNodes = mutableListOf<TimedFrequencyNode<I, O, *>>(originalNode)

  private var accessStringMutable: AccessString<I, O>? = null

  private var accessTransitionMutable: AlergiaTransition<I, O>? = null

  val isPromoted: Boolean
    get() = accessStringMutable != null

  fun promoteToRoot() {
    check(!isPromoted) { "re-parenting at root" }
    accessStringMutable = AccessString(originalNode.output)
  }

  fun promoteWithParent(parentTransition: AlergiaTransition<I, O>) {
    check(!isPromoted) { "re-parenting with transition $parentTransition" }
    accessStringMutable =
        parentTransition.source.accessString +
            (originalNode.accessTransition!!.input to originalNode.output)
    accessTransitionMutable = parentTransition
  }

  val accessString: AccessString<I, O>
    get() = checkNotNull(accessStringMutable) { "access string of unpromoted node not available" }

  val accessTransition: AlergiaTransition<I, O>?
    get() {
      check(isPromoted) { "access transition of unpromoted node not available" }
      return accessTransitionMutable
    }

  private val transitionsMutable = TwoStageHashMap<I, O, AlergiaTransition<I, O>>()
  override val transitions: Collection<AlergiaTransition<I, O>> = transitionsMutable

  operator fun get(input: I, output: O): AlergiaTransition<I, O>? =
      transitionsMutable[input, output]

  override fun getTransitionOrNull(
      input: I,
      output: O
  ): FrequencyMergedTransition<AlergiaState<I, O>, I>? = transitionsMutable[input, output]

  override fun getTransitions(
      input: I
  ): Collection<FrequencyMergedTransition<AlergiaState<I, O>, I>> = transitionsMutable[input]

  fun addTransition(transition: AlergiaTransition<I, O>) {
    require(transition.source == this) { "transition $transition is foreign" }
    require(transitionsMutable.put(transition.input, transition.output, transition) == null) {
      "transition $transition already existed"
    }
  }

  fun removeTransition(transition: AlergiaTransition<I, O>) {
    require(transition.source == this) { "transition $transition is foreign" }
    require(transitionsMutable.remove(transition.input, transition.output) != null) {
      "transition $transition was not present"
    }
  }

  override val output: O = originalNode.output

  override val originalTimings: Collection<Duration> = originalNode.timings

  override val timings: Collection<Duration>
    get() = mergedNodes.flatMap { it.timings }

  fun mergeInto(target: AlergiaState<I, O>) {
    require(target.output == output)
    require(target != this) { "self-merge of $this" }
    target.mergedNodes += mergedNodes
  }

  override fun toString(): String = "AlergiaState@${hashCode()}[output=$output]"
}

private infix fun <I : Comparable<I>, O : Comparable<O>> AlergiaState<I, O>.transitionPairs(
    other: AlergiaState<I, O>
) = iterator {
  transitions.forEach { ourT ->
    other.getTransitionOrNull(ourT.input, ourT.target.output)?.let { otherT ->
      yield(ourT to otherT)
    }
  }
}

class RTIOAlergiaMergedAutomaton<I : Comparable<I>, O : Comparable<O>>(
    pta: TimedFrequencyPTA<I, O>,
    blueStateOrder: DefaultProcessingOrders = LEX_ORDER,
    private val parallel: Boolean = false,
    private val deterministic: Boolean = true,
    private val frequencySimilaritySignificance: Double = 0.05,
    private val frequencySimilaritySignificanceDecay: Double = 1.0,
    private val timingSimilaritySignificance: Double = 0.05,
    private val timingSimilaritySignificanceDecay: Double = 1.0,
    private val tailLength: Int? = null,
    private val analyzeMergedSamples: Boolean = false,
) :
    DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<
        TimedFrequencyNode<I, O, *>, I, FrequencyTransition<TimedFrequencyNode<I, O, *>, I>, O> {
  init {
    require(frequencySimilaritySignificance in 0.0..1.0) {
      "Frequency similiarity significance must be in [0, 1])"
    }
    require(frequencySimilaritySignificanceDecay in 0.0..1.0) {
      "Frequency similiarity significance decay must be in [0, 1]"
    }
    require(timingSimilaritySignificance in 0.0..1.0) {
      "Timing similiarity significance must be in [0, 1]"
    }
    require(timingSimilaritySignificanceDecay in 0.0..1.0) {
      "Timing similiarity significance decay must be in [0, 1]"
    }
  }

  private val initialState =
      DeepRecursiveFunction<TimedFrequencyTreeNode<I, O, *>, AlergiaState<I, O>> { node ->
        AlergiaState(node).also { state ->
          node.transitions.forEach {
            // transitions register themselves
            AlergiaTransition(it, state, callRecursive(it.target))
          }
        }
      }(pta.getInitialState())

  init {
    initialState.promoteToRoot()
  }

  private val redStates = mutableListOf(initialState)

  private val blueStates =
      when (blueStateOrder) {
        CANONICAL_ORDER -> PriorityQueue<AlergiaState<I, O>>(comparing { it.accessString })
        LEX_ORDER ->
            PriorityQueue(
                comparing({ it.accessString }, { string, other -> string.lexCompareTo(other) }))
        FIFO_ORDER -> JArrayDeque()
        LIFO_ORDER -> asLifoQueue(JArrayDeque())
      }

  init {
    promoteSuccessors(initialState)
  }

  init {
    while (blueStates.isNotEmpty()) {
      mergeOrPromoteNext(blueStates.poll())
    }
  }

  private fun mergeOrPromoteNext(blueState: AlergiaState<I, O>) {
    val reds = if (parallel) redStates.parallelStream() else redStates.stream()
    val mergeableRed = reds.filter { isMergeCompatible(it, blueState) }
    val redOrEmpty = if (deterministic) mergeableRed.findFirst() else mergeableRed.findAny()

    if (redOrEmpty.isEmpty) {
      promote(blueState)
    } else {
      mergeRecursively(redOrEmpty.get(), blueState)
    }
  }

  private data class MergeExplorationTask<I : Comparable<I>, O : Comparable<O>>(
      val target: AlergiaState<I, O>,
      val source: AlergiaState<I, O>,
      val frequencySimilaritySignificanceLocal: Double,
      val timingSimilaritySignificanceLocal: Double,
      val remainingTail: Int?,
  )

  private fun isMergeCompatible(target: AlergiaState<I, O>, source: AlergiaState<I, O>): Boolean =
      DeepRecursiveFunction<MergeExplorationTask<I, O>, Boolean> { task ->
        val (target, source, frequencySSL, timingSSL, tail) = task

        // always check output compatibility
        if (target.output != source.output) {
          return@DeepRecursiveFunction false
        }

        // if tail is not exceeded: check stochastic compatibility
        if (tail != 0 && !isMergeStochasticallyValid(target, source, frequencySSL, timingSSL)) {
          return@DeepRecursiveFunction false
        }

        // recurse into children
        (source transitionPairs target).forEach { (sourceT, targetT) ->
          // if a single child cannot merge, abort
          if (!callRecursive(
              MergeExplorationTask(
                  targetT.target,
                  sourceT.target,
                  frequencySSL * frequencySimilaritySignificanceDecay,
                  timingSSL * timingSimilaritySignificanceDecay,
                  tail?.let { (it - 1).coerceAtLeast(0) }))) {
            return@DeepRecursiveFunction false
          }
        }
        return@DeepRecursiveFunction true
      }(
          MergeExplorationTask(
              target,
              source,
              frequencySimilaritySignificance,
              timingSimilaritySignificance,
              tailLength))

  private fun isMergeStochasticallyValid(
      target: AlergiaState<I, O>,
      source: AlergiaState<I, O>,
      frequencySimilaritySignificanceLocal: Double,
      timingSimilaritySignificanceLocal: Double
  ): Boolean {
    if (fTest(
            if (analyzeMergedSamples) target.averageTiming else target.averageOriginalTiming,
            if (analyzeMergedSamples) target.totalFrequency else target.totalOriginalFrequency,
            if (analyzeMergedSamples) source.averageTiming else source.averageOriginalTiming,
            if (analyzeMergedSamples) source.totalFrequency else source.totalOriginalFrequency,
            timingSimilaritySignificanceLocal)
        .not())
        return false

    (source transitionPairs target).forEach { (sourceT, targetT) ->
      if (hoeffdingTest(
              if (analyzeMergedSamples) targetT.frequency else targetT.originalFrequency,
              if (analyzeMergedSamples) target.totalFrequency(targetT.input)
              else target.totalOriginalFrequency(targetT.input),
              if (analyzeMergedSamples) sourceT.frequency else sourceT.originalFrequency,
              if (analyzeMergedSamples) source.totalFrequency(sourceT.input)
              else source.totalOriginalFrequency(sourceT.input),
              frequencySimilaritySignificanceLocal)
          .not())
          return false
    }

    return true
  }

  private data class MergeTask<I : Comparable<I>, O : Comparable<O>>(
      val targetParent: AlergiaState<I, O>,
      val target: AlergiaState<I, O>,
      val sourceAccess: AlergiaTransition<I, O>,
      val source: AlergiaState<I, O>,
  )

  private fun mergeRecursively(target: AlergiaState<I, O>, source: AlergiaState<I, O>) {
    require(source != target) { "self-merge of $source" }
    val access = requireNotNull(source.accessTransition) { "cannot merge root away" }
    DeepRecursiveFunction<MergeTask<I, O>, Unit> {
        (
            targetParent,
            target,
            sourceAccess,
            source,
        ) ->
      check(source !in redStates) { "merging looped back to red state $source" }

      blueStates.remove(source)

      // merge timing information
      source.mergeInto(target)

      val targetAccess = targetParent[sourceAccess.input, source.output]
      if (targetAccess != null && targetAccess != sourceAccess) {
        // target has a matching inbound transition
        sourceAccess.mergeInto(targetAccess)
      } else {
        // target has no matching inbound transition, create one
        sourceAccess.moveTo(targetParent, target)
      }

      // copy to avoid concurrent modification exceptions
      source.transitions.toList().forEach { sourceTransition ->
        val sourceSuccessor = sourceTransition.target
        val targetTransition = target[sourceTransition.input, sourceTransition.output]
        if (targetTransition != null) {
          callRecursive(
              MergeTask(target, targetTransition.target, sourceTransition, sourceTransition.target))
        } else {
          sourceTransition.moveTo(target, sourceSuccessor)
        }
      }
    }(MergeTask(access.source, target, access, source))

    redStates.forEach(::promoteSuccessors)
  }

  private fun promote(state: AlergiaState<I, O>) {
    check(state !in redStates) { "red state $state re-promoted" }
    redStates += state
    promoteSuccessors(state)
  }

  private fun promoteSuccessors(state: AlergiaState<I, O>) {
    state.transitions.forEach { outgoing ->
      outgoing.target.let { successor ->
        if (!successor.isPromoted) {
          successor.promoteWithParent(outgoing)
          blueStates += successor
        }
      }
    }
  }

  override fun getStates(): Collection<TimedFrequencyNode<I, O, *>> = redStates

  override fun getInitialState(): TimedFrequencyNode<I, O, *> = initialState

  override fun getStateOutput(state: TimedFrequencyNode<I, O, *>): O = state.output

  override fun getExitTime(state: TimedFrequencyNode<I, O, *>): Duration = state.averageTiming

  override fun getTransitions(
      state: TimedFrequencyNode<I, O, *>,
      input: I
  ): Collection<FrequencyTransition<TimedFrequencyNode<I, O, *>, I>> = state.getTransitions(input)

  override fun getSuccessor(
      transition: FrequencyTransition<TimedFrequencyNode<I, O, *>, I>
  ): TimedFrequencyNode<I, O, *> = transition.target

  override fun getTransitionProbability(
      transition: FrequencyTransition<TimedFrequencyNode<I, O, *>, I>
  ): Float = transition.probability

  override fun getTransitionFrequency(
      transition: FrequencyTransition<TimedFrequencyNode<I, O, *>, I>
  ): Int = transition.frequency

  override fun getTransition(
      state: TimedFrequencyNode<I, O, *>,
      input: I,
      output: O
  ): FrequencyTransition<TimedFrequencyNode<I, O, *>, I>? = state.getTransitionOrNull(input, output)
}
