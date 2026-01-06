#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
#
# SPDX-License-Identifier: Apache-2.0

set -euxo pipefail

compute_revision_score() {
  if [[ "$3" != "$4" ]]; then name="$3-$4"; else name="$3-self"; fi
  heat-pump-mining compute-revision-score \
    --automaton "automaton-$1-$3.dot" \
    --input "traces-$1-$4.json.zst" \
    --output "revision-score-$2-$1-$name.csv" \
    "${@:5}"
}

run_evaluation() {
  heat-pump-mining generate-automata \
    --output "automaton-$1.dot" \
    --automata 100 \
    --alphabet "${@:2}" \
    --min-size $((("$#" - 1) * 25 / 3)) --max-size $((("$#" - 1) * 50 / 3)) \
    --min-exit-time 1m --max-exit-time 30m \
    --seed $#

  heat-pump-mining generate-sub-csl \
    --output "csl-$1.txt" \
    --formulas 1000 \
    --alphabet "${@:2}" \
    --leaf-probability 0.9 \
    --min-duration 0s --max-duration 2h \
    --seed $#

  for a in $(seq 0 99); do
    heat-pump-mining generate-traces \
      --automaton "automaton-$1-$a.dot" \
      --output "traces-$1-$a.json.zst" \
      --traces 1000 \
      --mean-length 10000 \
      --exponential-distribution \
      --seconds \
      --seed $#

    heat-pump-mining evaluate-sub-csl \
      --sub-csl "csl-$1.txt" \
      --traces "traces-$1-$a.json.zst" \
      --output "csl-results-$1-$a.txt"
  done

  for left in $(seq 0 99); do
    for right in $(seq 0 99); do
      compute_revision_score "$1" sb "$left" "$right" --single-best
      compute_revision_score "$1" sr "$left" "$right" --single-rooted
      compute_revision_score "$1" gb "$left" "$right" --global
    done
  done
}

run_evaluation small A B C
run_evaluation large A B C D E F
