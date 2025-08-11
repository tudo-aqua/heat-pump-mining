#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
#
# SPDX-License-Identifier: Apache-2.0

set -euxo pipefail

key_event=defrost
choice=(
  boiler_on
  booster_on
  defrost
  force_dhw
  legionella_prevention
  outdoor_temp
  '#outside'
  '#running'
  '#running_heating'
)

hpm() {
  if command -v heat-pump-mining >/dev/null 2>&1; then
    heat-pump-mining "$@"
  else
    singularity run build/singularity/hpm.sif heat-pump-mining "$@"
  fi
}

# import data
hpm convert-format -i "$1.zip" -o nibe.json.zst

# get event array
readarray -t all_events < <(
  zstdcat nibe.json.zst | jq -r .logs.[].name | sed 's#.*/.*/.*/\(.*\)/.*#\1#' | sort -u
)

# create split logs
hpm select-and-merge -i nibe.json.zst -o all-full.json.zst -e "${all_events[@]}" -s $key_event -n
hpm select-and-merge -i nibe.json.zst -o manual-choice-full.json.zst -e "${choice[@]}" -s $key_event -n
hpm select-and-merge -i nibe.json.zst -o only-$key_event-full.json.zst -e $key_event -s $key_event -n

splits=(all manual-choice "only-$key_event")

# count logs (determined by key event)
n_logs=$(zstdcat all-full.json.zst | jq -r '.logs | length')

share_of_logs() {
  python3 -c "
from random import Random
print(', '.join(f'\"{x}\"' for x in Random(0).sample(range(0, $n_logs), k=$n_logs//$1)))
"
}

split_off() {
  zstdcat "$1-full.json.zst" |
    jq '{ "name": .name + " (selected: '"${2//\"/}}"')" } * ([.logs.[] | select(.name | split("/").[-1] as $n | ['"$2"'] | index($n))] | { "logs": . })' |
    zstd >"$1-$3-train.json.zst"
  zstdcat "$1-full.json.zst" |
    jq '{ "name": .name + " (selected: '"${2//\"/}}"')" } * ([.logs.[] | select(.name | split("/").[-1] as $n | ['"$2"'] | index($n) | not)] | { "logs": . })' |
    zstd >"$1-$3-validate.json.zst"
}

split_off_shares() {
  split_off "$1" "$(share_of_logs 2)" half
  split_off "$1" "$(share_of_logs 4)" quarter
  split_off "$1" "$(share_of_logs 8)" eighth
  split_off "$1" '"0"' first
}

shares=(half quarter eighth first)

for s in "${splits[@]}"; do
  split_off_shares "$s"
done

strict_epsilon=0.95
loose_epsilon=0.05

decay=0.5

low_k=2
medium_k=5
high_k=10

learn() {
  hpm learn -i "$1-train.json.zst" -o "$1-classic-loose.dot" -d -f $loose_epsilon -t $loose_epsilon -m
  hpm learn -i "$1-train.json.zst" -o "$1-classic-strict.dot" -d -f $strict_epsilon -t $strict_epsilon -m
  hpm learn -i "$1-train.json.zst" -o "$1-merged-loose.dot" -d -f $loose_epsilon -t $loose_epsilon -u
  hpm learn -i "$1-train.json.zst" -o "$1-merged-strict.dot" -d -f $strict_epsilon -t $strict_epsilon -u
  hpm learn -i "$1-train.json.zst" -o "$1-decaying.dot" -d -f $strict_epsilon -fd $decay -t $strict_epsilon -td $decay -u
  hpm learn -i "$1-train.json.zst" -o "$1-bounded-low.dot" -d -f $strict_epsilon -t $strict_epsilon -T $low_k -u
  hpm learn -i "$1-train.json.zst" -o "$1-bounded-medium.dot" -d -f $strict_epsilon -t $strict_epsilon -T $medium_k -u
  hpm learn -i "$1-train.json.zst" -o "$1-bounded-high.dot" -d -f $strict_epsilon -t $strict_epsilon -T $high_k -u
}

regimes=(
  classic-loose classic-strict
  merged-loose merged-strict
  decaying
  bounded-low bounded-medium bounded-high
)

for sp in "${splits[@]}"; do
  for sh in "${shares[@]}"; do
    learn "$sp-$sh"
  done
done

predicted_events=(
  d/event_defrost_0.0
  d/event_defrost_2.0
)

validate() {
  hpm validate-revision-score -a "$1.dot" -i "$2-validate.json.zst" -o "$1-revision.csv" -p
  if [[ "$1" == all-* ]]; then true; else
    hpm validate-hitting-times -a "$1.dot" -i "$2-validate.json.zst" -e "${predicted_events[@]}" -o "$1-times.csv" -m 50 -p
  fi
}

for sp in "${splits[@]}"; do
  for sh in "${shares[@]}"; do
    for r in "${regimes[@]}"; do
      validate "$sp-$sh-$r" "$sp-$sh"
    done
  done
done

echo "split, share, regime, states, transitions, rs-mean, rs-stdev, bad-automaton, htd-dnfs, htd-mean, htd-stdev" > "$1-results.csv"

for sp in "${splits[@]}"; do
  for sh in "${shares[@]}"; do
    for r in "${regimes[@]}"; do
      n_q="$(grep -c -E $'\t''s[0-9]+ \[' "$sp-$sh-$r.dot")"
      n_t="$(grep -c -E 's[0-9]+ -> s[0-9]+' "$sp-$sh-$r.dot")"
      summarize "$sp-$sh-$r" "$sp, $sh, $r, $n_q, $n_t" >> "$1-results.csv"
    done
  done
done
