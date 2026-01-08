#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
#
# SPDX-License-Identifier: Apache-2.0

set -euxo pipefail

n_automata="${1:-100}"
states_min_factor="${2:-25}"
states_max_factor="${2:-50}"
n_formulas="${4:-1000}"
n_traces="${5:-1000}"
trace_length="${6:-10000}"

exponents=(1 2)
modes_short=(sb sr gr)
modes_flags=(--single-best --single-rooted --global)

compute_sub_csl_score() {
  if [[ "$3" != "$4" ]]; then name="$3-$4"; else name="$3-self"; fi

  csl_data="$(csvjoin -c formula "csl-results-$1-$3.csv" "csl-results-$1-$4.csv" |
    csvgrep -c formula -im '<average>' |
    csvcut -c 'formula,satisfactionShare,satisfactionShare2')"

  echo "$(echo "$csl_data" | head -n1),delta" >"csl-score-e$2-$1-$name.csv"

  echo "$csl_data" | tail -n+2 | while IFS='', read -r formula left right; do
    echo -n "$formula,$left,$right,"
    echo "$left $right $2" | gawk -v OFMT='%f' '{ $r=($1 - $2)^$3; print ($r < 0) ? -$r : $r }'
  done >>"csl-score-e$2-$1-$name.csv"

  mean_ss1="$(LC_ALL=C csvstat -c satisfactionShare --mean "csl-score-e$2-$1-$name.csv")"
  mean_ss2="$(LC_ALL=C csvstat -c satisfactionShare2 --mean "csl-score-e$2-$1-$name.csv")"
  mean_d="$(echo "$(LC_ALL=C csvstat -c delta --mean "csl-score-e$2-$1-$name.csv") $2" |
    gawk -v OFMT='%f' '{ print 1-$1^(1/$2) }')"
  echo "<average>,$mean_ss1,$mean_ss2,$mean_d" >>"csl-score-e$2-$1-$name.csv"
}

compute_revision_score() {
  if [[ "$3" != "$4" ]]; then name="$3-$4"; else name="$3-self"; fi
  heat-pump-mining compute-revision-score \
    --automaton "automaton-$1-$3.dot" \
    --input "traces-$1-$4.json.zst" \
    --output "revision-score-$2-$1-$name.csv" \
    "${@:5}"
}

compute_comparison() {
  if [[ "$4" != "$5" ]]; then name="$4-$5"; else name="$4-self"; fi

  sub_csl_score="$(csvgrep -c formula -m '<average>' "csl-score-e$2-$1-$name.csv" |
    csvcut -c delta |
    tail -n+2)"
  revision_score="$(csvgrep -c log -r '<global>|<average>' "revision-score-$3-$1-$name.csv" |
    csvcut -c score |
    tail -n+2)"
  echo "$4,$5,$sub_csl_score,$revision_score"
}

run_evaluation() {
  heat-pump-mining generate-automata \
    --output "automaton-$1.dot" \
    --automata "$n_automata" \
    --alphabet "${@:2}" \
    --min-size $((("$#" - 1) * "$states_min_factor" / 3)) --max-size $((("$#" - 1) * "$states_max_factor" / 3)) \
    --min-exit-time 1m --max-exit-time 30m \
    --seed $#

  heat-pump-mining generate-sub-csl \
    --output "csl-$1.txt" \
    --formulas "$n_formulas" \
    --alphabet "${@:2}" \
    --leaf-probability 0.9 \
    --min-duration 0s --max-duration 2h \
    --seed $#

  for a in $(seq 0 $(("$n_automata" - 1))); do
    heat-pump-mining generate-traces \
      --automaton "automaton-$1-$a.dot" \
      --output "traces-$1-$a.json.zst" \
      --traces "$n_traces" \
      --mean-length "$trace_length" \
      --exponential-distribution \
      --seconds \
      --seed $#

    heat-pump-mining evaluate-sub-csl \
      --sub-csl "csl-$1.txt" \
      --traces "traces-$1-$a.json.zst" \
      --output "csl-results-$1-$a.csv"
  done

  for e in "${exponents[@]}"; do
    for m in "${modes_short[@]}"; do
      echo "left,right,cslScore,rsScore" >"csl-rs-$1-$e-$m.csv"
    done
  done

  for left in $(seq 0 $(("$n_automata" - 1))); do
    for right in $(seq 0 $(("$n_automata" - 1))); do
      for e in "${exponents[@]}"; do
        compute_sub_csl_score "$1" "$e" "$left" "$right"
      done

      for mi in $(seq 0 $(("${#modes_short[*]}" - 1))); do
        compute_revision_score "$1" "${modes_short[$mi]}" "$left" "$right" "${modes_flags[$mi]}"
      done

      for e in "${exponents[@]}"; do
        for m in "${modes_short[@]}"; do
          compute_comparison "$1" "$e" "$m" "$left" "$right" >>"csl-rs-$1-$e-$m.csv"
        done
      done
    done
  done
}

run_evaluation small A B C
run_evaluation large A B C D E F
