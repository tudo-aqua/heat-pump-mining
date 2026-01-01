// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.util

fun String.endOfBracketedExpression(): Int {
  require(startsWith('('))

  var depth = 1
  substring(1).withIndex().forEach { (idx, c) ->
    when (c) {
      '(' -> depth++
      ')' -> depth--
    }
    if (depth == 0) {
      return idx
    }
  }

  error("unterminated expression at depth $depth")
}
