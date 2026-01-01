// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.util

import kotlin.invoke
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories.COLLECTION
import org.assertj.core.api.InstanceOfAssertFactories.FLOAT
import org.assertj.core.data.Percentage
import org.assertj.core.data.Percentage.withPercentage
import tools.aqua.hpm.automata.DeterministicFrequencyProbabilisticTimedInputOutputAutomaton
import tools.aqua.hpm.rtioalergia.FrequencyTransition
import tools.aqua.hpm.rtioalergia.TimedFrequencyNode
import tools.aqua.hpm.rtioalergia.averageTiming
import tools.aqua.hpm.rtioalergia.probability

internal val Number.percent: Percentage
  get() = withPercentage(toDouble())

internal infix fun <A, B, C> Iterable<Pair<A, B>>.zip(right: Iterable<C>): List<Triple<A, B, C>> =
    this.zip(right) { a, b, c -> Triple(a, b, c) }

internal inline fun <A, B, C, V> Iterable<Pair<A, B>>.zip(
    other: Iterable<C>,
    transform: (a: A, b: B, c: C) -> V,
): List<V> =
    buildList(minOf(size(10), other.size(10))) {
      val first = this@zip.iterator()
      val second = other.iterator()
      while (first.hasNext() && second.hasNext()) {
        val (left, middle) = first.next()
        this += transform(left, middle, second.next())
      }
    }

internal fun <T> Iterable<T>.size(default: Int): Int =
    if (this is Collection<*>) this.size else default

internal fun Duration.assertCloseTo(reference: Duration) {
  assertThat(toBigIntegerNanoseconds()).isCloseTo(reference.toBigIntegerNanoseconds(), 0.01.percent)
}

internal val <T> T?.assertedNonNull: T
  get() {
    assertThat(this).isNotNull
    return this!!
  }

internal val <T> Iterable<T>.assertedSingle: T
  get() {
    assertThat(this).hasSize(1)
    return single()
  }

internal val <T> Iterable<T>.assertedDouble: Pair<T, T>
  get() {
    assertThat(this).hasSize(2)
    return first() to drop(1).first()
  }

internal fun <S : TimedFrequencyNode<I, O, *>, I, O> S.assertStateData(
    automaton:
        DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<
            S,
            I,
            FrequencyTransition<S, I>,
            O,
        >,
    output: O,
    exitTime: Duration?,
    timings: Collection<Duration>,
    nTransitions: Int,
) {
  assertThat(this).extracting { it.output }.isEqualTo(output)
  assertThat(this).extracting { automaton.getStateOutput(it) }.isEqualTo(output)

  if (exitTime == null) {
    assertThat(automaton.getExitTimes(this).averageOrNull()).isNull()
    assertThat(averageTiming).isEqualTo(ZERO)
  } else {
    automaton.getExitTimes(this).average().assertCloseTo(exitTime)
    averageTiming.assertCloseTo(exitTime)
  }

  assertThat(this)
      .extracting { it.timings }
      .asInstanceOf(COLLECTION)
      .containsExactlyInAnyOrderElementsOf(timings)

  assertThat(this).extracting { it.transitions }.asInstanceOf(COLLECTION).hasSize(nTransitions)
}

internal fun <S : TimedFrequencyNode<I, O, *>, I, O> FrequencyTransition<S, I>.assertTransitionData(
    automaton:
        DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<
            S,
            I,
            FrequencyTransition<S, I>,
            O,
        >,
    input: I,
    frequency: Int,
    probability: Float,
) {
  assertThat(this).extracting { it.input }.isEqualTo(input)

  assertThat(this).extracting { it.frequency }.isEqualTo(frequency)
  assertThat(this).extracting { automaton.getTransitionFrequency(it) }.isEqualTo(frequency)

  assertThat(this)
      .extracting { it.probability }
      .asInstanceOf(FLOAT)
      .isCloseTo(probability, 0.01.percent)
  assertThat(this)
      .extracting { automaton.getTransitionProbability(it) }
      .asInstanceOf(FLOAT)
      .isCloseTo(probability, 0.01.percent)
}

internal sealed interface StateRef<I, O> {
  val output: O
}

internal data class KnownState<I, O>(val level: Int, override val output: O) : StateRef<I, O>

internal data class State<I, O>(
    override val output: O,
    val exitTime: Duration?,
    val timings: Collection<Duration>,
    val transitions: List<Transition<I, O>>,
) : StateRef<I, O> {
  constructor(
      output: O,
      exitTime: Duration?,
      timings: Collection<Duration>,
      vararg transitions: Transition<I, O>,
  ) : this(output, exitTime, timings, transitions.asList())
}

internal data class Transition<I, O>(
    val input: I,
    val frequency: Int,
    val probability: Float,
    val successor: StateRef<I, O>,
)

private data class AssertionState<S : TimedFrequencyNode<I, O, *>, I, O>(
    val stateStack: List<S>,
    val stateRef: StateRef<I, O>,
) {
  constructor(state: S, stateRef: StateRef<I, O>) : this(listOf(state), stateRef)
}

internal fun <
    S : TimedFrequencyNode<I, O, *>,
    I,
    O,
> DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<S, I, FrequencyTransition<S, I>, O>
    .assertAutomatonStructure(initial: State<I, O>) {
  val a = this
  DeepRecursiveFunction<AssertionState<S, I, O>, Unit> { (qStack, qRef) ->
    val q = qStack.last()
    when (qRef) {
      is KnownState<I, O> -> assertThat(q).isEqualTo(qStack[qRef.level])
      is State<I, O> -> {
        val (output, exitTime, timings, transitions) = qRef
        q.assertStateData(a, output, exitTime, timings, transitions.size)
        transitions.forEach { tRef ->
          val (input, frequency, probability, succ) = tRef
          val t = a.getTransition(q, input, succ.output).assertedNonNull
          t.assertTransitionData(a, input, frequency, probability)
          callRecursive(AssertionState(qStack + t.target, succ))
        }
      }
    }
  }(AssertionState(getInitialState().assertedNonNull, initial))
}
