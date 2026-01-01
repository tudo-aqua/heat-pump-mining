// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.automata

import kotlin.time.Duration
import net.automatalib.common.util.Pair as APair
import net.automatalib.serialization.dot.DOTMutableAutomatonParser
import net.automatalib.serialization.dot.GraphDOT
import net.automatalib.visualization.VisualizationHelper.CommonAttrs.LABEL

val dfptioParser =
    DOTMutableAutomatonParser(
        { DefaultDeterministicFrequencyProbabilisticTimedInputOutputAutomaton() },
        { attr ->
          val (output, times) = attr.getValue(LABEL).split(" / ")
          val exitTimes =
              times.removePrefix("t=[").removeSuffix("]").split(", ").map { Duration.parse(it) }
          output.withExitTimes(exitTimes)
        },
        { attr ->
          val (input, freq, prob) = attr.getValue(LABEL).split(" / ")
          val frequency = freq.removePrefix("n=").toInt()
          val probability = prob.removePrefix("p=").toFloat()
          APair.of(input, frequency.withProbability(probability))
        },
        listOf(GraphDOT.initialLabel(0)),
        true,
    )
