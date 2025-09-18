// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.timing

import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import org.apache.commons.math3.distribution.ExponentialDistribution
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.random.Well19937c
import tools.aqua.hpm.util.average
import tools.aqua.hpm.util.standardDeviation

fun interface TimingDistribution {
  fun sample(data: Collection<Duration>, precision: DurationUnit, random: Random): Duration
}

val exponential = TimingDistribution { data, precision, random ->
  ExponentialDistribution(Well19937c(random.nextInt()), data.average().toDouble(precision))
      .sample()
      .toDuration(precision)
}

val normal = TimingDistribution { data, precision, random ->
  val sd = data.standardDeviation()

  if (sd == ZERO) {
    data.average()
  } else {
    NormalDistribution(
            Well19937c(random.nextInt()),
            data.average().toDouble(precision),
            sd.toDouble(precision))
        .sample()
        .toDuration(precision)
  }
}
