// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.options.varargValues
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.defaultStdout
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.clikt.parameters.types.outputStream
import com.github.ajalt.clikt.parameters.types.path
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import de.learnlib.datastructure.pta.config.DefaultProcessingOrders
import de.learnlib.datastructure.pta.config.DefaultProcessingOrders.LEX_ORDER
import java.util.Optional
import java.util.stream.Collectors.toList
import kotlin.io.path.createParentDirectories
import kotlin.io.path.div
import kotlin.io.path.name
import kotlin.jvm.optionals.getOrNull
import kotlin.random.Random
import kotlin.time.DurationUnit.DAYS
import kotlin.time.DurationUnit.HOURS
import kotlin.time.DurationUnit.MICROSECONDS
import kotlin.time.DurationUnit.MILLISECONDS
import kotlin.time.DurationUnit.MINUTES
import kotlin.time.DurationUnit.NANOSECONDS
import kotlin.time.DurationUnit.SECONDS
import tools.aqua.hpm.automata.DFPTIOState
import tools.aqua.hpm.automata.DFPTIOTransition
import tools.aqua.hpm.automata.dfptioParser
import tools.aqua.hpm.data.HittingPredictionList
import tools.aqua.hpm.data.HittingPredictionResult
import tools.aqua.hpm.data.RevisionScoreList
import tools.aqua.hpm.data.RevisionScoreResult
import tools.aqua.hpm.data.toLog
import tools.aqua.hpm.data.toQuery
import tools.aqua.hpm.data.toRows
import tools.aqua.hpm.data.toTimedIOTrace
import tools.aqua.hpm.merge.SplitMode.EACH_FROM_START
import tools.aqua.hpm.merge.SplitMode.EACH_NON_OVERLAPPING
import tools.aqua.hpm.merge.SplitMode.ONLY_FIRST
import tools.aqua.hpm.merge.selectAndMerge
import tools.aqua.hpm.merge.validationSplit
import tools.aqua.hpm.rtioalergia.RelaxedTimedIOAlergia
import tools.aqua.hpm.timing.exponential
import tools.aqua.hpm.timing.normal
import tools.aqua.hpm.traces.generateTraces
import tools.aqua.hpm.util.ComparableUnit
import tools.aqua.hpm.util.summarizeResults
import tools.aqua.hpm.util.writeDot
import tools.aqua.hpm.validation.computeBestTraceSimilarity
import tools.aqua.hpm.validation.getViterbiPaths
import tools.aqua.hpm.validation.hittingTimeDelta
import tools.aqua.rereso.log.LogArchive
import tools.aqua.rereso.log.importer.importSplitNibeData
import tools.aqua.rereso.util.smartDecode
import tools.aqua.rereso.util.smartEncode

class CLI : CliktCommand("heat-pump-mining") {
  override fun run() = Unit
}

class ConvertFormat : CliktCommand() {
  val input by
      option("-i", "--input")
          .path(mustExist = true, canBeDir = false, mustBeReadable = true)
          .required()
  val output by option("-o", "--output").path(canBeDir = false).required()

  override fun run() {
    val archive = importSplitNibeData(input)
    output.createParentDirectories().smartEncode(archive)
  }
}

class SelectAndMerge : CliktCommand() {
  val input by
      option("-i", "--input")
          .path(mustExist = true, canBeDir = false, mustBeReadable = true)
          .required()
  val output by option("-o", "--output").path(canBeDir = false).required()
  val events by option("-e", "--events").varargValues().default(emptyList())
  val splitEvent by option("-s", "--split-event").required()
  val splitMode by
      option()
          .switch(
              "-f" to ONLY_FIRST,
              "--use-only-first" to ONLY_FIRST,
              "-a" to EACH_FROM_START,
              "--use-all-overlapping" to EACH_FROM_START,
              "-n" to EACH_NON_OVERLAPPING,
              "--use-all-non-overlapping" to EACH_NON_OVERLAPPING)
          .default(EACH_NON_OVERLAPPING)

  override fun run() {
    val archive = input.smartDecode<LogArchive>()
    val result = archive.selectAndMerge(events.toSet() + splitEvent, splitEvent, splitMode)
    output.createParentDirectories().smartEncode(result)
  }
}

