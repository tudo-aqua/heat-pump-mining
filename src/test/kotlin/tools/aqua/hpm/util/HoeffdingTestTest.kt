// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.util

import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.FieldSource

internal class HoeffdingTestTest {

  private companion object {
    @JvmStatic
    val cases =
        listOf(
            arguments(10, 40, 30, 50, exp(-49.0 / (2.0 + sqrt(5.0)).pow(2))),
            arguments(30, 40, 10, 50, exp(-121.0 / (2.0 + sqrt(5.0)).pow(2))),
            arguments(10, 400, 30, 500, exp(49.0 / 10.0 * (4.0 * sqrt(5.0) - 9.0))),
            arguments(10, 500, 30, 400, exp(121.0 / 10.0 * (4.0 * sqrt(5.0) - 9.0))),
        )
  }

  @ParameterizedTest
  @FieldSource("cases")
  fun `Hoeffding test significance correct`(
      frequency1: Int,
      totalFrequency1: Int,
      frequency2: Int,
      totalFrequency2: Int,
      epsilon: Double
  ) {
    assertThat(hoeffdingSignificance(frequency1, totalFrequency1, frequency2, totalFrequency2))
        .isCloseTo(epsilon, 0.1.percent)
  }

  @ParameterizedTest
  @FieldSource("cases")
  fun `Hoeffding test similarity correct`(
      frequency1: Int,
      totalFrequency1: Int,
      frequency2: Int,
      totalFrequency2: Int,
      epsilon: Double
  ) {
    assertThat(hoeffdingSimilarity(frequency1, totalFrequency1, frequency2, totalFrequency2))
        .isCloseTo(epsilon, 0.1.percent)
  }

  @Test
  fun `Hoeffding test has no similarity with partial 1 evidence`() {
    assertThat(hoeffdingSimilarity(0, 0, 10, 10)).isZero()
  }

  @Test
  fun `Hoeffding test has no similarity with partial 2 evidence`() {
    assertThat(hoeffdingSimilarity(10, 10, 0, 0)).isZero()
  }

  @Test
  fun `Hoeffding test has high similarity with no evidence`() {
    assertThat(hoeffdingSimilarity(0, 0, 0, 0)).isOne()
  }

  @ParameterizedTest
  @FieldSource("cases")
  fun `Hoeffding test fails above bound`(
      frequency1: Int,
      totalFrequency1: Int,
      frequency2: Int,
      totalFrequency2: Int,
      epsilon: Double
  ) {
    assertThat(
            hoeffdingTest(frequency1, totalFrequency1, frequency2, totalFrequency2, epsilon * 1.01))
        .isFalse
  }

  @ParameterizedTest
  @FieldSource("cases")
  fun `Hoeffding test succeeds below bound`(
      frequency1: Int,
      totalFrequency1: Int,
      frequency2: Int,
      totalFrequency2: Int,
      epsilon: Double
  ) {
    assertThat(
            hoeffdingTest(frequency1, totalFrequency1, frequency2, totalFrequency2, epsilon * 0.99))
        .isTrue
  }

  @Test
  fun `Hoeffding test succeeds with partial 1 evidence`() {
    assertThat(hoeffdingTest(10, 10, 0, 0, 1.0)).isTrue
  }

  @Test
  fun `Hoeffding test succeeds with partial 2 evidence`() {
    assertThat(hoeffdingTest(0, 0, 10, 10, 1.0)).isTrue
  }

  @Test
  fun `Hoeffding test succeeds with no evidence`() {
    assertThat(hoeffdingTest(0, 0, 0, 0, 1.0)).isTrue
  }
}
