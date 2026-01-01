// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.subcsl

import java.util.Objects.hash
import kotlin.time.Duration
import org.checkerframework.checker.units.qual.A

sealed interface SubCSLFormula<A>

sealed interface TrueSubCSLFormula<A> : SubCSLFormula<A>

data class Until<A>(
    val now: PropositionalFormula<A>,
    val interval: ClosedRange<Duration>,
    val later: PropositionalFormula<A>,
) : TrueSubCSLFormula<A> {
  override fun toString(): String = "($now) U[$interval] ($later)"
}

data class Finally<A>(
    val interval: ClosedRange<Duration>,
    val proposition: PropositionalFormula<A>,
) : TrueSubCSLFormula<A> {
  override fun toString(): String = "◇[$interval] ($proposition)"
}

data class Global<A>(
    val interval: ClosedRange<Duration>,
    val proposition: PropositionalFormula<A>,
) : TrueSubCSLFormula<A> {
  override fun toString(): String = "□[$interval] ($proposition)"
}

sealed interface PropositionalFormula<A> : SubCSLFormula<A>

class True<A> : PropositionalFormula<A> {
  override fun toString() = "true"

  override fun equals(other: Any?): Boolean = other === this || other is True<*>

  override fun hashCode(): Int = hash()
}

data class Output<A>(val output: A) : PropositionalFormula<A> {
  override fun toString(): String = output.toString()
}

data class Or<A>(val left: PropositionalFormula<A>, val right: PropositionalFormula<A>) :
    PropositionalFormula<A> {
  override fun toString(): String = "($left) ∧ ($right)"
}

data class Not<A>(val negated: PropositionalFormula<A>) : PropositionalFormula<A> {
  override fun toString(): String = "¬($negated)"
}
