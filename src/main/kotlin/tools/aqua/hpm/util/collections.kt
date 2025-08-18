// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.util

fun <T> List<T>.subListFrom(fromIndex: Int): List<T> = subList(fromIndex, size)

fun <T> List<T>.subListTo(toIndex: Int): List<T> = subList(0, toIndex)

val <T> List<T>.prefixes: List<List<T>>
  get() = (0..size).map(::subListTo)

val <T> List<T>.suffixes: List<List<T>>
  get() = (0..size).map(::subListFrom)

fun <T> Collection<T>.takeEvenlySpaced(n: Int): List<T> {
  require(n <= size) { "cannot take more elements than contained in the collection" }
  if (n == size) return toList()
  val threshold = size
  return buildList(n) {
    var willTake = threshold
    this@takeEvenlySpaced.forEach {
      if (willTake >= threshold) {
        this += it
        willTake -= threshold
      }
      willTake += n
    }
  }
}

operator fun <T> List<T>.component6() = get(5)

operator fun <T> List<T>.component7() = get(6)

operator fun <T> List<T>.component8() = get(7)

operator fun <T> List<T>.component9() = get(8)

operator fun <T> List<T>.component10() = get(9)

operator fun <T> List<T>.component11() = get(10)

operator fun <T> List<T>.component12() = get(11)

operator fun <T> List<T>.component13() = get(12)

operator fun <T> List<T>.component14() = get(13)

operator fun <T> List<T>.component15() = get(14)

fun <T> Iterable<T>.allSame(): Boolean {
  val iter = iterator()
  if (!iter.hasNext()) return true
  val value = iter.next()
  for (e in iter) {
    if (e != value) return false
  }
  return true
}
