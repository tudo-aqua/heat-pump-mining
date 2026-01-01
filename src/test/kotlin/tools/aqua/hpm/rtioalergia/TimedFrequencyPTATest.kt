// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.rtioalergia

import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.Test
import tools.aqua.hpm.data.TimedIO
import tools.aqua.hpm.data.TimedIOTrace
import tools.aqua.hpm.util.State
import tools.aqua.hpm.util.Transition
import tools.aqua.hpm.util.assertAutomatonStructure
import tools.aqua.hpm.util.zip

class TimedFrequencyPTATest {

  @Test
  fun `single input-only trace generates correct PTA`() {
    val trace =
        TimedIOTrace(
            Unit,
            listOf("a", "b", "c").zip(listOf(1.seconds, 2.seconds, 3.seconds)).zip(List(3) {}) {
                input,
                time,
                output ->
              TimedIO(time, input, output)
            },
        )

    val pta = TimedFrequencyPTA<String, Unit>(Unit).apply { addWord(trace) }

    pta.assertAutomatonStructure(
        State(
            Unit,
            1.seconds,
            listOf(1.seconds),
            Transition(
                "a",
                1,
                1.0f,
                State(
                    Unit,
                    2.seconds,
                    listOf(2.seconds),
                    Transition(
                        "b",
                        1,
                        1.0f,
                        State(
                            Unit,
                            3.seconds,
                            listOf(3.seconds),
                            Transition("c", 1, 1.0f, State(Unit, null, emptyList())),
                        ),
                    ),
                ),
            ),
        )
    )
  }

  @Test
  fun `single output-only trace generates correct PTA`() {
    val trace =
        TimedIOTrace(
            "z",
            List(3) {}
                .zip(listOf(1.seconds, 2.seconds, 3.seconds))
                .zip(listOf("a", "b", "c")) { input, time, output -> TimedIO(time, input, output) },
        )

    val pta = TimedFrequencyPTA<Unit, String>("z").apply { addWord(trace) }

    pta.assertAutomatonStructure(
        State(
            "z",
            1.seconds,
            listOf(1.seconds),
            Transition(
                Unit,
                1,
                1.0f,
                State(
                    "a",
                    2.seconds,
                    listOf(2.seconds),
                    Transition(
                        Unit,
                        1,
                        1.0f,
                        State(
                            "b",
                            3.seconds,
                            listOf(3.seconds),
                            Transition(Unit, 1, 1.0f, State("c", null, emptyList())),
                        ),
                    ),
                ),
            ),
        )
    )
  }

  @Test
  fun `single IO trace generates correct PTA`() {
    val trace =
        TimedIOTrace(
            "z",
            listOf("a", "b", "c").zip(listOf(1.seconds, 2.seconds, 3.seconds)).zip(
                listOf("i", "j", "k")
            ) { input, time, output ->
              TimedIO(time, input, output)
            },
        )

    val pta = TimedFrequencyPTA<String, String>("z").apply { addWord(trace) }

    pta.assertAutomatonStructure(
        State(
            "z",
            1.seconds,
            listOf(1.seconds),
            Transition(
                "a",
                1,
                1.0f,
                State(
                    "i",
                    2.seconds,
                    listOf(2.seconds),
                    Transition(
                        "b",
                        1,
                        1.0f,
                        State(
                            "j",
                            3.seconds,
                            listOf(3.seconds),
                            Transition("c", 1, 1.0f, State("k", null, emptyList())),
                        ),
                    ),
                ),
            ),
        )
    )
  }

