// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.merge

import kotlin.collections.listOf
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.Instant.Companion.DISTANT_FUTURE
import kotlinx.datetime.Instant.Companion.DISTANT_PAST
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone.Companion.UTC
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import tools.aqua.hpm.data.hpmName
import tools.aqua.rereso.log.Log
import tools.aqua.rereso.log.LogArchive
import tools.aqua.rereso.util.mapToSet

private fun Iterable<Log>.selectEvents(
    includeEvents: Set<String>,
    changePoint: Int = 0,
): List<Log> {
  val matching = filter {
    it.hpmName.event in includeEvents && it.hpmName.changePointSplit == changePoint
  }
  val unmatched = includeEvents - matching.mapToSet { it.hpmName.event }
  require(unmatched.isEmpty()) { "included events do not exist: $unmatched" }
  return matching
}

private fun Log.tagEventsWithType(): Log {
  val tag = if (hpmName.isContinuous) "c" else "d"
  return copy(entries = entries.map { it.copy(value = "$tag/${it.value}") })
}

private fun Iterable<Log>.merge(): Log {
  val name = joinToString("+") { it.name!! }
  val firstEpoch = minOf { it.epoch!! }
  val entries =
      flatMap { log ->
            val offset = log.epoch!! - firstEpoch
            log.entries.map { it.copy(relativeStart = it.relativeStart!! + offset) }
          }
          .sortedBy { it.relativeStart }
  return Log(name, entries, epoch = firstEpoch)
}

private fun Log.splitIntervals(intervals: Iterable<OpenEndRange<Instant>>): List<Log> =
    intervals.mapIndexed { idx, it -> selectInterval(it, "$idx") }

private fun Log.selectInterval(interval: OpenEndRange<Instant>, tag: String = ""): Log {
  val first = entries.binarySearchBy(interval.start) { it.start(epoch!!) }
  val afterLast = entries.binarySearchBy(interval.endExclusive) { it.start(epoch!!) }
  return copy(name = "$name/$tag", entries = entries.subList(first, afterLast))
}

fun LogArchive.selectAndMergeSingle(includeEvents: Set<String>): LogArchive =
    LogArchive(
        "$name [single]",
        setOf(logs.selectEvents(includeEvents).map { it.tagEventsWithType() }.merge()),
    )

fun LogArchive.selectAndMergeFromKey(
    includeEvents: Set<String>,
    keyEvent: String,
    includePrefix: Boolean,
): LogArchive {
  val keyEpochs = logs.filter { it.hpmName.event == keyEvent }.map { it.epoch!! }
  val prefix = if (includePrefix) listOf(DISTANT_PAST..<keyEpochs.first()) else emptyList()
  val inner = keyEpochs.zipWithNext().map { (start, next) -> start..<next }
  val suffix = listOf(keyEpochs.last()..<DISTANT_FUTURE)
  return LogArchive(
      "$name [from key $keyEvent ${if (includePrefix) "with" else "without"} prefix]",
      logs
          .selectEvents(includeEvents)
          .map { it.tagEventsWithType() }
          .merge()
          .splitIntervals(prefix + inner + suffix)
          .toSet(),
  )
}

fun LogArchive.selectAndMergeWithStartOfDay(
    includeEvents: Set<String>,
    startOfDay: LocalTime,
): LogArchive {
  val merged = logs.selectEvents(includeEvents).map { it.tagEventsWithType() }.merge()

  val firstDay = merged.entries.first().start(merged.epoch!!)!!.toLocalDateTime(UTC).date
  val lastDay = merged.entries.last().start(merged.epoch!!)!!.toLocalDateTime(UTC).date

  val intervals = buildList {
    // everything until the SOD on the first day
    this += DISTANT_PAST..<firstDay.atTime(startOfDay).toInstant(UTC)
    var current = firstDay
    while (current < lastDay) {
      val next = current + DatePeriod(days = 1)
      // SOD-to-SOD interval
      this += current.atTime(startOfDay).toInstant(UTC)..<next.atTime(startOfDay).toInstant(UTC)
      current = next
    }
    // everything after the SOD on the last day
    this += lastDay.atTime(startOfDay).toInstant(UTC)..<DISTANT_FUTURE
  }

  return LogArchive(
      "$name [with start of day $startOfDay]",
      merged.splitIntervals(intervals).filter { it.entries.isNotEmpty() }.toSet(),
  )
}

fun LogArchive.selectAndMergeWithTimeInterval(
    includeEvents: Set<String>,
    splitInterval: Duration,
): LogArchive {
  require(splitInterval.isFinite() && splitInterval.isPositive() && splitInterval <= 1.days)

  val merged = logs.selectEvents(includeEvents).map { it.tagEventsWithType() }.merge()

  val firstDayMidnight =
      merged.entries.first().start(merged.epoch!!)!!.toLocalDateTime(UTC).date.atStartOfDayIn(UTC)
  val afterLastDayMidnight =
      (merged.entries.last().start(merged.epoch!!)!!.toLocalDateTime(UTC).date +
              DatePeriod(days = 1))
          .atStartOfDayIn(UTC)

  val intervals = buildList {
    var current = firstDayMidnight
    while (current < afterLastDayMidnight) {
      val next = current + splitInterval
      this += current..<next
      current = next
    }
  }

  return LogArchive(
      "$name [with time interval $splitInterval]",
      merged.splitIntervals(intervals).filter { it.entries.isNotEmpty() }.toSet(),
  )
}
