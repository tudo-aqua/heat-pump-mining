<!--
  SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md

  SPDX-License-Identifier: CC-BY-4.0
  -->

# Heat Pump Mining Tools

This repository contains the HPM tools for

1. Automaton and SubCSL synthesis
2. Trace generation
3. Data import from Nibe ZIP format
4. Trace selection and merging
5. Learning
6. Validation.

## Project

The project can be used via Gradle:

```shell
./gradlew build
./gradlew run --args "--help"  # or similar
```

Additionally, executable bundles can be built:

```shell
./gradlew distZip  # ZIP with dependencies
./gradlew jpackage  # Installer with JAR, needs platform build tools
./gradlew singularityImage  # Singularity image, needs singularity and Linux platform
```

Style is enforced via Spotless and wrapped inside Gradle's testing machinery:

```shell
./gradlew spotlessApply  # Format all files, should be run before committing or full building
./gradlew check  # Run tests and style checker
```

## Bundled Tools

When properly installed via the artifact created by jpackage or inside the singularity container,
the tool can be invoked as `heat-pump-mining [ARGS]`. Note that when running via Gradle or out of an
IDE, only the arguments can be passed (i.e., `heat-pump-mining` is _not_ part of the normal
parameters). The tools are documented in alphabetical order.

Note that for trace databases, compression is automatically determined by the file name extension:
`data.json.zst` is automatically (de-)coompressed using the Zstandard algorithm, `data.json.gz` uses
Gzip, `data.json` is uncompressed, etc.

### Hitting Time Computation

This is a tool for validation that compares the hitting times predicted by an automaton to those in
an actual trace and reports the resulting delta. The trace is split into prefix-suffix pairs with
increasing prefix size. The prefix is then used to compute the current location in the automaton
using Viterbi's algorithm. Then, a prediction for hitting certain events is made and compared to the
data in the actual traces.

Usage:

```shell
heat-pump-mining compute-hitting-times <options>
```

Options:

- `--automaton automaton.dot` the automaton to use for predictions.
- `--input traces.json.zst` the trace database.
- `--output results.csv` results file, defaults to standard output.
- `--init` prepend the initial state's output to the trace, required when used for learning.
- `--parallel` enable multithreaded computation.
- `--events event_1 event_2 …` the target events for hitting time prediction.
- `--samples n` the number of prefix-suffix splits to perform.

### Revision Score Computation

A validation tool that computes the revision score for a given trace database, i.e., the agreement
of the traces and the automaton under IOalergia's stochastic tests. This can operate in a pre-trace
mode, in which a revision score is computed for each trace in the database and then averaged, and a
global mode, in which a score is computed for all traces at once.

Usage:

```shell
heat-pump-mining compute-revision-score <options>
```

Options:

- `--automaton automaton.dot` the automaton to measure.
- `--input traces.json.zst` the trace database.
- `--output results.csv` results file, defaults to standard output.
- `--init` prepend the initial state's output to the trace, required when used for learning.
- one of `--single-best` (per-trace scoring with best trace root), `--single-rooted` (per-trace
  scoring with initial state root), and `--global` (global scoring with initial state root).
- - `--parallel` enable multithreaded computation, only for `--singlle` modes.
- `--frequency-weight n` score bias between frequencies and times, defaults to 0.5.

### Convert Formats

Converts the change point detector's format (Nibe ZIP) to the JSON format used by all other tools in
this software.

Usage:

```shell
heat-pump-mining convert-format <options>
```

Options:

- `--input data.zip` input file.
- `--output data.json.zst` converted file.

### SubCSL Evaluation

This evaluation tool measures the agreement of a given set of SubCSL formulas and a trace database
using the approach for evaluating IOalergia.

Usage:

```shell
heat-pump-mining evaluate-sub-csl <options>
```

Options:

- `--sub-csl formulas.txt` formula file, each on a separate line.
- `--traces traces.json.zst` trace database to use.
- `--output results.csv` results file, defaults to standard output.

### Automata Generator

This tool uses a naive random generation approach to create a family of automata. The algorithm
ensures that for each output, one state exists, and that the resulting automaton is deterministic.

Usage:

```shell
heat-pump-mining generate-automata <options>
```

Options:

- `--output automaton.dot` target file pattern; each generated automaton is suffixed with `-n`.
- `--automata n` number of automata to generate, defaults to one.
- `--alphabet o_1 o_2 …` the output alphabet to use.
- `--min-size n --max-size m` defines the range of states to generate.
- `--min-exit-time n --max-exit-time m` defines the range of exit times to generate.
- `--seed n` defines the generation seed, defaults to zero.

### SubCSL Generator

This tool randomly generates a set of SubCSL formulas. Each formula contains a temporal operator and
propositional components of confugurable depth.

Usage:

```shell
heat-pump-mining generate-sub-csl <options>
```

Options:

