// SPDX-FileCopyrightText: 2025-2026 The Heat Pump Mining Authors, see AUTHORS.md
//
// SPDX-License-Identifier: Apache-2.0

package tools.aqua

import javax.inject.Inject
import kotlin.io.path.createDirectories
import kotlin.text.Charsets.UTF_8
import org.apache.commons.io.output.ByteArrayOutputStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations

@UntrackedTask(because = "Singularity inputs can not be captured")
abstract class SingularityBuildTask @Inject constructor(private val exec: ExecOperations) :
    DefaultTask() {
  @get:InputFile abstract val specification: RegularFileProperty

  @get:OutputFile abstract val container: RegularFileProperty

  @get:Input
  @get:Option(option = "--fakeroot", description = "Use fakeroot to perform the Singularity build")
  abstract val useFakeroot: Property<Boolean>

  init {
    useFakeroot.convention(false)
  }

  @get:Input
  @get:Option(option = "--sandbox", description = "Perform a sandbox Singularity build")
  abstract val buildSandbox: Property<Boolean>

  init {
    buildSandbox.convention(false)
  }

  @TaskAction
  fun build() {
    checkVersion()
    runBuild()
  }

  private fun checkVersion() {
    val output = ByteArrayOutputStream()
    exec
        .exec {
          commandLine("singularity", "--version")
          standardOutput = output
        }
        .run {
          rethrowFailure()
          assertNormalExitValue()
        }

    val version = output.toString(UTF_8).lines().filter { it.isNotEmpty() }

    when (version.size) {
      0 -> logger.warn("Singularity version is empty")
      1 -> logger.info("Singularity version: ${version.single()}")
      else -> logger.warn("Singularity version is oddly formed: ${version.joinToString(" / ")}")
    }
  }

  private fun runBuild() {
    container.get().asFile.toPath().parent.createDirectories()
    exec
        .exec {
          commandLine("singularity", "build", "--force")
          if (useFakeroot.get()) args("--fakeroot")
          if (buildSandbox.get()) args("--sandbox")
          args(container.get().asFile.absolutePath, specification.get().asFile.absolutePath)
        }
        .run {
          rethrowFailure()
          assertNormalExitValue()
        }
  }
}
