// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.util

import java.math.BigInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import org.apache.commons.math3.stat.StatUtils.mean
import org.apache.commons.math3.stat.StatUtils.variance

inline fun Collection<Duration>.computeAdditiveOrNull(compute: (DoubleArray) -> Double): Duration? {
  if (isEmpty()) return null
  val seconds = DoubleArray(size)
  val nanoseconds = DoubleArray(size)
  forEachIndexed { idx, duration ->
    duration.toComponents { s, ns ->
      seconds[idx] = s.toDouble()
      nanoseconds[idx] = ns.toDouble()
    }
  }
  return compute(seconds).seconds + compute(nanoseconds).nanoseconds
}

fun Collection<Duration>.averageOrNull(): Duration? = computeAdditiveOrNull(::mean)

fun Collection<Duration>.average(): Duration =
    checkNotNull(averageOrNull()) { "collection must not be empty" }

fun Collection<Duration>.standardDeviationOrNull(): Duration? =
    varianceOrNull()?.toBigIntegerNanoseconds()?.sqrt()?.toDouble()?.nanoseconds

fun Collection<Duration>.standardDeviation(): Duration =
    checkNotNull(standardDeviationOrNull()) { "collection must not be empty" }

fun Collection<Duration>.varianceOrNull(): Duration? = computeAdditiveOrNull(::variance)

fun Collection<Duration>.variance(): Duration =
    checkNotNull(varianceOrNull()) { "collection must not be empty" }

fun Duration.toBigIntegerNanoseconds(): BigInteger = toComponents { s, ns ->
  s.toBigInteger() * 1.seconds.inWholeNanoseconds.toBigInteger() + ns.toBigInteger()
}
