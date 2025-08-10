// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.merge

import kotlinx.datetime.Instant
import kotlinx.datetime.Instant.Companion.DISTANT_FUTURE
import tools.aqua.hpm.data.hpmName
import tools.aqua.hpm.merge.SplitMode.EACH_FROM_START
import tools.aqua.hpm.merge.SplitMode.ONLY_FIRST
import tools.aqua.rereso.log.Log
import tools.aqua.rereso.log.LogArchive
import tools.aqua.rereso.util.mapToSet

enum class SplitMode {
  ONLY_FIRST,
  EACH_FROM_START,
  EACH_NON_OVERLAPPING
}

fun LogArchive.selectAndMerge(
    includeEvents: Set<String>,
    useSplitFrom: String,
    splitMode: SplitMode,
): LogArchive {
  require(useSplitFrom in includeEvents)

  val logsWithName = logs.map { it to it.hpmName }

  val events = logsWithName.mapToSet { (_, name) -> name.event }
  includeEvents.forEach { require(it in events) { "event $it missing from the log archive" } }

  val epochs =
      logsWithName
          .filter { (_, name) -> name.event == useSplitFrom }
          .map { (log, _) -> log.epoch!! } + DISTANT_FUTURE

  return if (splitMode == ONLY_FIRST) {
    val log = selectAndMerge(includeEvents, "all", epochs.first(), DISTANT_FUTURE)
    LogArchive("$name (only first)", setOf(log))
  } else {
    val logs =
        epochs.zipWithNext().withIndex().mapTo(mutableSetOf()) { (idx, range) ->
          val (epoch, end) = range
          selectAndMerge(
              includeEvents,
              idx.toString(),
              epoch,
              if (splitMode == EACH_FROM_START) DISTANT_FUTURE else end)
        }
    LogArchive("$name (merged)", logs)
  }
}

fun LogArchive.selectAndMerge(
    includeEvents: Set<String>,
    changePointSplit: String,
    epoch: Instant,
    end: Instant
): Log {

  val logsWithName = logs.map { it to it.hpmName }

  val includedLogs =
      logsWithName.filter { (_, name) -> name.event in includeEvents && name.changePointSplit == 0 }

  val merged =
      includedLogs
          .flatMap { (log, name) ->
            log.entries
                .filter { it.start(log.epoch!!)!! in epoch..<end }
                .map {
                  it.copy(
                      value = "${if (name.isContinuous) "c" else "d"}/${it.value}",
                      relativeStart = it.start(log.epoch!!)!! - epoch)
                }
          }
          .sortedBy { it.start(epoch)!! }

  val firstName = includedLogs.first().second
  val eventsMergedName =
      includedLogs
          .mapTo(mutableSetOf()) { (_, name) -> name.eventName }
          .toList()
          .sorted()
          .joinToString("+", "[", "]")
  val mergedName = "${firstName.source}/${firstName.processing}/$eventsMergedName/$changePointSplit"
  return Log(mergedName, merged, epoch = epoch)
}
