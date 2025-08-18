#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
#
# SPDX-License-Identifier: Apache-2.0

set -euxo pipefail

if [ $# -lt 3 ]; then
  echo \
    classic-loose \
    classic-strict \
    merged-loose \
    merged-strict \
    decaying \
    bounded-low \
    bounded-medium \
    bounded-high \
    classic-loose-untimed \
    classic-strict-untimed \
    merged-loose-untimed \
    merged-strict-untimed \
    decaying-untimed \
    bounded-low-untimed \
    bounded-medium-untimed \
    bounded-high-untimed
  exit
fi

strict_epsilon=0.95
loose_epsilon=0.05

decay=0.5

low_k=2
medium_k=5
high_k=10

# timed
heat-pump-mining learn -i "$1-$2-training-$3.json.zst" -o "$1-$2-classic-loose-$3.dot" -d -f $loose_epsilon -t $loose_epsilon -m
heat-pump-mining learn -i "$1-$2-training-$3.json.zst" -o "$1-$2-classic-strict-$3.dot" -d -f $strict_epsilon -t $strict_epsilon -m

heat-pump-mining learn -i "$1-$2-training-$3.json.zst" -o "$1-$2-merged-loose-$3.dot" -d -f $loose_epsilon -t $loose_epsilon -u
heat-pump-mining learn -i "$1-$2-training-$3.json.zst" -o "$1-$2-merged-strict-$3.dot" -d -f $strict_epsilon -t $strict_epsilon -u

heat-pump-mining learn -i "$1-$2-training-$3.json.zst" -o "$1-$2-decaying-$3.dot" -d -f $strict_epsilon -fd $decay -t $strict_epsilon -td $decay -u

heat-pump-mining learn -i "$1-$2-training-$3.json.zst" -o "$1-$2-bounded-low-$3.dot" -d -f $strict_epsilon -t $strict_epsilon -T $low_k -u
heat-pump-mining learn -i "$1-$2-training-$3.json.zst" -o "$1-$2-bounded-medium-$3.dot" -d -f $strict_epsilon -t $strict_epsilon -T $medium_k -u
heat-pump-mining learn -i "$1-$2-training-$3.json.zst" -o "$1-$2-bounded-high-$3.dot" -d -f $strict_epsilon -t $strict_epsilon -T $high_k -u

# untimed
heat-pump-mining learn -i "$1-$2-training-$3.json.zst" -o "$1-$2-classic-loose-untimed-$3.dot" -d -f $loose_epsilon -t 0 -m
heat-pump-mining learn -i "$1-$2-training-$3.json.zst" -o "$1-$2-classic-strict-untimed-$3.dot" -d -f $strict_epsilon -t 0 -m

heat-pump-mining learn -i "$1-$2-training-$3.json.zst" -o "$1-$2-merged-loose-untimed-$3.dot" -d -f $loose_epsilon -t 0 -u
heat-pump-mining learn -i "$1-$2-training-$3.json.zst" -o "$1-$2-merged-strict-untimed-$3.dot" -d -f $strict_epsilon -t 0 -u

heat-pump-mining learn -i "$1-$2-training-$3.json.zst" -o "$1-$2-decaying-untimed-$3.dot" -d -f $strict_epsilon -fd $decay -t 0 -u

heat-pump-mining learn -i "$1-$2-training-$3.json.zst" -o "$1-$2-bounded-low-untimed-$3.dot" -d -f $strict_epsilon -t 0 -T $low_k -u
heat-pump-mining learn -i "$1-$2-training-$3.json.zst" -o "$1-$2-bounded-medium-untimed-$3.dot" -d -f $strict_epsilon -t 0 -T $medium_k -u
heat-pump-mining learn -i "$1-$2-training-$3.json.zst" -o "$1-$2-bounded-high-untimed-$3.dot" -d -f $strict_epsilon -t 0 -T $high_k -u
