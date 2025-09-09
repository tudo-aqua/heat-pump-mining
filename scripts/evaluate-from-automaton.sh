#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
#
# SPDX-License-Identifier: Apache-2.0

set -euxo pipefail

# generate data
heat-pump-mining generate-traces -a "$1.dot" -o "$1-0-training-1.json.zst" -n "$3" -l "$4" --normal-distribution --milliseconds
heat-pump-mining generate-traces -a "$1.dot" -o "$1-0-validation.json.zst" -n "$5" -l "$6" --normal-distribution --milliseconds

# learn
learn "$1" 0 1

# validate
validate() {
  heat-pump-mining validate-revision-score -a "$1-0-$2-1.dot" -i "$1-0-validation.json.zst" -o "$1-0-$2-1-revision.csv" -n -p
  heat-pump-mining validate-hitting-times -a "$1-0-$2-1.dot" -i "$1-0-validation.json.zst" -e "$3" -o "$1-0-$2-1-times.csv" -p
}

for regime in $(learn); do
  validate "$1" "$regime" "$2"
done

# shellcheck disable=SC2046
heat-pump-mining summarize-results -d . -c "$1" -r 1 -s $(learn) -l 1 --seconds -o "$1-results.csv"
