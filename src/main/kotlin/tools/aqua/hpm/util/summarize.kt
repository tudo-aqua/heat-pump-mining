// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.util

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.nio.file.Path
import kotlin.io.path.div
import tools.aqua.hpm.automata.DeterministicFrequencyProbabilisticTimedInputOutputAutomaton
import tools.aqua.hpm.automata.dfptioParser
import tools.aqua.hpm.data.EvaluationResult
import tools.aqua.hpm.data.EvaluationResultList
import tools.aqua.hpm.data.HittingPredictionList
import tools.aqua.hpm.data.RevisionScoreList
import tools.aqua.hpm.data.toHittingPredictionList
import tools.aqua.hpm.data.toRevisionScoreList

fun summarizeResults(
    baseDirectory: Path,
    cases: List<String>,
    rounds: Int,
    setups: List<String>,
    trainingSizes: List<Int>
): EvaluationResultList =
    cases
        .flatMap { case ->
          setups.flatMap { setup ->
            trainingSizes.map { trainingSize ->
              val (automata, input) =
                  List(rounds) { automatonAndInput(baseDirectory, case, it, setup, trainingSize) }
                      .let { aai -> aai.map { it.first } to aai.map { it.second }.toSet().single() }

              val revisionScores =
                  List(rounds) { revisionScores(baseDirectory, case, it, setup, trainingSize) }
              val hittingPredictions =
                  List(rounds) { hittingPredictions(baseDirectory, case, it, setup, trainingSize) }

              EvaluationResult.fromData(
                  case,
                  setup,
                  trainingSize,
                  automata,
                  setOf(input),
                  revisionScores,
                  hittingPredictions)
            }
          }
        }
        .let(::EvaluationResultList)

private fun automatonAndInput(
    baseDirectory: Path,
    case: String,
    round: Int,
    setup: String,
    learningSize: Int
): Pair<
    DeterministicFrequencyProbabilisticTimedInputOutputAutomaton<*, String, *, String>, String> =
    dfptioParser.readModel((baseDirectory / "$case-$round-$setup-$learningSize.dot").toFile()).let {
      it.model to it.alphabet.single()
    }

private fun revisionScores(
    baseDirectory: Path,
    case: String,
    round: Int,
    setup: String,
    learningSize: Int
): RevisionScoreList =
    csvReader().open((baseDirectory / "$case-$round-$setup-$learningSize-revision.csv").toFile()) {
      readAllAsSequence().toList().toRevisionScoreList()
    }

private fun hittingPredictions(
    baseDirectory: Path,
    case: String,
    round: Int,
    setup: String,
    learningSize: Int
): HittingPredictionList =
    csvReader().open((baseDirectory / "$case-$round-$setup-$learningSize-times.csv").toFile()) {
      readAllAsSequence().toList().toHittingPredictionList()
    }
