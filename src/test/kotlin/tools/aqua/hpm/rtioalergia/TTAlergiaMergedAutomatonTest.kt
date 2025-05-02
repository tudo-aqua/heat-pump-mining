// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.rtioalergia

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.Test
import tools.aqua.hpm.data.TimedIO
import tools.aqua.hpm.data.TimedIOTrace
import tools.aqua.hpm.util.KnownState
import tools.aqua.hpm.util.State
import tools.aqua.hpm.util.Transition
import tools.aqua.hpm.util.assertAutomatonStructure
import tools.aqua.hpm.util.zip

class TTAlergiaMergedAutomatonTest {

  @Test
  fun `single word learns correctly`() {
    val trace =
        TimedIOTrace(
            "z",
            listOf("a", "b", "c").zip(listOf(1.seconds, 2.seconds, 3.seconds)).zip(
                listOf("i", "j", "k")) { input, time, output ->
                  TimedIO(time, input, output)
                })

    val automaton =
        RTIOAlergiaMergedAutomaton(TimedFrequencyPTA<String, String>("z").apply { addWord(trace) })

    automaton.assertAutomatonStructure(
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
                            Transition("c", 1, 1.0f, State("k", 0.seconds, emptyList()))))))))
  }

  @Test
  fun `two words learn correctly`() {
    val trace1 =
        TimedIOTrace(
            "z",
            listOf("a", "b", "c").zip(listOf(1.seconds, 2.seconds, 3.seconds)).zip(
                listOf("i", "j", "k")) { input, time, output ->
                  TimedIO(time, input, output)
                })
    val trace2 =
        TimedIOTrace(
            "z",
            listOf("a", "b", "d").zip(listOf(1.seconds, 4.seconds, 3.seconds)).zip(
                listOf("i", "j", "l")) { input, time, output ->
                  TimedIO(time, input, output)
                })

    val automaton =
        RTIOAlergiaMergedAutomaton(
            TimedFrequencyPTA<String, String>("z").apply {
              addWord(trace1)
              addWord(trace2)
            })

    automaton.assertAutomatonStructure(
        State(
            "z",
            1.seconds,
            List(2) { 1.seconds },
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
                            List(2) { 3.seconds },
                            Transition("c", 1, 1.0f, State("k", 0.seconds, emptyList())),
                            Transition(
                                "d",
                                1,
                                1.0f,
                                State("l", 0.seconds, emptyList()),
                            )))))))
  }

  @Test
  fun `loop learns correctly`() {
    val trace =
        TimedIOTrace(
            "z",
            List(3) { "a" }
                .zip(List(3) { 1.seconds })
                .zip(List(3) { "z" }) { input, time, output -> TimedIO(time, input, output) })

    val automaton =
        RTIOAlergiaMergedAutomaton(TimedFrequencyPTA<String, String>("z").apply { addWord(trace) })

    automaton.assertAutomatonStructure(
        State("z", 1.seconds, List(3) { 1.seconds }, Transition("a", 3, 1.0f, KnownState(0, "z"))))
  }

  @Test
  fun `time difference prevents all merges`() {
    val trace =
        TimedIOTrace(
            "z",
            List(100) { "a" }
                .zip(listOf(100_000.days, 100_000.days, 1.seconds))
                .zip(List(100) { "z" }) { input, time, output -> TimedIO(time, input, output) })

    val automaton =
        RTIOAlergiaMergedAutomaton(
            TimedFrequencyPTA<String, String>("z").apply {
              addWord(trace)
              addWord(trace)
              addWord(trace)
            })

    automaton.assertAutomatonStructure(
        State(
            "z",
            100_000.days,
            List(3) { 100_000.days },
            Transition(
                "a",
                3,
                1.0f,
                State(
                    "z",
                    100_000.days,
                    List(3) { 100_000.days },
                    Transition(
                        "a",
                        3,
                        1.0f,
                        State(
                            "z",
                            1.seconds,
                            List(3) { 1.seconds },
                            Transition("a", 3, 1.0f, KnownState(0, "z"))))))))
  }

  @Test
  fun `relaxation collapses unlabeled loop`() {
    val trace =
        TimedIOTrace(
            "z",
            List(3) { "a" }
                .zip(listOf(100_000.days, 100_000.days, 1.seconds))
                .zip(List(3) { "z" }) { input, time, output -> TimedIO(time, input, output) })

    val automaton =
        RTIOAlergiaMergedAutomaton(
            TimedFrequencyPTA<String, String>("z").apply {
              addWord(trace)
              addWord(trace)
              addWord(trace)
            },
            timingSimilaritySignificanceDecay = 0.0)

    automaton.assertAutomatonStructure(
        State(
            "z",
            (100_000.days * 2 / 3 + 1.seconds * 1 / 3),
            List(6) { 100_000.days } + List(3) { 1.seconds },
            Transition("a", 9, 1.0f, KnownState(0, "z"))))
  }

  @Test
  fun `relaxation partially collapses labeled loop`() {
    val trace =
        TimedIOTrace(
            "z",
            List(3) { "a" }
                .zip(listOf(100_000.days, 100_000.days, 1.seconds))
                .zip(listOf("z", "i", "z")) { input, time, output -> TimedIO(time, input, output) })

    val automaton =
        RTIOAlergiaMergedAutomaton(
            TimedFrequencyPTA<String, String>("z").apply {
              addWord(trace)
              addWord(trace)
              addWord(trace)
            },
            timingSimilaritySignificanceDecay = 0.0)

    automaton.assertAutomatonStructure(
        State(
            "z",
            100_000.days,
            List(6) { 100_000.days },
            Transition(
                "a",
                3,
                0.5f,
                State(
                    "i",
                    1.seconds,
                    List(3) { 1.seconds },
                    Transition("a", 3, 1.0f, KnownState(0, "z")))),
            Transition("a", 3, 0.5f, KnownState(0, "z"))))
  }
}
