// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.util

import java.math.BigInteger
import java.math.BigInteger.ZERO
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

private inline fun <T> Iterable<T>.sumOf(selector: (T) -> BigInteger): BigInteger =
    fold(ZERO) { acc, v -> acc + selector(v) }

fun Collection<Duration>.averageOrNull(): Duration? {
  if (isEmpty()) return null

  return (sumOf { it.toBigIntegerNanoseconds() } / size.toBigInteger()).nanoseconds
}

fun Collection<Duration>.average(): Duration =
    checkNotNull(averageOrNull()) { "collection must not be empty" }

fun Collection<Duration>.standardDeviationOrNull(): Duration? {
  if (size < 2) return null

  val mean = average().toBigIntegerNanoseconds()
  val varianceSum = sumOf { (it.toBigIntegerNanoseconds() - mean).pow(2) }
  val variance = varianceSum / (size - 1).toBigInteger()
  return variance.sqrt().nanoseconds
}

fun Collection<Duration>.standardDeviation(): Duration =
    checkNotNull(standardDeviationOrNull()) { "collection must have at least two elements" }

private val NS_PER_S = 1.seconds.inWholeNanoseconds.toBigInteger()

fun Duration.toBigIntegerNanoseconds(): BigInteger = toComponents { s, ns ->
  s.toBigInteger() * NS_PER_S + ns.toBigInteger()
}

val BigInteger.nanoseconds: Duration
  get() {
    val (s, ns) = this.divideAndRemainder(NS_PER_S)
    return s.toLong().seconds + ns.toLong().nanoseconds
  }
