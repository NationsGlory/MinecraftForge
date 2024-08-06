package fr.nationsglory.forgegradledev

import net.md_5.specialsource.Jar
import net.md_5.specialsource.JarMapping
import net.md_5.specialsource.JarRemapper
import net.md_5.specialsource.RemapperPreprocessor
import net.md_5.specialsource.provider.JarProvider
import net.md_5.specialsource.provider.JointProvider
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

abstract class RemapJar : DefaultTask() {
    @get:InputFile
    abstract val inputJar: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:InputFile
    abstract val mappings: RegularFileProperty

    @get:InputFiles
    @get:Optional
    val accessTransformers: FileCollection? = null

    @TaskAction
    fun doTask() {
        val jarMapping = JarMapping()
        jarMapping.loadMappings(mappings.get().asFile)

        val srgPrecessor = RemapperPreprocessor(null, jarMapping, null)

        val jarRemapper = JarRemapper(srgPrecessor, jarMapping)

        val input: Jar = Jar.init(inputJar.get().asFile)

        val inheritanceProviders = JointProvider()
        inheritanceProviders.add(JarProvider(input))
        jarMapping.setFallbackInheritanceProvider(inheritanceProviders)

        jarRemapper.remapJar(input, outputFile.get().asFile)
    }
}