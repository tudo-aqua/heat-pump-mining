// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.util

import java.math.BigInteger
import kotlin.collections.filterTo
import kotlin.math.ln
import kotlin.random.Random
import kotlin.random.asJavaRandom
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

fun <T> Random.selectWeighted(values: Map<T, Number>): T =
    values.entries
        .map { (entry, weight) -> entry to -ln(nextDouble()) / weight.toDouble() }
        .minWith(compareBy { (_, value) -> value })
        .first

fun <T> Random.selectOneOf(vararg elements: T): T = elements[nextInt(0, elements.size - 1)]

fun <T> Random.runOneOf(vararg elements: () -> T): T = selectOneOf(*elements)()

fun Random.nextBigInteger(range: ClosedRange<BigInteger>): BigInteger {
  require(!range.isEmpty())
  if (range.start == range.endInclusive) return range.start

  while (true) {
    val maybeResult = BigInteger(range.endInclusive.bitLength(), asJavaRandom())
    if (maybeResult in range) return maybeResult
  }
}

fun Random.nextDuration(range: ClosedRange<Duration>): Duration =
    (range.endInclusive - range.start).toComponents { s, ns ->
      val seconds = if (s == 0L) ZERO else nextLong(s).seconds
      val nanoseconds = if (ns == 0) ZERO else nextInt(ns).nanoseconds
      seconds + nanoseconds
    } + range.start

fun <T> Random.nextSubset(set: Set<T>): Set<T> = set.filterTo(mutableSetOf()) { nextBoolean() }

fun <T> Random.nextNonTrivialSubset(set: Set<T>): Set<T> {
  require(set.size > 1) { "no non-trivial subsets" }
  return runUntil(
      { nextSubset(set) },
      { it.isNotEmpty() && it.size != set.size },
  )
}
