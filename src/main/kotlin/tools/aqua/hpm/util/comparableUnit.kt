// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.util

data object ComparableUnit : Comparable<ComparableUnit> {
  override fun compareTo(other: ComparableUnit): Int = 0

  override fun toString(): String = "-"
}
