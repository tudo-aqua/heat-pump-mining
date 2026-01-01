// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.util

import java.math.BigInteger
import java.math.BigInteger.ONE

infix fun Int.ceilDiv(divisor: Int) = (this + divisor - 1) / divisor

infix fun Long.ceilDiv(divisor: Long) = (this + divisor - 1) / divisor

infix fun BigInteger.ceilDiv(divisor: BigInteger) = (this + divisor - ONE) / divisor