class TVSplit : CliktCommand("tv-split") {
  val input by
      option("-i", "--input")
          .path(mustExist = true, canBeDir = false, mustBeReadable = true)
          .required()
  val output by
      option("-o", "--output-template").path(canBeDir = false).required().check {
        it.name.contains("{round}") && it.name.contains("{set}")
      }
  val rounds by option("-r", "--rounds").int().default(1)
  val validationShare by
      option("-v", "--validation-share").double().default(0.5).check { it > 0 && it < 1 }
  val seed by option("-s", "--seed").long().default(0)

  override fun run() {
    val archive = input.smartDecode<LogArchive>()
    val result = archive.validationSplit(rounds, validationShare, seed)
    result.withIndex().forEach { (round, tv) ->
      val roundName = output.name.replace("{round}", "$round")
      val (training, validation) = tv
      training.forEach {
        output
            .resolveSibling(roundName.replace("{set}", "training-${it.logs.size}"))
            .createParentDirectories()
            .smartEncode(it)
      }
      output
          .resolveSibling(roundName.replace("{set}", "validation"))
          .createParentDirectories()
          .smartEncode(validation)
    }
  }
}

class GenerateTraces : CliktCommand() {
  val dot by
      option("-a", "--automaton")
          .path(mustExist = true, canBeDir = false, mustBeReadable = true)
          .required()
  val output by option("-o", "--output").path(canBeDir = false).required()
  val nTraces by option("-n", "--traces").int().default(1).check { it >= 1 }
  val meanLength by option("-l", "--mean-length").double().default(100.0).check { it > 0 }
  val lengthSD by option("-L", "--length-stddev").double().default(0.0).check { it >= 0 }
  val distribution by
      option()
          .switch(
              "--normal-distribution" to normal,
              "--exponential-distribution" to exponential,
          )
          .required()
  val precision by
      option()
          .switch(
              "--nanoseconds" to NANOSECONDS,
              "--microseconds" to MICROSECONDS,
              "--milliseconds" to MILLISECONDS,
              "--seconds" to SECONDS,
              "--minutes" to MINUTES,
              "--hours" to HOURS,
              "--days" to DAYS,
          )
          .required()
  val seed by option("-s", "--seed").long().default(0)

  override fun run() {
    val data = dfptioParser.readModel(dot.toFile())
    val automaton = data.model
    val alphabet = data.alphabet

    val random = Random(seed)

    val result =
        LogArchive(
            "Traced from $dot",
            automaton
                .generateTraces(
                    nTraces,
                    meanLength,
                    lengthSD,
                    alphabet.single(),
                    distribution,
                    precision,
                    random)
                .withIndex()
                .mapTo(mutableSetOf()) { (idx, trace) -> trace.toLog().copy(name = "Trace $idx") })
    output.createParentDirectories().smartEncode(result)
  }
}

class Learn : CliktCommand() {
  val input by
      option("-i", "--input")
          .path(mustExist = true, canBeDir = false, mustBeReadable = true)
          .required()
  val output by option("-o", "--output").path(canBeDir = false).required()

  val blueStateOrder by
      option("-b", "--blue-state-order")
          .choice(DefaultProcessingOrders.entries.associateBy { it.name }, ignoreCase = true)
          .default(LEX_ORDER)
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
            analyzeMergedSamples)
    val canonicalFirst = archive.logs.first().entries.first().value
    println("Canonical first event: $canonicalFirst")
    val usableLogs = archive.logs.filter { it.entries.first().value == canonicalFirst }
    println("${usableLogs.size} of ${archive.logs.size} match the canonical first event.")
    learner.addSamples(usableLogs.map { it.toTimedIOTrace(ComparableUnit).toQuery() })
    val automaton = learner.computeModel()

    automaton.writeDot(output.createParentDirectories(), setOf(ComparableUnit))
  }
}

class ValidateRevisionScore : CliktCommand() {
  val dot by
      option("-a", "--automaton")
          .path(mustExist = true, canBeDir = false, mustBeReadable = true)
          .required()
  val input by
      option("-i", "--input")
          .path(mustExist = true, canBeDir = false, mustBeReadable = true)
          .required()
  val output by option("-o", "--output").outputStream(truncateExisting = true).defaultStdout()

