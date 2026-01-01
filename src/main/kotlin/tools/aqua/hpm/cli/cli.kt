// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands

class CLI : CliktCommand("heat-pump-mining") {
  override fun run() = Unit
}

fun main(args: Array<String>) =
    CLI()
        .subcommands(
            ComputeHittingTimes(),
            ComputeRevisionScore(),
            ConvertFormat(),
            EvaluateSubCSL(),
            GenerateAutomata(),
            GenerateSubCSL(),
            GenerateTraces(),
            Learn(),
            RenderAutomaton(),
            SelectAndMerge(),
            // SummarizeResults(), disable for new rev score
            TrainingValidationSplit(),
        )
        .main(args)
