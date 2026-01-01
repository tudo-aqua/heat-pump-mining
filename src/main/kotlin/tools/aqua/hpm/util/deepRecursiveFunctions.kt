// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.util

operator fun <A, B, Z> DeepRecursiveFunction<Pair<A, B>, Z>.invoke(a: A, b: B): Z = this(a to b)

suspend fun <A, B, Z> DeepRecursiveScope<Pair<A, B>, Z>.callRecursive(a: A, b: B): Z =
    callRecursive(a to b)

operator fun <A, B, C, Z> DeepRecursiveFunction<Triple<A, B, C>, Z>.invoke(a: A, b: B, c: C): Z =
    this(Triple(a, b, c))

suspend fun <A, B, C, Z> DeepRecursiveScope<Triple<A, B, C>, Z>.callRecursive(a: A, b: B, c: C): Z =
    callRecursive(Triple(a, b, c))
