// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
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

private inline fun <T> Duration.toComponents(
    action:
        (
            days: Long,
            hours: Int,
            minutes: Int,
            seconds: Int,
            milliseconds: Int,
            miroseconds: Int,
            nanoseconds: Int,
        ) -> T
): T {
  toComponents { days, hours, minutes, seconds, nanosecondsBig ->
    val milliseconds = nanosecondsBig / 1_000_000
    val microseconds = (nanosecondsBig / 1_000) % 1000
    val nanoseconds = nanosecondsBig % 1000
    return action(days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds)
  }
}

private val UNITS = listOf("d", "h", "m", "s", "ms", "us", "ns")
private val ROUNDING_THRESHOLD = listOf(Int.MAX_VALUE, 12, 30, 30, 500, 500, 500)

fun Duration.toApproximateString(subcomponents: Int): String {
  if (this == Duration.ZERO || isInfinite()) return toString()

  return buildString {
    if (isNegative()) append('-')
    absoluteValue.toComponents {
        days,
        hours,
        minutes,
        seconds,
        milliseconds,
        microsconds,
        nanoseconds ->
      val withUnits =
          listOf(days, hours, minutes, seconds, milliseconds, microsconds, nanoseconds) zip UNITS
      val renderedComponents =
          withUnits
              .dropWhile { (value, _) -> value.toLong() == 0L }
              .take(1 + subcomponents)
              .filter { (value, _) -> value.toLong() != 0L }

      if (renderedComponents.isEmpty()) {
        append(Duration.ZERO)
      } else {
        val (firstValue, firstUnit) = renderedComponents.first()
        append(firstValue, firstUnit)
        renderedComponents.drop(1).forEach { (value, unit) -> append(" ", value, unit) }
      }

      if (isNegative() && renderedComponents.size > 1) insert(1, '(').append(')')
    }
  }
}
