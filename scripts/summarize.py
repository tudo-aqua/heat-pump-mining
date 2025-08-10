#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
#
# SPDX-License-Identifier: Apache-2.0

from csv import DictReader, reader as csv_reader
from datetime import timedelta
from isodate import parse_duration
from os.path import isfile
from statistics import mean, stdev
from sys import argv

with open(f'{argv[1]}-revision.csv') as f:
  rs = [float(line['REVISION-SCORE']) for line in DictReader(f, delimiter=',')]

if not isfile(f'{argv[1]}-times.csv'):
  print(f'{argv[2]}, {mean(rs)}, {stdev(rs)}, ???, ???, ???, ???')

else:
  with open(f'{argv[1]}-times.csv') as f:
    csv = csv_reader(f, delimiter=',')
    next(csv)  # skip header
    bad = False
    ds = []
    dnfs = 0
    for line in csv:
      if line[1] == 'false':
        bad = True
      else:
        for d in line[2:]:
          if d == 'DNF':
            dnfs += 1
          elif d != '':
            ds.append(parse_duration(d) / timedelta(microseconds=1))
  if ds:
    mds = mean(ds)
    sds = stdev(ds)
  else:
    mds = 'DNF'
    sds = 'DNF'
  ast = 'bad' if bad else 'good'

  print(f'{argv[2]}, {mean(rs)}, {stdev(rs)}, {ast}, {dnfs}, {mds}, {sds}')
