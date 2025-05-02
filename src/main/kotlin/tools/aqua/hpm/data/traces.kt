// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.data

import kotlin.collections.plus
import kotlin.time.Duration

data class TimedIO<I, O>(val time: Duration, val input: I, val output: O)

data class TimedIOTrace<I, O>(val head: O, val tail: Iterable<TimedIO<I, O>> = emptyList())

val TimedIOTrace<*, *>.times: List<Duration>
  get() = tail.map { it.time }

val <I> TimedIOTrace<I, *>.inputs: List<I>
  get() = tail.map { it.input }

val <O> TimedIOTrace<*, O>.outputs: List<O>
  get() = listOf(head) + tail.map { it.output }

operator fun <I, O> TimedIOTrace<I, O>.plus(element: TimedIO<I, O>) = copy(tail = tail + element)

data class SimpleTrace<S, T>(val head: S, val tail: Iterable<Pair<T, S>> = emptyList())

val <S> SimpleTrace<S, *>.states: List<S>
  get() = listOf(head) + tail.map { (_, s) -> s }

val <T> SimpleTrace<*, T>.transitions: List<T>
  get() = tail.map { (t, _) -> t }

val <S> SimpleTrace<S, *>.lastState: S
  get() = tail.lastOrNull()?.second ?: head

operator fun <S, T> SimpleTrace<S, T>.plus(element: Pair<T, S>) = copy(tail = tail + element)
