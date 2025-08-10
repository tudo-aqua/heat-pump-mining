#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
#
# SPDX-License-Identifier: Apache-2.0

set -euxo pipefail

hpm() {
  if command -v heat-pump-mining >/dev/null 2>&1; then
    heat-pump-mining "$@"
  else
    singularity run build/singularity/hpm.sif heat-pump-mining "$@"
  fi
}

hpm generate-traces -a "$1.dot" -o "$1-train.json.zst" -n 1 -l 10000
hpm generate-traces -a "$1.dot" -o "$1-validate.json.zst" -n 100 -l 100

strict_epsilon=0.95
loose_epsilon=0.05

decay=0.5

low_k=2
medium_k=5
high_k=10

hpm learn -i "$1-train.json.zst" -o "$1-classic-loose.dot" -d -f $loose_epsilon -t $loose_epsilon -m
hpm learn -i "$1-train.json.zst" -o "$1-classic-strict.dot" -d -f $strict_epsilon -t $strict_epsilon -m
hpm learn -i "$1-train.json.zst" -o "$1-merged-loose.dot" -d -f $loose_epsilon -t $loose_epsilon -u
hpm learn -i "$1-train.json.zst" -o "$1-merged-strict.dot" -d -f $strict_epsilon -t $strict_epsilon -u
hpm learn -i "$1-train.json.zst" -o "$1-decaying.dot" -d -f $strict_epsilon -fd $decay -t $strict_epsilon -td $decay -u
hpm learn -i "$1-train.json.zst" -o "$1-bounded-low.dot" -d -f $strict_epsilon -t $strict_epsilon -T $low_k -u
hpm learn -i "$1-train.json.zst" -o "$1-bounded-medium.dot" -d -f $strict_epsilon -t $strict_epsilon -T $medium_k -u
hpm learn -i "$1-train.json.zst" -o "$1-bounded-high.dot" -d -f $strict_epsilon -t $strict_epsilon -T $high_k -u

regimes=(
  classic-loose classic-strict
  merged-loose merged-strict
  decaying
  bounded-low bounded-medium bounded-high
)

predicted_event=b

validate() {
  hpm validate-revision-score -a "$1.dot" -i "$2-validate.json.zst" -o "$1-revision.csv" -p
  hpm validate-hitting-times -a "$1.dot" -i "$2-validate.json.zst" -e $predicted_event -o "$1-times.csv" -p
}

for r in "${regimes[@]}"; do
  validate "$1-$r" "$1"
done

echo "regime, states, transitions, rs-mean, rs-stdev, bad-automaton, htd-dnfs, htd-mean, htd-stdev" > "$1-results.csv"

for r in "${regimes[@]}"; do
  n_q="$(grep -c -E $'\t''s[0-9]+ \[' "$1-$r.dot")"
  n_t="$(grep -c -E 's[0-9]+ -> s[0-9]+' "$1-$r.dot")"
  summarize "$1-$r" "$r, $n_q, $n_t" >> "$1-results.csv"
done
