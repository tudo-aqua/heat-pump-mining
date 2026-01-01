// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.data

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.InputStream
import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Instant
import tools.aqua.rereso.log.Log
import tools.aqua.rereso.log.LogArchive
import tools.aqua.rereso.log.LogEntry
import tools.aqua.rereso.util.asZip
import tools.aqua.rereso.util.mapToSet

/**
 * Import a Nibe data set in CSV format into the ReReSo Log format. Supports fractional timestamps.
 *
 * @param archive the downloaded archive file to read. This is a ZIP archive containing a CSV file
 *   per event type and split series.
 * @return the ReReSo [LogArchive] containing the data.
 */
fun importSplitNibe2Data(archive: Path): LogArchive =
    archive.asZip().use { zip ->
      LogArchive(
          "Nibe Data",
          zip.entries
              .toList()
              .filter { it.name.endsWith(".csv") }
              .mapToSet { readTrace(zip.getInputStream(it), it.name.removeSuffix(".csv")) },
      )
    }

private fun readTrace(input: InputStream, name: String): Log =
    input.use {
      val lines =
          csvReader().readAllWithHeader(it).map { line ->
            val timestamp = line.getValue("timestamp").asUnixTimestamp()
            val event = line.getValue("event")
            timestamp to event
          }
      val firstTimestamp = lines.minOf { (timestamp, _) -> timestamp }
      Log(
          name,
          lines.map { (timestamp, event) ->
            LogEntry(event, relativeStart = timestamp - firstTimestamp)
          },
          epoch = firstTimestamp,
      )
    }

private fun String.asUnixTimestamp(): Instant =
    toLongOrNull()?.let(Instant::fromEpochSeconds)
        ?: toDoubleOrNull()?.let {
          val seconds = it.toLong()
          val subSeconds = it.rem(1.0)
          Instant.fromEpochSeconds(seconds, (subSeconds * 1.seconds.inWholeNanoseconds).toLong())
        }
        ?: throw IllegalArgumentException("unrecognized timestamp '$this'")
