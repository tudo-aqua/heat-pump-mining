// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.util

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.time.Duration

fun hoeffdingTest(
    frequency1: Int,
    totalFrequency1: Int,
    frequency2: Int,
    totalFrequency2: Int,
    epsilon: Double
): Boolean {
  require(epsilon in 0.0..1.0) { "epsilon must be in [0, 1]" }
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
      frequency1 == 0 && frequency2 == 0 -> 1.0
      frequency1 == 0 || frequency2 == 0 -> 0.0
      else -> 1.0 - hoeffdingSignificance(frequency1, totalFrequency1, frequency2, totalFrequency2)
    }

fun hoeffdingSignificance(
    frequency1: Int,
    totalFrequency1: Int,
    frequency2: Int,
    totalFrequency2: Int
): Double {
  require(0 < frequency1 && frequency1 <= totalFrequency1) { "0 < f1 <= n1 must hold" }
  require(0 < frequency2 && frequency2 <= totalFrequency2) { "0 < f2 <= n2 must hold" }

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
  require(epsilon >= 0) { "epsilon must be positive" }
  if (redSupport <= 1 || blueSupport <= 2) return true
  return fTestSignificance(redAverage, redSupport, blueAverage, blueSupport) >= epsilon
}

fun fTestSignificance(
    redAverage: Duration,
    redSupport: Int,
    blueAverage: Duration,
    blueSupport: Int
): Double = fTestSignificance(redAverage / blueAverage, redSupport, blueSupport)

fun fTestSimilarity(
    redAverage: Duration,
    redSupport: Int,
    blueAverage: Duration,
    blueSupport: Int
): Double =
    when {
      redSupport <= 1 || blueSupport <= 2 -> 1.0
      else ->
          1.0 -
              fTestSignificance(redAverage / blueAverage, redSupport, blueSupport).coerceAtMost(1.0)
    }

fun fTestSignificance(redBlueAverageRatio: Double, redSupport: Int, blueSupport: Int): Double {
  require(redBlueAverageRatio >= 0.0) { "tr/tb must be positive" }
  require(redSupport > 2) { "nr must be > 2" }
  require(blueSupport > 1) { "nb must be > 1" }

  val nr = redSupport.toDouble()
  val nb = blueSupport.toDouble()

  val muDifferenceSquared = (nb / (nb - 1) - redBlueAverageRatio).pow(2)
  val rhoSquared = (nb.pow(2) * (nr + nb - 1)) / (nr * (nb - 1).pow(2) * (nb - 2))
  return rhoSquared / muDifferenceSquared
}

// fun main() {
//    (10..100 step 10).forEach { f1 ->
//        (f1+1..100 step 10).forEach { n1 ->
//            (10..100 step 10).forEach { f2 ->
//                (f2 + 1..100 step 10).forEach { n2 ->
//                    val fs = fTestSignificance(f1.seconds, n1, f2.seconds, n2)
//                    var eps = 0.0
//                    do {
//                        eps += 0.01
//                        if (eps > 1.0) eps = 1.0
//                        print("f1=$f1, n1=$n1, f2=$f2, n2=$n2, eps=$eps")
//                        val ft = fTestLegacy(f1.seconds, n1, f2.seconds, n2, eps)
//                        val ft2 = fTest(f1.seconds, n1, f2.seconds, n2, eps)
//                        println(" -> fs=$fs, ft=$ft, ft2=$ft2")
//                        check(ft == ft2)
//                    } while (eps < 1.0)
//                }
//            }
//        }
//    }
//
//    (10..100 step 10).forEach { f1 ->
//        (f1+1..100 step 10).forEach { n1 ->
//            (10..100 step 10).forEach { f2 ->
//                (f2 + 1..100 step 10).forEach { n2 ->
//                    val hs = hoeffdingSignificance(f1, n1, f2, n2)
//                    var eps = 0.0
//                    do {
//                        eps += 0.01
//                        if (eps > 1.0) eps = 1.0
//                        print("f1=$f1, n1=$n1, f2=$f2, n2=$n2, eps=$eps")
//                        val ht = hoeffdingTestLegacy(f1, n1, f2, n2, eps*2.0)
//                        val ht2 = hoeffdingTest(f1, n1, f2, n2, eps)
//                        println(" -> hs=$hs, ht=$ht, ht2=$ht2")
//                        check(ht == ht2)
//                    } while (eps < 1.0)
//                }
//            }
//        }
//    }
// }
