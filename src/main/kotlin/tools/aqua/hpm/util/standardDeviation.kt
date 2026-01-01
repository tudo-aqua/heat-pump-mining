// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.util

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation

private val STANDARD_DEVIATION = StandardDeviation()

fun standardDeviation(values: DoubleArray): Double = STANDARD_DEVIATION.evaluate(values)

fun Iterable<Double>.standardDeviation(): Double = standardDeviation(doubleArrayOf())
