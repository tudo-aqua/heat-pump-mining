// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.util

import java.math.BigInteger
import java.math.BigInteger.ONE
import java.util.Objects.hash

infix fun <T : Comparable<T>> T.orderedRangeTo(other: T) =
    if (this <= other) this..other else other..this

class BigIntegerRange(override val start: BigInteger, override val endInclusive: BigInteger) :
    Iterable<BigInteger>, ClosedRange<BigInteger> {

  override fun iterator(): Iterator<BigInteger> = iterator {
    var current = start
    while (current <= endInclusive) {
      yield(current)
      current += ONE
    }
  }

  override fun equals(other: Any?): Boolean =
      when {
        this === other -> true
        other !is BigIntegerRange -> false
        else ->
            isEmpty() && other.isEmpty() ||
                start == other.start && endInclusive == other.endInclusive
      }

  override fun hashCode(): Int = if (isEmpty()) -1 else hash(start, endInclusive)

  override fun toString(): String = "$start..$endInclusive"
}

operator fun BigInteger.rangeTo(other: BigInteger): BigIntegerRange = BigIntegerRange(this, other)
