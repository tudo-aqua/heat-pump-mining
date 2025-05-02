// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.data

import tools.aqua.rereso.log.Log

data class LogName(
    val source: String,
    val processing: String,
    val isDiscrete: Boolean,
    val event: String,
    val changePointSplit: Int
) {
  val isContinuous: Boolean
    get() = !isDiscrete

  val eventNameShort: String = "${if (isDiscrete) "d" else "c"}/$event"
  val eventName: String = "${if (isDiscrete) "discrete" else "continuous"}/$event"

  override fun toString(): String {
    val discrete = if (isDiscrete) "discrete" else "continuous"
    val count =
        if (changePointSplit in 10..19) "th"
        else {
          when (changePointSplit.mod(10)) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
          }
        }
    val number = "$changePointSplit${count}_event_trace"
    return "$source/$processing/$discrete/$event/$number"
  }
}

private val numberRegex = """(\d+)(?:st|nd|rd|th)_event_trace""".toRegex()

val Log.hpmName: LogName
  get() {
    val components = checkNotNull(name) { "log $this does not define a name" }.split('/')
    require(components.size == 5) {
      "format violation of $name, five slash-divided components expected"
    }
    val (source, processing, discreteness, event, numberRich) = components
    require(discreteness == "discrete" || discreteness == "continuous")
    val number =
        requireNotNull(numberRegex.matchEntire(numberRich)) { "malformed number $numberRich" }
            .groups[1]!!
            .value
            .toInt()
    return LogName(source, processing, discreteness == "discrete", event, number)
  }
