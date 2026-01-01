// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.util

class ThreeStageHashMap<K1, K2, K3, V : Any> : AbstractCollection<V>() {
  private val map = mutableMapOf<K1, TwoStageHashMap<K2, K3, V>>()

  override fun iterator(): Iterator<V> = iterator {
    map.values.forEach { sub -> yieldAll(sub.iterator()) }
  }

  override val size: Int
    get() = map.values.sumOf { it.size }

  operator fun get(key1: K1): Collection<V> = map[key1]?.toList() ?: emptyList()

  operator fun get(
      key1: K1,
      key2: K2,
  ): Collection<V> = map[key1]?.get(key2) ?: emptyList()

  operator fun get(key1: K1, key2: K2, key3: K3): V? = map[key1]?.get(key2, key3)

  fun put(key1: K1, key2: K2, key3: K3, value: V): V? =
      map.computeIfAbsent(key1) { TwoStageHashMap() }.put(key2, key3, value)

  fun computeIfAbsent(key1: K1, key2: K2, key3: K3, valueCompute: () -> V): V =
      map.computeIfAbsent(key1) { TwoStageHashMap() }.computeIfAbsent(key2, key3, valueCompute)

  fun compute(key1: K1, key2: K2, key3: K3, valueUpdate: (V?) -> V): V =
      map.computeIfAbsent(key1) { TwoStageHashMap() }.compute(key2, key3, valueUpdate)

  fun remove(key1: K1): Collection<V>? = map.remove(key1)

  fun remove(
      key1: K1,
      key2: K2,
      key3: K3,
  ): V? = map[key1]?.remove(key2, key3)

  fun clear() = map.clear()
}

operator fun <K1, K2, K3, V : Any> ThreeStageHashMap<K1, K2, K3, V>.set(
    key1: K1,
    key2: K2,
    key3: K3,
    value: V,
) {
  put(key1, key2, key3, value)
}