  @Test
  fun `double input-only trace generates correct PTA`() {
    val trace1 =
        TimedIOTrace(
            Unit,
            listOf("a", "b", "c").zip(listOf(1.seconds, 2.seconds, 3.seconds)).zip(List(3) {}) {
                input,
                time,
                output ->
              TimedIO(time, input, output)
            },
        )
    val trace2 =
        TimedIOTrace(
            Unit,
            listOf("a", "b", "d").zip(listOf(1.seconds, 4.seconds, 3.seconds)).zip(List(3) {}) {
                input,
                time,
                output ->
              TimedIO(time, input, output)
            },
        )

    val pta =
        TimedFrequencyPTA<String, Unit>(Unit).apply {
          addWord(trace1)
          addWord(trace2)
        }

    pta.assertAutomatonStructure(
        State(
            Unit,
            1.seconds,
            listOf(1.seconds, 1.seconds),
            Transition(
                "a",
                2,
                1.0f,
                State(
                    Unit,
                    3.seconds,
                    listOf(2.seconds, 4.seconds),
                    Transition(
                        "b",
                        2,
                        1.0f,
                        State(
                            Unit,
                            3.seconds,
                            listOf(3.seconds, 3.seconds),
                            Transition("c", 1, 1.0f, State(Unit, null, emptyList())),
                            Transition(
                                "d",
                                1,
                                1.0f,
                                State(Unit, null, emptyList()),
                            ),
                        ),
                    ),
                ),
            ),
        )
    )
  }

  @Test
  fun `double output-only trace generates correct PTA`() {
    val trace1 =
        TimedIOTrace(
            "z",
            List(3) {}
                .zip(listOf(1.seconds, 2.seconds, 3.seconds))
                .zip(listOf("a", "b", "c")) { input, time, output -> TimedIO(time, input, output) },
        )
    val trace2 =
        TimedIOTrace(
            "z",
            List(3) {}
                .zip(listOf(1.seconds, 4.seconds, 3.seconds))
                .zip(listOf("a", "b", "d")) { input, time, output -> TimedIO(time, input, output) },
        )

    val pta =
        TimedFrequencyPTA<Unit, String>("z").apply {
          addWord(trace1)
          addWord(trace2)
        }

    pta.assertAutomatonStructure(
        State(
            "z",
            1.seconds,
            listOf(1.seconds, 1.seconds),
            Transition(
                Unit,
                2,
                1.0f,
                State(
                    "a",
                    3.seconds,
                    listOf(2.seconds, 4.seconds),
                    Transition(
                        Unit,
                        2,
                        1.0f,
                        State(
                            "b",
                            3.seconds,
                            listOf(3.seconds, 3.seconds),
                            Transition(Unit, 1, 0.5f, State("c", null, emptyList())),
                            Transition(
                                Unit,
                                1,
                                0.5f,
                                State("d", null, emptyList()),
                            ),
                        ),
                    ),
                ),
            ),
        )
    )
  }

  @Test
  fun `double IO trace generates correct PTA`() {
    val trace1 =
        TimedIOTrace(
            "z",
            listOf("a", "b", "c").zip(listOf(1.seconds, 2.seconds, 3.seconds)).zip(
                listOf("i", "j", "k")
            ) { input, time, output ->
              TimedIO(time, input, output)
            },
        )
    val trace2 =
        TimedIOTrace(
            "z",
            listOf("a", "b", "d").zip(listOf(1.seconds, 4.seconds, 3.seconds)).zip(
                listOf("i", "j", "l")
            ) { input, time, output ->
              TimedIO(time, input, output)
            },
        )

    val pta =
        TimedFrequencyPTA<String, String>("z").apply {
          addWord(trace1)
          addWord(trace2)
        }

    pta.assertAutomatonStructure(
        State(
            "z",
            1.seconds,
            listOf(1.seconds, 1.seconds),
            Transition(
                "a",
                2,
                1.0f,
                State(
                    "i",
                    3.seconds,
                    listOf(2.seconds, 4.seconds),
                    Transition(
                        "b",
                        2,
                        1.0f,
                        State(
                            "j",
                            3.seconds,
                            listOf(3.seconds, 3.seconds),
                            Transition("c", 1, 1.0f, State("k", null, emptyList())),
                            Transition(
                                "d",
                                1,
                                1.0f,
                                State("l", null, emptyList()),
                            ),
                        ),
                    ),
                ),
            ),
        )
    )
  }
}
