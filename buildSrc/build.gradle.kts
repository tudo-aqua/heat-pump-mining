// SPDX-FileCopyrightText: 2025-2025 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

plugins { `kotlin-dsl-base` }

repositories {
  mavenCentral()
  mavenLocal()
}

dependencies { implementation(libs.rereso) }
