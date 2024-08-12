import de.undercouch.gradle.tasks.download.Download
import fr.nationsglory.forgegradledev.ApplyPatches
import fr.nationsglory.forgegradledev.DecompJar
import fr.nationsglory.forgegradledev.MergeJars
import fr.nationsglory.forgegradledev.RemapJar

group = "fr.nationsglory"
version = "1.0-SNAPSHOT"

plugins {
    id("java")
    id("java-gradle-plugin")
    id("de.undercouch.download").version("5.6.0")
}

repositories {
    maven {
        url = uri(path = "https://jitpack.io")
    }
    maven {
        url = uri(path = "https://maven.nationsglory.fr/releases/")
    }
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("com.google.code.gson:gson:2.10")
    implementation("argo:argo:2.25_fixed")
    implementation("org.lwjgl.lwjgl:lwjgl:2.9.0")
    implementation("org.lwjgl.lwjgl:lwjgl-platform:2.9.0")
    implementation("org.lwjgl.lwjgl:lwjgl_util:2.9.0")
    implementation("com.paulscode:soundsystem:20120107")
    implementation("com.paulscode:libraryjavasound:20101123")
    implementation("com.paulscode:librarylwjglopenal:20100824")
    implementation("com.paulscode:codecjorbis:20101023")
    implementation("com.paulscode:codecwav:20101023")
    implementation("commons-io:commons-io:2.4")
    implementation("org.apache.commons:commons-lang3:3.1")
    implementation("com.google.guava:guava:14.0.1")
    implementation("net.java.jinput:jinput:2.0.5")
    implementation("net.java.jinput:jinput-platform:2.0.5")
    implementation("net.sf.jopt-simple:jopt-simple:4.5")
    implementation("net.java.jutils:jutils:1.0.0\"")
    implementation("net.minecraft:launchwrapper:1.8")
    implementation("lzma:lzma:0.0.1")
    implementation("org.bouncycastle:bcprov-jdk15on:1.47")
    implementation("org.ow2.asm:asm-commons:6.2.1")
    implementation("org.ow2.asm:asm-util:6.2.1")
}

sourceSets.create("client") {
    java.srcDir("src/client/java")
    resources.srcDir("src/client/resources")
}

sourceSets.create("common") {
    java.srcDir("src/common/java")
    resources.srcDir("src/common/resources")
}

sourceSets.create("server") {
    java.srcDir("src/server/java")
    resources.srcDir("src/server/resources")
}

tasks.register<Download>("downloadClient") {
    src("https://launcher.mojang.com/v1/objects/1703704407101cf72bd88e68579e3696ce733ecd/client.jar")
    dest(layout.buildDirectory.dir("tmp/"))
}

tasks.register<Download>("downloadServer") {
    src("https://launcher.mojang.com/v1/objects/050f93c1f3fe9e2052398f7bd6aca10c63d64a87/server.jar")
    dest(layout.buildDirectory.dir("tmp/"))
}

tasks.register<Download>("downloadFernflower") {
    src("https://maven.nationsglory.fr/files/fernflower-382.jar")
    dest(layout.buildDirectory.dir("tmp/"))
}

tasks.register<Download>("downloadMcpConfigs") {
    src("https://maven.nationsglory.fr/files/mcp-configs.zip")
    dest(layout.buildDirectory.dir("tmp/"))
}

tasks.register<Copy>("extractMcpConfig") {
    from(zipTree(layout.buildDirectory.file("tmp/mcp-configs.zip")))
    into(layout.buildDirectory.dir("unpacked/"))
    dependsOn("downloadMcpConfigs")
}

tasks.register<RemapJar>("deobfMerged") {
    dependsOn("mergeClientServer", "extractMcpConfig")
    inputJar.set(project.layout.buildDirectory.file("tmp/merged.jar"))
    outputFile.set(project.layout.buildDirectory.file("tmp/merged-deobf.jar"))
    mappings.set(project.layout.buildDirectory.file("unpacked/conf/packaged.srg"))
    accessTransformers.add(project.layout.projectDirectory.file("src/common/resources/fml_at.cfg"))
    accessTransformers.add(project.layout.projectDirectory.file("src/common/resources/forge_at.cfg"))
}

tasks.register<DecompJar>("decompMerged") {
    dependsOn("downloadFernflower", "deobfMerged")
    inputJar.set(project.layout.buildDirectory.file("tmp/merged-deobf.jar"))
    outputDir.set(project.layout.buildDirectory.dir("unpacked/"))
    fernflowerJar.set(project.layout.buildDirectory.file("tmp/fernflower-382.jar"))
}

tasks.register<MergeJars>("mergeClientServer") {
    dependsOn("downloadClient", "downloadServer")
    outputFile.set(project.layout.buildDirectory.file("tmp/merged.jar"))
    serverJar.set(project.layout.buildDirectory.file("tmp/server.jar"))
    clientJar.set(project.layout.buildDirectory.file("tmp/client.jar"))
    excludes.addAll("argo/", "com/google/", "org/")
}

tasks.register<Copy>("extractSources") {
    from(zipTree(layout.buildDirectory.file("unpacked/merged-deobf.jar")))
    into(project.layout.buildDirectory.file("tmp/minecraft/"))
    dependsOn("decompMerged")
}

tasks.register<ApplyPatches>("applyForgePatches") {
    dependsOn("extractSources")
    sourceDirectory.set(project.layout.buildDirectory.dir("tmp/minecraft"))
    targetDirectory.set(project.layout.buildDirectory.dir("tmp/minecraft_patched"))
    patchesDirectory.set(project.layout.projectDirectory.dir("patches/fml/"))
}