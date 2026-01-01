// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.data

import kotlin.time.Duration.Companion.ZERO
import tools.aqua.rereso.log.LogArchive
import tools.aqua.rereso.log.LogEntry
import tools.aqua.rereso.util.mapToSet

fun LogArchive.rooted(root: String = $$"$init"): LogArchive =
    copy(
        logs =
            logs.mapToSet { log ->
              log.copy(
                  entries =
                      listOf(LogEntry(root, relativeStart = ZERO, duration = ZERO)) + log.entries
              )
            }
    )
