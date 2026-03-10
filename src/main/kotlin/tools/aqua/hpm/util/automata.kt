// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.util

import java.nio.file.Path
import net.automatalib.automaton.Automaton
import net.automatalib.automaton.UniversalAutomaton
import net.automatalib.automaton.graph.TransitionEdge
import net.automatalib.serialization.dot.DOTSerializationProvider
import tools.aqua.hpm.automata.FrequencyProbabilisticTimedInputOutputAutomaton

fun <S, I> Automaton<S, I, *>.countTransitions(inputs: Set<I>): Int =
    states.sumOf { state -> inputs.sumOf { getTransitions(state, it).size } }

fun <S, I, T> UniversalAutomaton<S, I, T, *, *>.writeDot(path: Path, alphabet: Collection<I>) {
  DOTSerializationProvider.getInstance<S, TransitionEdge<I, T>>()
      .writeModel(path.toFile(), transitionGraphView(alphabet))
}

data class RenderConfig(
    val simplify: Boolean,
    val transitionFullColorMin: Double,
    val transitionRenderMin: Double,
    val probabilityDecimals: Int,
    val timeSubcomponents: Int,
) {
  init {
    require(transitionFullColorMin in 0.0..1.0)
    require(transitionRenderMin in 0.0..1.0)
    require(probabilityDecimals >= 0)
    require(timeSubcomponents >= 0)
  }

  companion object {
    val Full = RenderConfig(false, 0.0, 0.0, Int.MAX_VALUE, Int.MAX_VALUE)
  }
}

fun <S, I, T> FrequencyProbabilisticTimedInputOutputAutomaton<S, I, T, *>.writeRenderDot(
    path: Path,
    alphabet: Collection<I>,
    renderConfig: RenderConfig,
) {
  DOTSerializationProvider.getInstance<S, TransitionEdge<I, T>>()
      .writeModel(
          path.toFile(),
          renderTransitionGraphView(alphabet, renderConfig),
      )
}
