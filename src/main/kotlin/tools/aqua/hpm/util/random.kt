// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.util

import kotlin.math.ln
import kotlin.random.Random

fun <T> Random.selectWeighted(values: Map<T, Number>): T =
    values.entries
        .map { (entry, weight) -> entry to -ln(nextDouble()) / weight.toDouble() }
        .minWith(compareBy { (_, value) -> value })
        .first
