package fr.nationsglory.forgegradledev

import net.md_5.specialsource.*
import net.md_5.specialsource.provider.JarProvider
import net.md_5.specialsource.provider.JointProvider
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
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
    abstract val accessTransformers: ListProperty<RegularFile>

    @TaskAction
    fun doTask() {
        val jarMapping = JarMapping()
        jarMapping.loadMappings(mappings.get().asFile)

        val accessMap = AccessMap()
        accessTransformers.get().forEach { file -> accessMap.loadAccessTransformer(file.asFile) }

        val srgPrecessor = RemapperPreprocessor(null, jarMapping, accessMap)

        val jarRemapper = JarRemapper(srgPrecessor, jarMapping)

        val input: Jar = Jar.init(inputJar.get().asFile)

        val inheritanceProviders = JointProvider()
        inheritanceProviders.add(JarProvider(input))
        jarMapping.setFallbackInheritanceProvider(inheritanceProviders)

        jarRemapper.remapJar(input, outputFile.get().asFile)
    }
}