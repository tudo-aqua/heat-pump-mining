// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.util

import java.nio.file.Path
import net.automatalib.automaton.UniversalAutomaton
import net.automatalib.automaton.graph.TransitionEdge
import net.automatalib.serialization.dot.DOTSerializationProvider

fun <S, I, T> UniversalAutomaton<S, I, T, *, *>.writeDot(path: Path, alphabet: Collection<I>) {
  DOTSerializationProvider.getInstance<S, TransitionEdge<I, T>>()
      .writeModel(path.toFile(), transitionGraphView(alphabet))
}
