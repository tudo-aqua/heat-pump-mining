// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.subcsl

import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import tools.aqua.hpm.util.nextDuration
import tools.aqua.hpm.util.orderedRangeTo
import tools.aqua.hpm.util.runOneOf

class SubCSLFormulaGenerator<A>(
    private val alphabet: Collection<A>,
    private val leafProbability: Double,
    private val durationRange: ClosedRange<Duration>,
    private val random: Random,
) : Iterator<TrueSubCSLFormula<A>> {

  private val trueRange = durationRange.endInclusive - durationRange.start

  init {
    require(leafProbability > 0.0 && leafProbability <= 1.0) {
      "leaf probability must be in (0, 1]"
    }
    require(durationRange.start >= ZERO) { "duration range must not be in the past" }
  }

  override fun hasNext(): Boolean = true

  override fun next(): TrueSubCSLFormula<A> =
      random.runOneOf(
          { Until(generatePropositional(), generateDurationInterval(), generatePropositional()) },
          { Finally(generateDurationInterval(), generatePropositional()) },
          { Global(generateDurationInterval(), generatePropositional()) },
      )

  private fun generateDurationInterval(): ClosedRange<Duration> =
      random.nextDuration(durationRange) orderedRangeTo random.nextDuration(durationRange)

  private fun generatePropositional(): PropositionalFormula<A> =
      if (random.nextDouble() <= leafProbability) {
            Output(alphabet.random(random))
          } else {
            Or(generatePropositional(), generatePropositional())
          }
          .let { if (random.nextBoolean()) it else Not(it) }
}
