// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.data

import de.learnlib.query.DefaultQuery
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import net.automatalib.word.Word
import tools.aqua.rereso.log.Log
import tools.aqua.rereso.log.LogEntry

fun <I, O> TimedIOTrace<I, O>.toQuery(): DefaultQuery<I, Word<Pair<Duration, O>>> {
  return DefaultQuery<I, Word<Pair<Duration, O>>>(
      Word.fromList(tail.map { it.input }),
      Word.fromList(listOf(ZERO to head) + tail.map { it.time to it.output }))
}

fun <T> Log.toTimedIOTrace(inputSymbol: T): TimedIOTrace<T, String> =
    TimedIOTrace(
        entries.first().value,
        entries.zipWithNext().map { (past, current) ->
          TimedIO(current.relativeStart!! - past.relativeStart!!, inputSymbol, current.value)
        })

fun TimedIOTrace<*, String>.toLog(): Log =
    Log(
        entries =
            listOf(LogEntry(head, relativeStart = ZERO)) +
                buildList {
                  var runningTime = ZERO
                  tail.forEach { (time, _, output) ->
                    this += LogEntry(output, relativeStart = time + runningTime)
                    runningTime += time
                  }
                })
