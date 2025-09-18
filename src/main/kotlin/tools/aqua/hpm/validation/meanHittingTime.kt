// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.validation

import com.microsoft.z3.Context
import com.microsoft.z3.Expr
import com.microsoft.z3.Global.setParameter
import com.microsoft.z3.Model
import com.microsoft.z3.RatNum
import com.microsoft.z3.RealSort
import com.microsoft.z3.Status.SATISFIABLE
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.time.Duration
import tools.aqua.hpm.automata.DeterministicFrequencyProbabilisticTimedInputOutputAutomaton
import tools.aqua.hpm.util.average
import tools.aqua.hpm.util.nanoseconds
import tools.aqua.hpm.util.toBigIntegerNanoseconds

class UnconnectedAutomatonException(val unconnectedStates: Collection<*>) :
    RuntimeException("Automaton hat states that cannot reach a hit target: $unconnectedStates")

fun <S, I> DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<S, I, *, *>
    .computeMeanHittingTimes(
    input: I,
    hitTargets: Set<S>,
    parallel: Boolean = false
): Map<S, Duration> {
  require(hitTargets.isNotEmpty()) { "hit targets are empty" }

  (states - backwardsReach(hitTargets, setOf(input))).let {
    if (it.isNotEmpty()) throw UnconnectedAutomatonException(it)
  }

  return solveMeanHittingTimes(input, hitTargets, parallel)
}

tailrec fun <S, I> DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<S, I, *, *>
    .backwardsReach(targets: Set<S>, alphabet: Iterable<I>): Set<S> {
  val reaching =
      states.filterTo(mutableSetOf()) { state ->
        alphabet.flatMap { getSuccessors(state, it) }.any { it in targets }
      }
  val newTargets = targets + reaching
  return if (newTargets.size > targets.size) backwardsReach(newTargets, alphabet) else targets
}

fun <S, I, T> DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<S, I, T, *>
    .solveMeanHittingTimes(
    input: I,
    hitTargets: Set<S>,
    parallel: Boolean = false,
): Map<S, Duration> {
  Context().useApply {
    if (parallel) setParameter("parallel.enable", "true")
    val stateTimes = states.withIndex().associate { (idx, state) -> state to mkRealConst("q_$idx") }
    mkSolver().apply {
      hitTargets.forEach { state -> add(mkEq(stateTimes.getValue(state), mkReal(0))) }
      (states - hitTargets).forEach { source ->
        add(
            mkEq(
                stateTimes.getValue(source),
                mkAdd(
                    mkReal(getExitTimes(source).average().toBigIntegerNanoseconds().toString()),
                    *getTransitions(source, input)
                        .filter { getTransitionProbability(it) > 0 }
                        .map { transition ->
                          mkMul(
                              mkReal(getTransitionProbability(transition).toString()),
                              stateTimes.getValue(getSuccessor(transition)))
                        }
                        .toTypedArray<Expr<RealSort>>())))
      }
      check(check() == SATISFIABLE) { "solver failed to find a valuation" }
      val model = model
      return states.associateWith { model.getDuration(stateTimes.getValue(it)) }
    }
  }
}

@OptIn(ExperimentalContracts::class)
inline fun <T : AutoCloseable, R> T.useApply(block: T.() -> R): R {
  contract { callsInPlace(block, EXACTLY_ONCE) }
  return use { it.block() }
}

fun Model.getDuration(expr: Expr<RealSort>): Duration {
  val ratNum = getConstInterp(expr) as RatNum
  return (ratNum.bigIntNumerator / ratNum.bigIntDenominator).nanoseconds
}