  val parallel by option("-p", "--parallel").flag("--single-threaded")
  val frequencyWeight by
      option("-f", "--frequency-weight").double().default(0.5).check { it in 0.0..1.0 }
  val normalizeLikelihood by option("-n", "--normalize-likelihood").flag()

  override fun run() {
    val data = dfptioParser.readModel(dot.toFile())
    val automaton = data.model
    val alphabet = data.alphabet

    val archive = input.smartDecode<LogArchive>()

    val inputSymbol = alphabet.single()

    val result =
        archive.logs
            .let { if (parallel) it.parallelStream() else it.stream() }
            .map { log ->
              val trace = log.toTimedIOTrace(inputSymbol)
              val revisionScore =
                  automaton.computeBestTraceSimilarity(alphabet, trace, frequencyWeight)
              val pathLikeliness =
                  automaton
                      .getViterbiPaths<DFPTIOState, String, DFPTIOTransition, String>(
                          trace, normalizeLikelihood)
                      .maxOfOrNull { (_, score) -> score } ?: 0.0
              RevisionScoreResult("${log.name}", revisionScore, pathLikeliness)
            }
            .collect(toList())
            .let(::RevisionScoreList)

    csvWriter { lineTerminator = "\n" }.open(output) { writeRows(result.toRows()) }
  }
}

class ValidateHittingTimes : CliktCommand() {
  val dot by
      option("-a", "--automaton")
          .path(mustExist = true, canBeDir = false, mustBeReadable = true)
          .required()
  val input by
      option("-i", "--input")
          .path(mustExist = true, canBeDir = false, mustBeReadable = true)
          .required()
  val output by option("-o", "--output").outputStream(truncateExisting = true).defaultStdout()

  val parallel by option("-p", "--parallel").flag("--single-threaded")
  val events by option("-e", "--events").varargValues().required()
  val samples by option("-s", "--samples").int().check { it > 0 }

  override fun run() {
    val data = dfptioParser.readModel(dot.toFile())
    val automaton = data.model
    val alphabet = data.alphabet

    val archive = input.smartDecode<LogArchive>()

    val inputSymbol = alphabet.single()

    val traces = archive.logs.map { it.toTimedIOTrace(inputSymbol) }
    val deltas = automaton.hittingTimeDelta(traces, samples, inputSymbol, events.toSet(), parallel)

    val result =
        if (deltas == null) {
              archive.logs.map { HittingPredictionResult("${it.name}", false, emptyList()) }
            } else {
              (archive.logs zip deltas.values).map { (log, hittingInformation) ->
                HittingPredictionResult(
                    "${log.name}", true, hittingInformation.map { it?.hittingTime })
              }
            }
            .let(::HittingPredictionList)

    csvWriter { lineTerminator = "\n" }.open(output) { writeRows(result.toRows()) }
  }
}

class SummarizeResults : CliktCommand() {
  val basedir by option("-d", "--directory").path(mustExist = true, canBeFile = false).required()
  val cases by option("-c", "--cases").varargValues().required()
  val rounds by option("-r", "--rounds").int().required()
  val setups by option("-s", "--setups").varargValues().required()
  val learningSizes by option("-l", "--learning-sizes").int().varargValues().required()

  val timeUnit by
      option()
          .switch(
              "--iso-duration" to Optional.empty(),
              "--nanoseconds" to Optional.of(NANOSECONDS),
              "--microseconds" to Optional.of(MICROSECONDS),
              "--milliseconds" to Optional.of(MILLISECONDS),
              "--seconds" to Optional.of(SECONDS),
              "--minutes" to Optional.of(MINUTES),
              "--hours" to Optional.of(HOURS),
              "--days" to Optional.of(DAYS),
          )
          .default(Optional.empty())

  val output by option("-o", "--output").outputStream(truncateExisting = true).defaultStdout()

  override fun run() {
    csvWriter { lineTerminator = "\n" }
        .open(output) {
          writeRows(
              summarizeResults(basedir, cases, rounds, setups, learningSizes)
                  .toRows(timeUnit.getOrNull()))
        }
  }
}

fun main(args: Array<String>) =
    CLI()
        .subcommands(
            ConvertFormat(),
            SelectAndMerge(),
            TVSplit(),
            GenerateTraces(),
            Learn(),
            ValidateRevisionScore(),
            ValidateHittingTimes(),
            SummarizeResults())
        .main(args)