- `--output formulas.txt` target file.
- `--formulas n` number of formulas to generate, defaults to one.
- `--alphabet o_1 o_2 …` the output alphabet to use.
- `--variable-probability n` the probability that a variable is selected for a propositional clause.
  Hiigher values yield easier-to-satisfy formulas.
- `--min-interval-start n --max-interval-start m` defines the range of acceptable interval start
  points for the temporal operator to generate.
- `--min-interval-duration n --max-interval-duration m` defines the range of acceptable interval
  lengths for the temporal operator to generate.
- `--seed n` defines the generation seed, defaults to zero.

### Trace Generator

Generate random traces from a given automaton.

Usage:

```shell
heat-pump-mining generate-traces <options>
```

Options:

- `--automaton automaton.dot` the automaton to generate traces from.
- `--output traces.json.zst` the trace database to generate.
- `--traces n` the number of traces to generate, defaults to one.
- `--mean-length n` the mean length of generated traces, defaults to 100.
- `--length-stddev n` standard deviation of lengths, defaults to zero.
- either `--normal-distribution` or `--exponential-distribution` to select the distribution of exit
  times to assume.
- one of `--nanoseconds`, `--microseconds`, `--milliseconds`, `--seconds`, `--minutes`, `--hours`,
  or `--days` to determine the precision used in exit time generation. This should roughly match the
  scale of the automaton's exit times.
- `--seed n` defines the generation seed, defaults to zero.

### Learn

Run the RTIOAlergia algorithm on a given trace database to obtain a matching automaton. If the input
traces do not have the same starting event, a canonical start event is selected and all traces with
a different event are ignored. Alternatively, an additional init event can be prepended to every
trace. Note that this must be accounted for when using the automaton!

Usage:

```shell
heat-pump-mining learn <options>
```

Options:

- `--input traces.json.zst` the trace database to learn from.
- `--output automaton.dot` learned automaton.
- `--init` or `--init value` prepend an initial event to every trace (default: `$init`). This avoids
  ignoring of traces.
- `--blue-state-order order` one of `CANONICAL_ORDER`, `LEX_ORDER`, `FIFO_ORDER`, and `LIFO_ORDER`;
  determines the order in which blue states are processed. Defaults to lexicographic.
- `--parallel` multithreaded checking of merge eligibility.
- `--nondeterministic` sacrifice determinism for faster merging (not recommended).
- `--frequency-epsilon e` the merge eligibility bound for frequency similarity, higher is stricter.
  Defaults to 0.05.
- `--frequency-epsilon-decay z` the merge eligibility bound decay for frequency similarity. Defaults
  to one (disabled).
- `--timing-epsilon e` the merge eligibility bound for timing similarity, higher is stricter.
  Defaults to 0.05.
- `--timing-epsilon-decay z` the merge eligibility bound decay for timing similarity. Defaults to
  one (disabled).
- `--tail-length k` disable stochastic tests in a certain recursive merge depth. Disabled by
  default.
- `--consider-merged` perform stochastic tests on the _merged_ automaton, not the PTA's data.
  Stochastically unsound, but helpful for sparse traces.

### Pretty Printer

This tool pretty-prints an automaton into a shorted, more huma-readable expression. The new
automaton _cannot_ be used by any tool in this software, but can be rendered using Graphviz. The
automaton only retains probabilities and average exit times, but no individual measurements.
Additionally, transitions can be color-graded based on their probabilities.

Usage:

```shell
heat-pump-mining render-automaton <options>
```

Options:

- `--input automaton.dot` the input automaton.
- `--output neat.dot` the rendered automaton.
- `--full-color-min i` the probability above which a transition is rendered in black. Transitions
  below this are rendered in grayscale proportional to their likeliness. Defaults to zero.
- `--render-min i` the minimum probability to render a transition at all. Defaults to zero.

### Select and Merge

Selects a subset of events from the inputs and splits the merged log into sub-logs according to one
of four modes. The modes are:

1. Single Trace: No splitting is performed.
2. Key Event: An event is chosen as the key event. For each log provided for the key event, the
   first event is used to split the log (i.e., [<key_1>, … <key_2>), [<key_2>, …, <key_3>), etc.).
   Optionally, events preceding the first such event are retained (the prefix).
3. Start of Day: A specific time is chosen to start a new split.
4. Interval: A sub-day interval is chosen to split the log. The intervals are aligned at midnight,
   UTC.

Usage:

```shell
heat-pump-mining select-and-merge <options>
```

Options:

- `--input data.json.zst` the input database from a `convert-format` invocation.
- `--output traces.json.zst` the merged trace database.
- `--events e_1 e_2 …` the events to include.
- one of `--single` (Single Trace mode), `--key-event e_k` (Key Event without prefix),
  `--key-event-with-prefix e_k` (Key Event with prefix), `--start-of-day-utc HH:MM` (Start of Day at
  the given time in UTC), or `--interval i` (Interval with the given interval duration).
