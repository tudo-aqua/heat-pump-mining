// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.data

import de.learnlib.query.DefaultQuery
import kotlin.time.Duration
import net.automatalib.word.Word
import tools.aqua.rereso.log.Log

fun <T> Log.toQuery(inputSymbol: T): DefaultQuery<T, Word<Pair<Duration, String>>> =
    DefaultQuery<T, Word<Pair<Duration, String>>>(
        Word.fromList(List(entries.size - 1) { inputSymbol }),
        Word.fromList(entries.map { it.relativeStart!! to it.value }))

fun <T> Log.toTimedIOTrace(inputSymbol: T): TimedIOTrace<T, String> =
    TimedIOTrace(
        entries.first().value,
        entries.drop(1).map { TimedIO(it.relativeStart!!, inputSymbol, it.value) })
