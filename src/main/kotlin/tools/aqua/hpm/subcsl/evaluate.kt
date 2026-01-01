// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.subcsl

import tools.aqua.rereso.log.Log
import tools.aqua.rereso.log.LogEntry

fun Log.isChronological(): Boolean =
    entries.asSequence().zipWithNext().all { (entry, next) ->
      entry.relativeStart!! <= next.relativeStart!!
    }

fun Log.requireChronological(): Log = apply {
  require(isChronological()) { "log is not chronological" }
}

infix fun Log.satisfiesSubCSL(subCSL: SubCSLFormula<String>): Boolean =
    when (subCSL) {
      is Until -> this satisfiesU subCSL
      is Finally -> this satisfiesF subCSL
      is Global -> this satisfiesG subCSL
      is PropositionalFormula -> entries.firstOrNull()?.satisfiesProp(subCSL) ?: false
    }

infix fun Log.satisfiesU(until: Until<String>): Boolean {
  entries.forEach { entry ->
    // we have not found a matching change point in the interval
    if (entry.relativeStart!! > until.interval.endInclusive) return false

    // we have found a matching change point
    if (entry.relativeStart!! in until.interval && entry satisfiesProp until.later) return true

    // "now" formula stopped being satisfied before change point
    if (!(entry satisfiesProp until.now)) return false
  }
  return false
}

infix fun Log.satisfiesF(finally: Finally<String>): Boolean {
  entries.forEach { entry ->
    // we have not found a matching entry in the interval
    if (entry.relativeStart!! > finally.interval.endInclusive) return false

    // we have found a matching entry
    if (entry.relativeStart!! in finally.interval && entry satisfiesProp finally.proposition)
        return true
  }
  return false
}

infix fun Log.satisfiesG(global: Global<String>): Boolean {
  entries.forEach { entry ->
    // we have not found a falsifying entry in the interval
    if (entry.relativeStart!! > global.interval.endInclusive) return true

    // we have found a falsifying entry
    if (entry.relativeStart!! in global.interval && !(entry satisfiesProp global.proposition))
        return false
  }
  return true
}

infix fun LogEntry.satisfiesProp(proposition: PropositionalFormula<String>): Boolean =
    when (proposition) {
      is True -> true
      is Output -> value == proposition.output
      is Or -> this satisfiesProp proposition.left || this satisfiesProp proposition.right
      is Not -> !(this satisfiesProp proposition.negated)
    }
