#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
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

# import data
heat-pump-mining convert-format -i "$1.zip" -o "$1.json.zst"

# select and merge traces
heat-pump-mining select-and-merge -i "$1.json.zst" -o "$1-manual-choice-all.json.zst" -e "${choice[@]}" -s $key_event -n

n_rounds=3

# create training and validation splits
heat-pump-mining tv-split -i "$1-manual-choice-all.json.zst" -o "$1-manual-choice-{round}-{set}.json.zst" -r $n_rounds

# count training logs (determined by key event)
n_training_max="$(find . -name "$1-manual-choice-0-training-*.json.zst" | sed 's/.*-manual-choice-0-training-\([0-9]\+\).json.zst$/\1/' | sort -n | tail -n 1)"

# learn
for round in $(seq 0 $((n_rounds - 1))); do
  for n_training in $(seq "$n_training_max"); do
    learn "$1-manual-choice" "$round" "$n_training"
  done
done

# validate
predicted_events=(
  d/event_defrost_0.0
  d/event_defrost_2.0
)

validate() {
  heat-pump-mining validate-revision-score -a "$1-$2-$3-$4.dot" -i "$1-$2-validation.json.zst" -o "$1-$2-$3-$4-revision.csv" -n -p
  heat-pump-mining validate-hitting-times -a "$1-$2-$3-$4.dot" -i "$1-$2-validation.json.zst" -e "${predicted_events[@]}" -o "$1-$2-$3-$4-times.csv" -p
}

for round in $(seq 0 $((n_rounds - 1))); do
  for regime in $(learn); do
    for n_training in $(seq "$n_training_max"); do
      validate "$1-manual-choice" "$round" "$regime" "$n_training"
    done
  done
done

# shellcheck disable=SC2046
heat-pump-mining summarize-results -d . -c "$1-manual-choice" -r $n_rounds -s $(learn) -l $(seq "$n_training_max") --seconds -o "$1-results.csv"
