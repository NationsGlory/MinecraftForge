package fr.nationsglory.forgegradledev

import com.google.common.collect.Lists
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

abstract class MergeJars : DefaultTask() {
    @get:InputFile
    abstract val clientJar: RegularFileProperty

    @get:InputFile
    abstract val serverJar: RegularFileProperty

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
            val clientZip = ZipFile(clientJar.get().asFile)
            val serverZip = ZipFile(serverJar.get().asFile)

            val addedEntries = Lists.newArrayList<String>()

            clientZip.entries().toList()
                .filter { entry -> !isExcluded(entry) && entry.name.endsWith(".class") }
                .forEach { clientEntry ->
                    clientZip.getInputStream(clientEntry).use { inputStream ->
                        val clientClassNode = ClassNode()
                        var serverClassNode: ClassNode? = null

                        val reader = ClassReader(inputStream)
                        reader.accept(clientClassNode, 0)

                        val serverEntry = serverZip.getEntry(clientEntry.name)
                        if (serverEntry != null) {
                            serverZip.getInputStream(serverEntry).use { inputStream ->
                                serverClassNode = ClassNode()
                                ClassReader(inputStream).accept(serverClassNode, 0)
                            }
                        }

                        val newEntry = ZipEntry(clientEntry.name)
                        val finalClassNode = processClass(clientClassNode, serverClassNode) ?: return

                        val writer = ClassWriter(reader, ClassWriter.COMPUTE_MAXS)
                        finalClassNode.accept(writer)
                        out.putNextEntry(newEntry)
                        out.write(writer.toByteArray())
                        out.closeEntry()

                        addedEntries.add(newEntry.name)
                    }
                }

            serverZip.entries().toList()
                .filter { entry -> !isExcluded(entry) && !addedEntries.contains(entry.name) && entry.name.endsWith(".class") }
                .forEach { zipEntry ->
                    serverZip.getInputStream(zipEntry).use { inputStream ->
                        val serverClassNode = ClassNode()

                        val reader = ClassReader(inputStream)
                        reader.accept(serverClassNode, 0)

                        val finalServerClass = processClass(null, serverClassNode) ?: return

                        val writer = ClassWriter(reader, ClassWriter.COMPUTE_MAXS)
                        finalServerClass.accept(writer)
                        out.putNextEntry(ZipEntry(zipEntry.name))
                        out.write(writer.toByteArray())
                        out.closeEntry()
                    }
                }
        }
    }

    private fun getSideAnn(client: Boolean): AnnotationNode {
        val annotationNode = AnnotationNode("Lcpw/mods/fml/relauncher/SideOnly;")
        annotationNode.values = Lists.newArrayList()
        annotationNode.values.add("value")
        annotationNode.values.add(arrayOf("Lcpw/mods/fml/relauncher/Side;", if (client) "CLIENT" else "SERVER"))
        return annotationNode
    }

    private fun processClass(classNodeClient: ClassNode?, classNodeServer: ClassNode?): ClassNode? {
        if (classNodeClient == null && classNodeServer != null) {
            if (classNodeServer.visibleAnnotations == null)
                classNodeServer.visibleAnnotations = Lists.newArrayList()

            classNodeServer.visibleAnnotations.add(getSideAnn(false))
            return classNodeServer
        } else if (classNodeServer == null && classNodeClient != null) {
            if (classNodeClient.visibleAnnotations == null)
                classNodeClient.visibleAnnotations = Lists.newArrayList()

            classNodeClient.visibleAnnotations.add(getSideAnn(true))
            return classNodeClient
        } else if (classNodeClient != null && classNodeServer != null) {
            mergeFields(classNodeClient, classNodeServer)
            mergeMethods(classNodeClient, classNodeServer)
            return classNodeClient
        }

        return null
    }

    private fun mergeFields(classNodeClient: ClassNode, classNodeServer: ClassNode) {
        val clientFields = HashMap<String, FieldNode>()
        val serverFields = HashMap<String, FieldNode>()

        classNodeClient.fields.forEach { fieldNode ->
            clientFields[fieldNode.name + " " + fieldNode.desc] = fieldNode
        }

        classNodeServer.fields.forEach { fieldNode ->
            serverFields[fieldNode.name + " " + fieldNode.desc] = fieldNode
        }

        clientFields.filter { entry -> !serverFields.containsKey(entry.key) }
            .forEach { entry ->
                if (entry.value.visibleAnnotations == null)
                    entry.value.visibleAnnotations = Lists.newArrayList()

                entry.value.visibleAnnotations.add(getSideAnn(true))
            }

        serverFields.forEach { entry ->
            if (!clientFields.containsKey(entry.key)) {
                if (entry.value.visibleAnnotations == null)
                    entry.value.visibleAnnotations = Lists.newArrayList()

                entry.value.visibleAnnotations.add(getSideAnn(false))
                classNodeClient.fields.add(entry.value)
            }
        }
    }

    private fun mergeMethods(classNodeClient: ClassNode, classNodeServer: ClassNode) {
        val clientMethods = HashMap<String, MethodNode>()
        val serverMethods = HashMap<String, MethodNode>()

        classNodeClient.methods.forEach { methodNode ->
            clientMethods[methodNode.name + " " + methodNode.desc] = methodNode
        }

        classNodeServer.methods.forEach { methodNode ->
            serverMethods[methodNode.name + " " + methodNode.desc] = methodNode
        }

        clientMethods.filter { entry -> !serverMethods.containsKey(entry.key) }
            .forEach { entry ->
                if (entry.value.visibleAnnotations == null)
                    entry.value.visibleAnnotations = Lists.newArrayList()

                entry.value.visibleAnnotations.add(getSideAnn(true))
            }

        serverMethods.forEach { entry ->
            if (!clientMethods.containsKey(entry.key)) {
                if (entry.value.visibleAnnotations == null)
                    entry.value.visibleAnnotations = Lists.newArrayList()

                entry.value.visibleAnnotations.add(getSideAnn(false))
                classNodeClient.methods.add(entry.value)
            }
        }
    }
}