// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.util

inline fun <T> runUntil(generator: () -> T, check: (T) -> Boolean): T {
  var result: T
  do {
    result = generator()
  } while (!check(result))
  return result
}
