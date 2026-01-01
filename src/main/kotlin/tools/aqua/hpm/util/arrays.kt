// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.util

import java.math.BigInteger

operator fun <T> Array<T>.get(index: BigInteger): T = this[index.intValueExact()]

operator fun <T> Array<T>.set(index: BigInteger, value: T) {
  this[index.intValueExact()] = value
}
