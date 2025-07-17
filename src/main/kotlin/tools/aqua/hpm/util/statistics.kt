// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.util

import kotlin.Double.Companion.POSITIVE_INFINITY
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO

fun hoeffdingTest(
    frequency1: Int,
    totalFrequency1: Int,
    frequency2: Int,
    totalFrequency2: Int,
    epsilon: Double
): Boolean {
  require(epsilon in 0.0..1.0) { "epsilon must be in [0, 1], is $epsilon" }
  return if (totalFrequency1 == 0 || totalFrequency2 == 0) {
    true
  } else {
    hoeffdingSignificance(frequency1, totalFrequency1, frequency2, totalFrequency2) > epsilon
  }
}

fun hoeffdingSimilarity(
    frequency1: Int,
    totalFrequency1: Int,
    frequency2: Int,
    totalFrequency2: Int
): Double =
    when {
      totalFrequency1 == 0 && totalFrequency2 == 0 -> 1.0
      totalFrequency1 == 0 || totalFrequency2 == 0 -> 0.0
      else -> hoeffdingSignificance(frequency1, totalFrequency1, frequency2, totalFrequency2)
    }

fun hoeffdingSignificance(
    frequency1: Int,
    totalFrequency1: Int,
    frequency2: Int,
    totalFrequency2: Int
): Double {
  require(0 < totalFrequency1) { "n1 must be strictly positive, is $totalFrequency1" }
  require(frequency1 <= totalFrequency1) { "f1 must be leq n1, is $frequency1 > $totalFrequency1" }
  require(0 < totalFrequency2) { "n2 must be strictly positive, is $totalFrequency2" }
  require(frequency2 <= totalFrequency2) { "f2 must be leq n2, is $frequency2 > $totalFrequency2" }

  val f1 = frequency1.toDouble()
  val n1 = totalFrequency1.toDouble()
  val f2 = frequency2.toDouble()
  val n2 = totalFrequency2.toDouble()

  val probabilityDifference = abs(f1 / n1 - f2 / n2)
  val supportWeight = sqrt(1 / n1) + sqrt(1 / n2)
  return exp(-2 * (probabilityDifference / supportWeight).pow(2))
}

fun fTest(
    redAverage: Duration,
    redSupport: Int,
    blueAverage: Duration,
    blueSupport: Int,
    epsilon: Double
): Boolean {
  require(epsilon >= 0) { "epsilon must be positive, is $epsilon" }
  if (redSupport <= 2 || blueSupport <= 1) return true
  return fTestSignificance(redAverage, redSupport, blueAverage, blueSupport) >= epsilon
}

fun fTestSignificance(
    redAverage: Duration,
    redSupport: Int,
    blueAverage: Duration,
    blueSupport: Int
): Double =
    if (redAverage == ZERO && blueAverage == ZERO) {
      POSITIVE_INFINITY
    } else {
      fTestSignificance(redAverage / blueAverage, redSupport, blueSupport)
    }

fun fTestSimilarity(
    redAverage: Duration,
    redSupport: Int,
    blueAverage: Duration,
    blueSupport: Int
): Double =
    when {
      redSupport <= 1 || blueSupport <= 2 -> 1.0
      redAverage == ZERO && blueAverage == ZERO -> 1.0
      else -> fTestSignificance(redAverage / blueAverage, redSupport, blueSupport).coerceAtMost(1.0)
    }

fun fTestSignificance(redBlueAverageRatio: Double, redSupport: Int, blueSupport: Int): Double {
  require(redBlueAverageRatio >= 0.0) { "tr/tb must be positive, is $redBlueAverageRatio" }
  require(redSupport > 1) { "nr must be > 1, is $redSupport" }
  require(blueSupport > 2) { "nb must be > 2, is $blueSupport" }

  val nr = redSupport.toDouble()
  val nb = blueSupport.toDouble()

  val muDifferenceSquared = (nb / (nb - 1) - redBlueAverageRatio).pow(2)
  val rhoSquared = (nb.pow(2) * (nr + nb - 1)) / (nr * (nb - 1).pow(2) * (nb - 2))
  return rhoSquared / muDifferenceSquared
}
