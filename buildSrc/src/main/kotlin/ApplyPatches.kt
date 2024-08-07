package fr.nationsglory.forgegradledev

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import com.github.difflib.patch.PatchFailedException
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.IOException
import java.nio.file.Files

abstract class ApplyPatches : DefaultTask() {
    @get:InputDirectory
    abstract val sourceDirectory: DirectoryProperty

    @get:InputDirectory
    abstract val patchesDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val targetDirectory: DirectoryProperty

    @TaskAction
    fun doTask() {
        patchesDirectory.get().asFile.walk().filter { f -> f.isFile }.forEach { patchFile ->
            val relativePath = patchFile.toRelativeString(patchesDirectory.get().asFile).removeSuffix(".patch")

            val sourceFile = File(sourceDirectory.get().asFile, relativePath)
            val targetFile = File(targetDirectory.get().asFile, relativePath)

            val patchLines = Files.readAllLines(patchFile.toPath())
            val sourceLines = Files.readAllLines(sourceFile.toPath())

            try {
                val patch = UnifiedDiffUtils.parseUnifiedDiff(patchLines)

                val outputLines = DiffUtils.patch(sourceLines, patch)

                targetFile.parentFile.mkdirs()
                Files.write(targetFile.toPath(), outputLines)
                println("PATCH SUCCESS: ${targetFile.absolutePath}")
            } catch (e: PatchFailedException) {
                println("PATCH FAILED: ${patchFile.absolutePath}")
                println(e)
            }

        }
    }
}