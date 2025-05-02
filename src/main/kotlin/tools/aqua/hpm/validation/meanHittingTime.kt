// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.validation

import com.microsoft.z3.Context
import com.microsoft.z3.Expr
import com.microsoft.z3.Model
import com.microsoft.z3.RatNum
import com.microsoft.z3.RealSort
import com.microsoft.z3.Status.SATISFIABLE
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import tools.aqua.hpm.automata.DeterministicFrequencyProbabilisticTimedInputOutputAutomaton
import tools.aqua.hpm.util.toBigIntegerNanoseconds

fun <S, I, T, O> DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<S, I, T, O>
    .computeMeanHittingTimes(alphabet: Iterable<I>, hitTargets: Set<S>): Map<S, Duration> {
  require(hitTargets.isNotEmpty()) { "hit targets are empty" }

  computeReach(alphabet)
      .filter { (_, reach) -> reach.intersect(hitTargets).isEmpty() }
      .let { check(it.isEmpty()) { "automaton hat states that cannot reach a hit target: $it" } }

  return solveMeanHittingTimes(alphabet, hitTargets)
}

fun <S, I> DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<S, I, *, *>.computeReach(
    alphabet: Iterable<I>
): Map<S, Set<S>> =
    computeReachInternal(
        states.size - 2,
        states.associateWith { state ->
          setOf(state) + alphabet.flatMap { getSuccessors(state, it) }
        })

tailrec fun <S, I, T> DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<S, I, T, *>
    .computeReachInternal(iterations: Int, lastState: Map<S, Set<S>>): Map<S, Set<S>> =
    if (iterations == 0) {
      lastState
    } else {
      val newState =
          lastState.mapValues { (_, reached) ->
            reached + reached.flatMap { lastState.getValue(it) }
          }
      if (newState != lastState) computeReachInternal(iterations - 1, newState) else lastState
    }

fun <S, I, T, O> DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<S, I, T, O>
    .solveMeanHittingTimes(
    alphabet: Iterable<I>,
    hitTargets: Set<S>,
): Map<S, Duration> {
  Context().useApply {
    val stateTimes = states.withIndex().associate { (idx, state) -> state to mkRealConst("q_$idx") }
    mkSolver().apply {
      hitTargets.forEach { state -> add(mkEq(stateTimes.getValue(state), mkReal(0))) }
      (states - hitTargets).forEach { source ->
        add(
            mkEq(
                stateTimes.getValue(source),
                mkAdd(
                    *alphabet
                        .flatMap { getTransitions(source, it) }
                        .filter { getTransitionProbability(it) > 0 }
                        .map { transition ->
                          mkMul(
                              mkReal(getTransitionProbability(transition).toString()),
                              mkAdd(
                                  mkReal(getExitTime(source).toBigIntegerNanoseconds().toString()),
                                  stateTimes.getValue(getSuccessor(transition))))
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
  val value = ratNum.bigIntNumerator / ratNum.bigIntDenominator
  val (seconds, nanoseconds) = value.divideAndRemainder(1.seconds.inWholeNanoseconds.toBigInteger())
  return seconds.toDouble().seconds + nanoseconds.toDouble().nanoseconds
}
