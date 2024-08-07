
group = "fr.nationsglory"


plugins {
    kotlin("jvm") version "2.0.10"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.md-5:SpecialSource:1.6.1")
    implementation(kotlin("stdlib-jdk8"))
    implementation(gradleApi())
    implementation("io.github.java-diff-utils:java-diff-utils:4.12")
}
