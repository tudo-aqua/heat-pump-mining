// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.merge

import kotlinx.datetime.Instant
import tools.aqua.hpm.data.hpmName
import tools.aqua.rereso.log.Log
import tools.aqua.rereso.log.LogArchive
import tools.aqua.rereso.util.mapToSet

fun LogArchive.selectAndMerge(
    includeEvents: Set<String>,
    useSplitFrom: String,
    useOnlyFirst: Boolean
): LogArchive {
  require(useSplitFrom in includeEvents)

  val logsWithName = logs.map { it to it.hpmName }

  val events = logsWithName.mapToSet { (_, name) -> name.event }
  includeEvents.forEach { require(it in events) { "event $it missing from the log archive" } }

  val initial =
      selectAndMerge(
          includeEvents,
          0,
          logsWithName
              .single { (_, name) -> name.event == useSplitFrom && name.changePointSplit == 0 }
              .first
              .epoch!!)

  val others =
      if (!useOnlyFirst) {
        logsWithName
            .filter { (_, name) -> name.event == useSplitFrom && name.changePointSplit > 0 }
            .map { (log, name) ->
              selectAndMerge(includeEvents, name.changePointSplit, log.epoch!!)
            }
      } else emptyList()

  return LogArchive("$name (merged)", setOf(initial) + others)
}

fun LogArchive.selectAndMerge(
    includeEvents: Set<String>,
    changePointSplit: Int,
    epoch: Instant,
): Log {

  val logsWithName = logs.map { it to it.hpmName }

  val includedLogs =
      logsWithName.filter { (_, name) -> name.event in includeEvents && name.changePointSplit == 0 }

  val merged =
      includedLogs
          .flatMap { (log, name) ->
            log.entries
                .filter { it.start(log.epoch!!)!! >= epoch }
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
          .map { (_, name) -> name.eventName }
          .toSet()
          .toList()
          .sorted()
          .joinToString("+", "[", "]")
  val mergedName = "${firstName.source}/${firstName.processing}/$eventsMergedName/$changePointSplit"
  return Log(mergedName, merged)
}
