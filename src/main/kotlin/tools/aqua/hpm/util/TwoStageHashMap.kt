// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.util

class TwoStageHashMap<K1, K2, V : Any> : AbstractCollection<V>() {
  private val map = mutableMapOf<K1, MutableMap<K2, V>>()

  override fun iterator(): Iterator<V> = iterator {
    map.values.forEach { sub -> sub.values.forEach { yield(it) } }
  }

  override val size: Int
    get() = map.values.sumOf { it.size }

  operator fun get(key1: K1): Collection<V> = map[key1]?.values ?: emptyList()

  operator fun get(
      key1: K1,
      key2: K2,
  ): V? = map[key1]?.get(key2)

  fun put(key1: K1, key2: K2, value: V): V? =
      map.computeIfAbsent(key1) { mutableMapOf() }.put(key2, value)

  fun computeIfAbsent(key1: K1, key2: K2, valueCompute: () -> V): V =
      map.computeIfAbsent(key1) { mutableMapOf() }.computeIfAbsent(key2) { valueCompute() }

  fun compute(key1: K1, key2: K2, valueUpdate: (V?) -> V): V =
      map.computeIfAbsent(key1) { mutableMapOf() }
          .compute(key2) { _, oldValue -> valueUpdate(oldValue) }!!

  fun remove(
      key1: K1,
      key2: K2,
  ): V? = map[key1]?.remove(key2)

  fun clear() = map.clear()
}

operator fun <K1, K2, V : Any> TwoStageHashMap<K1, K2, V>.set(key1: K1, key2: K2, value: V) {
  put(key1, key2, value)
}
