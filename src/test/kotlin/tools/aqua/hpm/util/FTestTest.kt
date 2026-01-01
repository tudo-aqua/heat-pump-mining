// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.util

import kotlin.time.Duration.Companion.seconds
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.FieldSource

internal class FTestTest {

  private companion object {
    @JvmStatic
    val cases =
        listOf(
            arguments(5, 100, 1, 10, 109.0 / 9_800.0),
            arguments(5, 10, 1, 100, 2_180.0 / 305_809.0),
            arguments(1, 100, 4, 10, 218.0 / 961.0),
            arguments(1, 10, 4, 100, 872_000.0 / 4_439_449.0),
        )
  }

  @ParameterizedTest
  @FieldSource("cases")
  fun `f-test significance correct`(
      redAverage: Int,
      redSupport: Int,
      blueAverage: Int,
      blueSupport: Int,
      epsilon: Double,
  ) {
    val redBlueAverageRatio = (redAverage.toDouble() / blueAverage.toDouble())
    assertThat(fTestSignificance(redBlueAverageRatio, redSupport, blueSupport))
        .isCloseTo(epsilon, 0.1.percent)
  }

  @ParameterizedTest
  @FieldSource("cases")
  fun `f-test similarity correct`(
      redAverage: Int,
      redSupport: Int,
      blueAverage: Int,
      blueSupport: Int,
      epsilon: Double,
  ) {
    assertThat(fTestSimilarity(redAverage.seconds, redSupport, blueAverage.seconds, blueSupport))
        .isCloseTo(epsilon, 0.1.percent)
  }

  @Test
  fun `f-test has high similarity with partial blue evidence`() {
    assertThat(fTestSimilarity(10.seconds, 0, 10.seconds, 10)).isOne()
  }

  @Test
  fun `f-test has high similarity with partial red evidence`() {
    assertThat(fTestSimilarity(10.seconds, 10, 10.seconds, 0)).isOne()
  }

  @Test
  fun `f-test has high similarity with no evidence`() {
    assertThat(fTestSimilarity(10.seconds, 0, 10.seconds, 0)).isOne()
  }

  @ParameterizedTest
  @FieldSource("cases")
  fun `f-test fails above bound`(
      redAverage: Int,
      redSupport: Int,
      blueAverage: Int,
      blueSupport: Int,
      epsilon: Double,
  ) {
    assertThat(
            fTest(redAverage.seconds, redSupport, blueAverage.seconds, blueSupport, epsilon * 1.01)
        )
        .isFalse
  }

  @ParameterizedTest
  @FieldSource("cases")
  fun `f-test succeeds below bound`(
      redAverage: Int,
      redSupport: Int,
      blueAverage: Int,
      blueSupport: Int,
      epsilon: Double,
  ) {
    assertThat(
            fTest(redAverage.seconds, redSupport, blueAverage.seconds, blueSupport, epsilon * 0.99)
        )
        .isTrue
  }

  @Test
  fun `f-test succeeds with partial blue evidence`() {
    assertThat(fTest(10.seconds, 0, 10.seconds, 10, 1.0)).isTrue
  }

  @Test
  fun `f-test succeeds with partial red evidence`() {
    assertThat(fTest(10.seconds, 10, 10.seconds, 0, 1.0)).isTrue
  }

  @Test
  fun `f-test succeeds with no evidence`() {
    assertThat(fTest(10.seconds, 0, 10.seconds, 0, 1.0)).isTrue
  }
}
