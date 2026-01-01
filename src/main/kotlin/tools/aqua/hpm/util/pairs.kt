// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.util

import net.automatalib.alphabet.Alphabet
import net.automatalib.common.util.Pair as APair
import net.automatalib.serialization.InputModelData
import net.automatalib.ts.simple.SimpleTS

operator fun <T1, T2> APair<T1, T2>.component1(): T1 = first

operator fun <T1, T2> APair<T1, T2>.component2(): T2 = second

operator fun <I, M : SimpleTS<*, I>> InputModelData<I, M>.component1(): Alphabet<I> = alphabet

operator fun <I, M : SimpleTS<*, I>> InputModelData<I, M>.component2(): M = model
