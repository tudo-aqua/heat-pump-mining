// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.rtioalergia

import de.learnlib.algorithm.PassiveLearningAlgorithm
import de.learnlib.datastructure.pta.config.DefaultProcessingOrders
import de.learnlib.datastructure.pta.config.DefaultProcessingOrders.LEX_ORDER
import de.learnlib.query.DefaultQuery
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import net.automatalib.word.Word
import tools.aqua.hpm.data.TimedIO
import tools.aqua.hpm.data.TimedIOTrace

class RelaxedTimedIOAlergia<Input : Comparable<Input>, Output : Comparable<Output>>(
    private val blueStateOrder: DefaultProcessingOrders = LEX_ORDER,
    private val parallel: Boolean = false,
    private val deterministic: Boolean = true,
    private val frequencySimilaritySignificance: Double = 0.05,
    private val frequencySimilaritySignificanceDecay: Double = 1.0,
    private val timingSimilaritySignificance: Double = 0.05,
    private val timingSimilaritySignificanceDecay: Double = 1.0,
    private val tailLength: Int? = null,
    private val analyzeMergedSamples: Boolean = false,
) :
    PassiveLearningAlgorithm<
        RTIOAlergiaMergedAutomaton<Input, Output>,
        Input,
        Word<Pair<Duration, Output>>,
    > {
  private var pta: TimedFrequencyPTA<Input, Output>? = null

  override fun addSamples(samples: Collection<DefaultQuery<Input, Word<Pair<Duration, Output>>>>) {
    if (samples.isEmpty()) return
    samples.forEach {
      require(it.input.length() + 1 == it.output.length()) {
        "sample $it does not have k inputs and k+1 outputs, but ${it.input.length()} and ${it.output.length()}"
      }
      require(it.output.first().first == ZERO) {
        "first event does not happen at zero, but at ${it.output.first().first}"
      }
    }
    if (pta == null) {
      pta = TimedFrequencyPTA(samples.first().output.first().second)
    }
    samples.forEach {
      val word =
          TimedIOTrace(
              it.output.first().second,
              (it.input zip it.output.drop(1)).map { (input, output) ->
                TimedIO(output.first, input, output.second)
              },
          )
      pta!!.addWord(word)
    }
  }

  override fun computeModel(): RTIOAlergiaMergedAutomaton<Input, Output> =
      RTIOAlergiaMergedAutomaton(
          checkNotNull(pta) { "no samples have been added" },
          blueStateOrder,
          parallel,
          deterministic,
          frequencySimilaritySignificance,
          frequencySimilaritySignificanceDecay,
          timingSimilaritySignificance,
          timingSimilaritySignificanceDecay,
          tailLength,
          analyzeMergedSamples,
      )
}
