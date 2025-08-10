<!--
  SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md

  SPDX-License-Identifier: CC-BY-4.0
  -->

# Heat Pump Mining Tools

This repository contains the HPM tools for

1. Trace selection and merging
2. Learning
3. Validation

plus an additional format converter.

## Project

The project can be used via Gradle:

```shell
./gradlew build
./gradlew run --args "--help"  # or similar
```

Additionally, executable bundles can be built:

```shell
./gradlew application  # ZIP with dependencies
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
parameters).

### Converter

Converts the change point detector's format to the JSON format used by this tool.

```shell
heat-pump-mining convert-format -i NibeData_EventTrace_$number.zip -o nibe.json.zst
```

### Select and Merge

Selects a subset of events from the inputs. This requires a key event and a list of events. It then
uses the change points detected for the key event to segment the trace resulting from the
combination of all selected events. The change points for non-key events are ignored. If the option
`-f` is set, only one trace corresponding to key change 0 is created, if `-a` is set, each trace is
converted ( continuing to the end), and if `-n` is set, each trace is shortened to the beginning of
the next trace and converted ( the default setting).

```shell
heat-pump-mining select-and-merge -i nibe.json.zst -o selected.json.zst -e event1 event2 ... -s key_event
```

### Generate Traces

Generate random traces from a given automaton. The number of traces can be controlled via `-n`. The
length is selected from a normal distribution from the mean `-l`, the standard deviation `-L`, and
the optional seed `-s` (default `0`).

```shell
heat-pump-mining generate-traces -a examples.dot -o traces.json.zst -n 100 -l 1000 -L 22.5
```

### Learn

Run the RTIOAlergia algorithm on a given JSON trace database, ignoring all traces that do not have
the initial trace's starting output. This accepts multiple options affecting the algorithm's
behavior:

- `-b` changes the blue state selection for the Blue-Fringe component. Defaults to `LEX_ORDER`.
- `-p` runs the merge search in parallel (off by default).
- `-d` ensures that the merge search result is deterministic (active by default).
- `-f` selects the transition similarity levels required for merges. Between 0 and 1, defaults to
  0.05. 0 disables the test, 1 always fails. This corresponds to _half_ the Hoeffding bound.
- `-fd` determines the decay of `-f` when exploring successive merges per step. Between 0 and 1,
  defaults to 1. 0 corresponds to the simplified Alergia algorithm, 1 to Alergia.
- `-t` and `-td` are the same for the timing similarity required (i.e., f-Test significance).
- `-T` can be used to enable a k-tails-like mode in which `-f` and `-t` are forced to 0 after the
  selected number of steps.
- `-m` performs all statistical tests on the current merged form of a state. This is statistically
  unsound, but may perform better on limited data.

```shell
heat-pump-mining learn -i selected.json.zst -o nibe.dot
```

### Validate Revision Score

This measures a learned automaton's performance on all traces of a database using the revision score
metric. Two measures are computed: revision score (i.e., inverse significances of the trace
observations and the automaton's) and trace likeliness. Note that the latter is usually rounded down
to zero for longer traces. The option `-f` can be used to shift the revision score weight towards
transition likeliness; default is 0.5 (even weight). `-n` removes a normalization from the trace
likeliness, resulting in slightly larger result values.

```shell
heat-pump-mining validate-revision-score -a nibe.dot -i validation.json.zst
```

### Validate Hitting Time Prediction

This measures a learned automaton's performance on all traces of a database w.r.t. mean hitting
times errors. The events for which hitting times should be checked must be given via the `-e`
option.

```shell
heat-pump-mining validate-hitting-times -a nibe.dot -i validation.json.zst -e d/event_defrost_0.0 d/event_defrost_2.0
```
