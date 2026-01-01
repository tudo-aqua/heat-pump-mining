// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.data

import kotlin.collections.plus
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import tools.aqua.hpm.util.prefixes
import tools.aqua.hpm.util.subListFrom
import tools.aqua.hpm.util.subListTo
import tools.aqua.hpm.util.suffixes

data class TimedIO<I, O>(val time: Duration, val input: I, val output: O)

fun <I, O> Iterable<TimedIO<I, O>>.withAbsoluteTimes(): Sequence<Pair<Duration, TimedIO<I, O>>> =
    sequence {
      var time = ZERO
      forEach {
        yield(time to it)
        time += it.time
      }
    }

data class TimedIOTrace<I, O>(val head: O, val tail: List<TimedIO<I, O>> = emptyList()) {
  val size: Int = tail.size
}

val TimedIOTrace<*, *>.times: List<Duration>
  get() = tail.map { it.time }

val <I> TimedIOTrace<I, *>.inputs: List<I>
  get() = tail.map { it.input }

val <O> TimedIOTrace<*, O>.outputs: List<O>
  get() = listOf(head) + tail.map { it.output }

fun <I, O> TimedIOTrace<I, O>.subTrace(fromIndex: Int, toIndex: Int): TimedIOTrace<I, O> =
    copy(tail = tail.subList(fromIndex, toIndex))

fun <I, O> TimedIOTrace<I, O>.subTraceFrom(fromIndex: Int): TimedIOTrace<I, O> =
    copy(tail = tail.subListFrom(fromIndex))

fun <I, O> TimedIOTrace<I, O>.subTraceTo(toIndex: Int): TimedIOTrace<I, O> =
    copy(tail = tail.subListTo(toIndex))

val <I, O> TimedIOTrace<I, O>.prefixes: List<TimedIOTrace<I, O>>
  get() = tail.prefixes.map { copy(tail = it) }

val <I, O> TimedIOTrace<I, O>.suffixes: List<TimedIOTrace<I, O>>
  get() = tail.suffixes.map { copy(tail = it) }

operator fun <I, O> TimedIOTrace<I, O>.plus(element: TimedIO<I, O>) = copy(tail = tail + element)

data class SimpleTrace<S, T>(val head: S, val tail: Iterable<Pair<T, S>> = emptyList())

val <S> SimpleTrace<S, *>.states: List<S>
  get() = listOf(head) + tail.map { (_, s) -> s }

val <T> SimpleTrace<*, T>.transitions: List<T>
  get() = tail.map { (t, _) -> t }

val <S> SimpleTrace<S, *>.lastState: S
  get() = tail.lastOrNull()?.second ?: head

operator fun <S, T> SimpleTrace<S, T>.plus(element: Pair<T, S>) = copy(tail = tail + element)
