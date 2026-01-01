// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.optionalValue
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import de.learnlib.datastructure.pta.config.DefaultProcessingOrders
import kotlin.io.path.createParentDirectories
import tools.aqua.hpm.data.rooted
import tools.aqua.hpm.data.toQuery
import tools.aqua.hpm.data.toTimedIOTrace
import tools.aqua.hpm.rtioalergia.RelaxedTimedIOAlergia
import tools.aqua.hpm.util.ComparableUnit
import tools.aqua.hpm.util.writeDot
import tools.aqua.rereso.log.LogArchive
import tools.aqua.rereso.util.smartDecode

class Learn : CliktCommand() {
  val input by
      option("-i", "--input")
          .path(mustExist = true, canBeDir = false, mustBeReadable = true)
          .required()
  val output by option("-o", "--output").path(canBeDir = false).required()

  val initOutput by option("-I", "--init").optionalValue($$"$init")

  val blueStateOrder by
      option("-b", "--blue-state-order")
          .choice(DefaultProcessingOrders.entries.associateBy { it.name }, ignoreCase = true)
          .default(DefaultProcessingOrders.LEX_ORDER)
  val parallel by option("-p", "--parallel").flag("--single-threaded")
  val deterministic by option("-d", "--deterministic").flag("--nondeterministic", default = true)
  val frequencySimilaritySignificance by
      option("-f", "--frequency-epsilon").double().default(0.05).check { it in 0.0..1.0 }
  val frequencySimilaritySignificanceDecay by
      option("-fd", "--frequency-epsilon-decay").double().default(1.0).check { it in 0.0..1.0 }
  val timingSimilaritySignificance by
      option("-t", "--timing-epsilon").double().default(0.05).check { it in 0.0..1.0 }
  val timingSimilaritySignificanceDecay by
      option("-td", "--timing-epsilon-decay").double().default(1.0).check { it in 0.0..1.0 }
  val tailLength by option("-T", "--tail-length").int()
  val analyzeMergedSamples by option("-m", "--consider-merged").flag("-u", "--consider-unmerged")

  override fun run() {
    val archive = input.smartDecode<LogArchive>()

    val learner =
        RelaxedTimedIOAlergia<ComparableUnit, String>(
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

    val init = initOutput
    val usableLogs =
        if (init == null) {
          val canonicalFirst = archive.logs.first().entries.first().value
          println("Canonical first event: $canonicalFirst")
          val matchingLogs = archive.logs.filter { it.entries.first().value == canonicalFirst }
          println("${matchingLogs.size} of ${archive.logs.size} match the canonical first event.")
          matchingLogs
        } else {
          archive.rooted(init).logs
        }
    learner.addSamples(usableLogs.map { it.toTimedIOTrace(ComparableUnit).toQuery() })
    val automaton = learner.computeModel()

    automaton.writeDot(output.createParentDirectories(), setOf(ComparableUnit))
  }
}
