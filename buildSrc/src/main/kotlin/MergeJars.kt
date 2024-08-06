package fr.nationsglory.forgegradledev

import com.google.common.collect.Lists
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

abstract class MergeJars : DefaultTask() {
    @get:InputFiles
    abstract val inputJars: ListProperty<RegularFile>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Input
    abstract val excludes: ListProperty<String>

    private fun isExcluded(entry: ZipEntry): Boolean {
        return excludes.isPresent && excludes.get().any { exclude -> entry.name.startsWith(exclude) }
    }

    @TaskAction
    fun doTask() {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile.get().asFile))).use { out ->
            val entriesInserted = Lists.newArrayList<String>()

            inputJars.get().forEach { jar ->
                ZipFile(jar.asFile).use { zipIn ->
                    zipIn.entries().asSequence()
                        .filter { entry -> !entriesInserted.contains(entry.name) && !isExcluded(entry) }
                        .forEach { entry ->
                            zipIn.getInputStream(entry).use { inputStream ->
                                val newEntry = ZipEntry(entry.name)
                                out.putNextEntry(newEntry)
                                inputStream.copyTo(out)
                                entriesInserted.add(entry.name)
                            }
                        }
                }
            }
        }
    }
}