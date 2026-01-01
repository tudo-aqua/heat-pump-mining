// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua.hpm.subcsl

import kotlin.time.Duration

private sealed interface BracketTree

@JvmInline
private value class Word(val value: String) : BracketTree, CharSequence by value {
  override fun toString(): String = value
}

@JvmInline
private value class BracketedExpression(val inner: List<BracketTree>) :
    BracketTree, List<BracketTree> by inner {
  override fun toString(): String = inner.joinToString(" ", "(", ")")
}

private fun String.toBracketTree(): BracketTree {
  val (result, remaining) = bracketListParser(this.replace("""\s+""".toRegex(), ""))
  check(remaining.isEmpty())
  return BracketedExpression(result)
}

private val bracketListParser =
    DeepRecursiveFunction<String, Pair<List<BracketTree>, String>> { input ->
      var remaining = input

      val result = buildList {
        while (remaining.isNotEmpty() && !remaining.startsWith(')')) {
          if (remaining.startsWith('(')) {
            val (child, remainingNew) = callRecursive(remaining.substring(1))
            add(BracketedExpression(child))

            require(remainingNew.startsWith(')')) {
              "unterminated bracketed child expression, ends with ${remainingNew.firstOrNull()}"
            }
            remaining = remainingNew.substring(1)
          } else {
            val endOfWord =
                remaining.indexOfAny(charArrayOf('(', ')')).let {
                  if (it == -1) remaining.length else it
                }
            this += Word(remaining.substring(0, endOfWord))

            remaining = remaining.substring(endOfWord)
          }
        }
      }

      result to remaining
    }

fun String.toSubCSLFormula(): SubCSLFormula<String> =
    requireNotNull(parseSubCSLFormula(toBracketTree())) { "failed to parse $this" }

private val parseSubCSLFormula = DeepRecursiveFunction { tree ->
  parseUntil.callRecursive(tree)
      ?: parseFinally.callRecursive(tree)
      ?: parseGlobal.callRecursive(tree)
      ?: parsePropositionalFormula.callRecursive(tree)
}

private val parseUntil =
    DeepRecursiveFunction<BracketTree, Until<String>?> { tree ->
      if (tree !is BracketedExpression || tree.size != 3) return@DeepRecursiveFunction null
      val (now, until, later) = tree
      if (now !is BracketedExpression || until !is Word || later !is BracketedExpression)
          return@DeepRecursiveFunction null

      val nowExpression =
          parsePropositionalFormula.callRecursive(now) ?: return@DeepRecursiveFunction null

      if (!until.startsWith("U[") || !until.endsWith("]")) return@DeepRecursiveFunction null
      val interval =
          until.substring(2, until.length - 1).toInterval() ?: return@DeepRecursiveFunction null

      val laterExpression =
          parsePropositionalFormula.callRecursive(later) ?: return@DeepRecursiveFunction null

      Until(nowExpression, interval, laterExpression)
    }

private val parseFinally =
    DeepRecursiveFunction<BracketTree, Finally<String>?> { tree ->
      if (tree !is BracketedExpression || tree.size != 2) return@DeepRecursiveFunction null
      val (finally, proposition) = tree
      if (finally !is Word || proposition !is BracketedExpression) return@DeepRecursiveFunction null

      if (!finally.startsWith("◇[") || !finally.endsWith("]")) return@DeepRecursiveFunction null
      val interval =
          finally.substring(2, finally.length - 1).toInterval() ?: return@DeepRecursiveFunction null

      val propositionExpression =
          parsePropositionalFormula.callRecursive(proposition) ?: return@DeepRecursiveFunction null

      Finally(interval, propositionExpression)
    }

private val parseGlobal =
    DeepRecursiveFunction<BracketTree, Global<String>?> { tree ->
      if (tree !is BracketedExpression || tree.size != 2) return@DeepRecursiveFunction null
      val (finally, proposition) = tree
      if (finally !is Word || proposition !is BracketedExpression) return@DeepRecursiveFunction null

      if (!finally.startsWith("□[") || !finally.endsWith("]")) return@DeepRecursiveFunction null
      val interval =
          finally.substring(2, finally.length - 1).toInterval() ?: return@DeepRecursiveFunction null

      val propositionExpression =
          parsePropositionalFormula.callRecursive(proposition) ?: return@DeepRecursiveFunction null

      Global(interval, propositionExpression)
    }

fun String.toInterval(): ClosedRange<Duration>? {
  val components = split("..", limit = 2)
  if (components.size != 2) return null
  return try {
    Duration.parse(components[0])..Duration.parse(components[1])
  } catch (_: IllegalArgumentException) {
    null
  }
}

private val parsePropositionalFormula = DeepRecursiveFunction { doParsePropositionalFormula(it) }

private suspend fun DeepRecursiveScope<BracketTree, PropositionalFormula<String>?>
    .doParsePropositionalFormula(tree: BracketTree): PropositionalFormula<String>? =
    parseTrue.callRecursive(tree)
        ?: parseOutput.callRecursive(tree)
        ?: parseOr.callRecursive(tree)
        ?: parseNot.callRecursive(tree)

private val parseTrue =
    DeepRecursiveFunction<BracketTree, True<String>?> { tree ->
      if (tree !is BracketedExpression || tree.size != 1) return@DeepRecursiveFunction null
      val word = tree.single()
      if (word !is Word || word.value != "true") return@DeepRecursiveFunction null

      True()
    }

private val parseOutput =
    DeepRecursiveFunction<BracketTree, Output<String>?> { tree ->
      if (tree !is BracketedExpression || tree.size != 1) return@DeepRecursiveFunction null
      val word = tree.single()
      if (word !is Word || word.value == "true") return@DeepRecursiveFunction null

      Output(word.value)
    }

private val parseOr =
    DeepRecursiveFunction<BracketTree, Or<String>?> { tree ->
      if (tree !is BracketedExpression || tree.size != 3) return@DeepRecursiveFunction null
      val (left, or, right) = tree
      if (left !is BracketedExpression || or !is Word || right !is BracketedExpression)
          return@DeepRecursiveFunction null

      val leftExpression =
          parsePropositionalFormula.callRecursive(left) ?: return@DeepRecursiveFunction null

      if (or.value != "∧") return@DeepRecursiveFunction null

      val rightExpression =
          parsePropositionalFormula.callRecursive(right) ?: return@DeepRecursiveFunction null

      Or(leftExpression, rightExpression)
    }

private val parseNot =
    DeepRecursiveFunction<BracketTree, Not<String>?> { tree ->
      if (tree !is BracketedExpression || tree.size != 2) return@DeepRecursiveFunction null
      val (not, negated) = tree
      if (not !is Word || negated !is BracketedExpression) return@DeepRecursiveFunction null

      if (not.value != "¬") return@DeepRecursiveFunction null

      val negatedExpression =
          parsePropositionalFormula.callRecursive(negated) ?: return@DeepRecursiveFunction null

      Not(negatedExpression)
    }
