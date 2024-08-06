package fr.nationsglory.forgegradledev

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.OutputDirectory

abstract class DecompJar : JavaExec() {
    @get:InputFile
    abstract val inputJar: RegularFileProperty

    @get:InputFile
    abstract val fernflowerJar: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty


    init {
        this.mainClass.set("org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler")
    }

    override fun exec() {
        this.classpath(fernflowerJar.get().asFile)
        this.args(
            "-din=0",
            "-rbr=0",
            "-dgs=1",
            "-asc=1",
            "-log=ERROR",
            inputJar.get().asFile.absolutePath,
            outputDir.get().asFile.absolutePath
        )
        this.workingDir(project.layout.buildDirectory.dir("tmp/"))
        super.exec()
    }
}