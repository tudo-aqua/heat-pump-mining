// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.subcsl

import kotlin.random.Random
import kotlin.time.Duration
import tools.aqua.hpm.util.nextDuration
import tools.aqua.hpm.util.nextNonEmptySubset
import tools.aqua.hpm.util.nextNonTrivialSubset
import tools.aqua.hpm.util.orderedRangeTo
import tools.aqua.hpm.util.runOneOf

class SubCSLFormulaGenerator<A>(
    alphabet: Collection<A>,
    private val variableSelectionProbability: Double,
    private val durationRange: ClosedRange<Duration>,
    private val random: Random,
) : Iterator<TrueSubCSLFormula<A>> {
  init {
    require(alphabet.size >= 2) {
      "alphabet must have at least 2 elements to create non-trivial formulas"
    }
  }

  private val alphabet = alphabet.toSet()

  override fun hasNext(): Boolean = true

  override fun next(): TrueSubCSLFormula<A> =
      random.runOneOf(
          ::generateUntil,
          ::generateFinally,
          ::generateGlobal,
      )

  private fun generateUntil(): Until<A> {
    val left = random.nextNonTrivialSubset(alphabet, variableSelectionProbability)
    val right = random.nextNonEmptySubset(alphabet - left, variableSelectionProbability)
    return Until(left.toPropositional(), generateDurationInterval(), right.toPropositional())
  }

  private fun generateFinally(): Finally<A> =
      Finally(
          generateDurationInterval(),
          random.nextNonTrivialSubset(alphabet, variableSelectionProbability).toPropositional(),
      )

  private fun generateGlobal(): Global<A> =
      Global(
          generateDurationInterval(),
          random.nextNonTrivialSubset(alphabet, variableSelectionProbability).toPropositional(),
      )

  private fun Set<A>.toPropositional(): PropositionalFormula<A> =
      fold<A, PropositionalFormula<A>>(True()) { acc, symbol ->
        if (acc is True) {
          Output(symbol)
        } else {
          Or(acc, Output(symbol))
        }
      }

  private fun generateDurationInterval(): ClosedRange<Duration> =
      random.nextDuration(durationRange) orderedRangeTo random.nextDuration(durationRange)
}
