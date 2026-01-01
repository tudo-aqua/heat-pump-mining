// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.rtioalergia

import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import tools.aqua.hpm.util.averageOrNull

interface TimedFrequencyNode<I, O, N : TimedFrequencyNode<I, O, N>> {
  val output: O
  val transitions: Collection<FrequencyTransition<N, I>>

  fun getTransitions(input: I): Collection<FrequencyTransition<N, I>>

  fun getTransitionOrNull(input: I, output: O): FrequencyTransition<N, I>?

  val timings: Collection<Duration>
}

val TimedFrequencyNode<*, *, *>.totalFrequency: Int
  get() = transitions.sumOf { it.frequency }

fun <I> TimedFrequencyNode<I, *, *>.totalFrequency(input: I): Int =
    getTransitions(input).sumOf { it.frequency }

fun <I, O> TimedFrequencyNode<I, O, *>.getProbability(input: I, output: O): Float =
    getTransitionOrNull(input, output)?.probability ?: 0.0f

val TimedFrequencyNode<*, *, *>.averageTiming: Duration
  get() = timings.averageOrNull() ?: ZERO

interface TimedFrequencyTreeNode<I, O, N : TimedFrequencyTreeNode<I, O, N>> :
    TimedFrequencyNode<I, O, N> {
  val accessTransition: FrequencyTransition<N, I>?
}

interface TimedFrequencyMergedNode<I, O, N : TimedFrequencyMergedNode<I, O, N>> :
    TimedFrequencyNode<I, O, N> {
  override val transitions: Collection<FrequencyMergedTransition<N, I>>

  override fun getTransitions(input: I): Collection<FrequencyMergedTransition<N, I>>

  override fun getTransitionOrNull(input: I, output: O): FrequencyMergedTransition<N, I>?

  val originalTimings: Collection<Duration>
}

val TimedFrequencyMergedNode<*, *, *>.totalOriginalFrequency: Int
  get() = transitions.sumOf { it.originalFrequency }

fun <I> TimedFrequencyMergedNode<I, *, *>.totalOriginalFrequency(input: I): Int =
    getTransitions(input).sumOf { it.originalFrequency }

fun <I, O> TimedFrequencyMergedNode<I, O, *>.getOriginalProbability(input: I, output: O): Float =
    getTransitionOrNull(input, output)?.originalProbability ?: 0.0f

val TimedFrequencyMergedNode<*, *, *>.averageOriginalTiming: Duration
  get() = originalTimings.averageOrNull() ?: ZERO

interface FrequencyTransition<out N, out I> {
  val source: N
  val input: I
  val target: N
  val frequency: Int
}

val <I, N : TimedFrequencyNode<I, *, *>> FrequencyTransition<N, I>.probability: Float
  get() = frequency.toFloat() / source.totalFrequency(input).toFloat()

interface FrequencyMergedTransition<out N, out I> : FrequencyTransition<N, I> {
  val originalFrequency: Int
}

val <I, N : TimedFrequencyMergedNode<I, *, *>> FrequencyMergedTransition<N, I>.originalProbability:
    Float
  get() = originalFrequency.toFloat() / source.totalOriginalFrequency(input).toFloat()
