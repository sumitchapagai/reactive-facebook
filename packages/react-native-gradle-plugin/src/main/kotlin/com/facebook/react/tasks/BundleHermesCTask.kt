/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.react.tasks

import com.facebook.react.utils.detectOSAwareHermesCommand
import com.facebook.react.utils.moveTo
import com.facebook.react.utils.windowsAwareCommandLine
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

abstract class BundleHermesCTask : DefaultTask() {

  init {
    group = "react"
  }

  @get:Internal abstract val root: DirectoryProperty

  @get:InputFiles
  val sources: ConfigurableFileTree =
      project.fileTree(root) {
        it.include("**/*.js")
        it.include("**/*.jsx")
        it.include("**/*.ts")
        it.include("**/*.tsx")
        it.exclude("**/android/**/*")
        it.exclude("**/ios/**/*")
        it.exclude("**/build/**/*")
        it.exclude("**/node_modules/**/*")
      }

  @get:Input abstract val nodeExecutableAndArgs: ListProperty<String>

  @get:Input abstract val cliPath: Property<String>

  @get:Input abstract val composeSourceMapsPath: Property<String>

  @get:Input abstract val bundleCommand: Property<String>

  @get:InputFile abstract val entryFile: RegularFileProperty

  @get:InputFile @get:Optional abstract val bundleConfig: RegularFileProperty

  @get:Input abstract val bundleAssetName: Property<String>

  @get:Input abstract val minifyEnabled: Property<Boolean>

  @get:Input abstract val hermesEnabled: Property<Boolean>

  @get:Input abstract val devEnabled: Property<Boolean>

  @get:Input abstract val extraPackagerArgs: ListProperty<String>

  @get:Input abstract val hermesCommand: Property<String>

  @get:Input abstract val hermesFlags: ListProperty<String>

  @get:OutputDirectory abstract val jsBundleDir: DirectoryProperty

  @get:OutputDirectory abstract val resourcesDir: DirectoryProperty

  @get:OutputDirectory abstract val jsIntermediateSourceMapsDir: RegularFileProperty

  @get:OutputDirectory abstract val jsSourceMapsDir: DirectoryProperty

  @TaskAction
  fun run() {
    jsBundleDir.get().asFile.mkdirs()
    resourcesDir.get().asFile.mkdirs()
    jsIntermediateSourceMapsDir.get().asFile.mkdirs()
    jsSourceMapsDir.get().asFile.mkdirs()
    val bundleAssetFilename = bundleAssetName.get()

    val bundleFile = File(jsBundleDir.get().asFile, bundleAssetFilename)
    val packagerSourceMap = resolvePackagerSourceMapFile(bundleAssetFilename)

    val bundleCommand = getBundleCommand(bundleFile, packagerSourceMap)
    runCommand(bundleCommand)

    if (hermesEnabled.get()) {
      val detectedHermesCommand = detectOSAwareHermesCommand(root.get().asFile, hermesCommand.get())
      val bytecodeFile = File("${bundleFile}.hbc")
      val outputSourceMap = resolveOutputSourceMap(bundleAssetFilename)
      val compilerSourceMap = resolveCompilerSourceMap(bundleAssetFilename)

      val hermesCommand = getHermescCommand(detectedHermesCommand, bytecodeFile, bundleFile)
      runCommand(hermesCommand)
      bytecodeFile.moveTo(bundleFile)

      if (hermesFlags.get().contains("-output-source-map")) {
        val hermesTempSourceMapFile = File("$bytecodeFile.map")
        hermesTempSourceMapFile.moveTo(compilerSourceMap)
        val composeSourceMapsCommand =
            getComposeSourceMapsCommand(packagerSourceMap, compilerSourceMap, outputSourceMap)
        runCommand(composeSourceMapsCommand)
      }
    }
  }

  internal fun resolvePackagerSourceMapFile(bundleAssetName: String) =
      if (hermesEnabled.get()) {
        File(jsIntermediateSourceMapsDir.get().asFile, "$bundleAssetName.packager.map")
      } else {
        resolveOutputSourceMap(bundleAssetName)
      }

  internal fun resolveOutputSourceMap(bundleAssetName: String) =
      File(jsSourceMapsDir.get().asFile, "$bundleAssetName.map")

  internal fun resolveCompilerSourceMap(bundleAssetName: String) =
      File(jsIntermediateSourceMapsDir.get().asFile, "$bundleAssetName.compiler.map")

  private fun runCommand(command: List<Any>) {
    project.exec {
      it.workingDir(root.get().asFile)
      it.commandLine(command)
    }
  }

  internal fun getBundleCommand(bundleFile: File, sourceMapFile: File): List<Any> =
      windowsAwareCommandLine(
          buildList {
            addAll(nodeExecutableAndArgs.get())
            add(cliPath.get())
            add(bundleCommand.get())
            add("--platform")
            add("android")
            add("--dev")
            add(devEnabled.get().toString())
            add("--reset-cache")
            add("--entry-file")
            add(entryFile.get().asFile.toString())
            add("--bundle-output")
            add(bundleFile.toString())
            add("--assets-dest")
            add(resourcesDir.get().asFile.toString())
            add("--sourcemap-output")
            add(sourceMapFile.toString())
            if (bundleConfig.isPresent) {
              add("--config")
              add(bundleConfig.get().asFile.absolutePath)
            }
            add("--minify")
            add(minifyEnabled.get().toString())
            addAll(extraPackagerArgs.get())
            add("--verbose")
          })

  internal fun getHermescCommand(
      hermesCommand: String,
      bytecodeFile: File,
      bundleFile: File
  ): List<Any> =
      windowsAwareCommandLine(
          hermesCommand,
          "-emit-binary",
          "-out",
          bytecodeFile.absolutePath,
          bundleFile.absolutePath,
          *hermesFlags.get().toTypedArray())

  internal fun getComposeSourceMapsCommand(
      packagerSourceMap: File,
      compilerSourceMap: File,
      outputSourceMap: File
  ): List<Any> =
      windowsAwareCommandLine(
          *nodeExecutableAndArgs.get().toTypedArray(),
          composeSourceMapsPath.get(),
          packagerSourceMap.toString(),
          compilerSourceMap.toString(),
          "-o",
          outputSourceMap.toString())
}
