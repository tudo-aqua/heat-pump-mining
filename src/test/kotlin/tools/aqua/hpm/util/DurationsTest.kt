// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.util

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class DurationsTest {

  @Test
  fun `single-duration mean is correct`() {
    listOf(3.seconds).average().assertCloseTo(3.seconds)
  }

  @Test
  fun `multi-duration mean is correct`() {
    listOf(3.seconds, 4.seconds, 5.seconds).average().assertCloseTo(4.seconds)
  }

  @Test
  fun `complex mean is correct`() {
    listOf(3.seconds + 1000.nanoseconds, 5.seconds + 1000.nanoseconds)
        .average()
        .assertCloseTo(4.seconds + 1000.nanoseconds)
  }

  @Test
  fun `single-duration stddev is null`() {
    assertThat(listOf(3.seconds).standardDeviationOrNull()).isNull()
  }

  @Test
  fun `multi-duration stddev is correct`() {
    listOf(3.seconds, 4.seconds, 5.seconds).standardDeviation().assertCloseTo(1.seconds)
  }

  @Test
  fun `small bigint conversion is correct`() {
    assertThat(1.seconds.toBigIntegerNanoseconds()).isEqualTo(1_000_000_000L.toBigInteger())
  }

  @Test
  fun `small duration conversion is correct`() {
    assertThat(1_000_000_000L.toBigInteger().nanoseconds).isEqualTo(1.seconds)
  }

  @Test
  fun `big bigint conversion is correct`() {
    assertThat(1000.days.toBigIntegerNanoseconds())
        .isEqualTo(864.toBigInteger() * 10.toBigInteger().pow(14))
  }

  @Test
  fun `big duration conversion is correct`() {
    assertThat((864.toBigInteger() * 10.toBigInteger().pow(14)).nanoseconds).isEqualTo(1000.days)
  }
}
