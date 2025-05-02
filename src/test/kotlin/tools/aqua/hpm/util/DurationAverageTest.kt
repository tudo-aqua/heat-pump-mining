// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.util

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.SECONDS
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class DurationAverageTest {

  @Test
  fun `average of empty is null`() {
    assertThat(listOf<Duration>().averageOrNull()).isNull()
  }

  @Test
  fun `average of one is correct`() {
    assertThat(listOf(5.seconds).average().toDouble(SECONDS)).isCloseTo(5.0, 0.01.percent)
  }

  @Test
  fun `average of two is correct`() {
    assertThat(listOf(2.seconds, 4.seconds).average().toDouble(SECONDS))
        .isCloseTo(3.0, 0.01.percent)
  }

  @Test
  fun `average of three is correct`() {
    assertThat(listOf(1.seconds, 7.seconds, 10.seconds).average().toDouble(SECONDS))
        .isCloseTo(6.0, 0.01.percent)
  }

  @Test
  fun `average of mixed units is correct`() {
    assertThat(listOf(30.seconds, 1.minutes).average().toDouble(SECONDS))
        .isCloseTo(45.0, 0.01.percent)
  }
}
