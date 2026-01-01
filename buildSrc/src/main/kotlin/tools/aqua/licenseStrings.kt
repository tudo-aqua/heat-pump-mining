// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua

import java.time.Year

data class FileCopyright(val fromYear: Year, val name: String, val contact: String? = null) {
  override fun toString(): String =
      "$fromYear-${Year.now()} $name${contact?.let { " <$it>" } ?: ""}"
}

enum class License(val spdxName: String) {
  APACHE_2_0("Apache-2.0"),
  CC_BY_4_0("CC-BY-4.0"),
  ;

  override fun toString(): String = spdxName
}

data class ReuseHeader(val license: License, val copyrights: List<FileCopyright>) {
  init {
    require(copyrights.isNotEmpty())
  }

  constructor(
      license: License,
      vararg copyrights: FileCopyright,
  ) : this(license, copyrights.toList())

  // REUSE-IgnoreStart
  override fun toString(): String =
      """
    ${copyrights.joinToString("\n") { "SPDX-FileCopyrightText: $it" }}
    
    SPDX-License-Identifier: $license"""
          .trimIndent()
  // REUSE-IgnoreEnd
}
