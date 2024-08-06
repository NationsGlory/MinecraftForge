import de.undercouch.gradle.tasks.download.Download
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
    mappings.set(project.layout.buildDirectory.file("unpacked/conf/notch-mcp.srg"))
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
    inputJars.add(project.layout.buildDirectory.file("tmp/server.jar"))
    inputJars.add(project.layout.buildDirectory.file("tmp/client.jar"))
    excludes.addAll("argo/", "com/google/", "org/")
}

tasks.register<Copy>("extractSources") {
    from(zipTree(layout.buildDirectory.file("unpacked/merged-deobf.jar")))
    into(project.layout.buildDirectory.file("tmp/minecraft/"))
    dependsOn("deobfMerged")
}

