@file:Suppress("unused", "PropertyName")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project
val kotlin_version: String by project
val ktorm_version: String by project
val logback_version: String by project
val kotlin_logging_version: String by project

plugins {
    kotlin("jvm") version "1.3.50"
    id("maven-publish")
}

group = "com.github.lamba92"
version = "0.0.1"

repositories {
    mavenLocal()
    jcenter()
    maven("https://kotlin.bintray.com/ktor")
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("ch.qos.logback:logback-classic:$logback_version")
    api(ktorm("core"))
    api(ktorm("jackson"))

    api(ktor("server-core"))
    api(ktor("jackson"))
    api(ktor("auth"))

    testImplementation(ktor("server-tests"))
    testImplementation(ktorm("support-sqlite"))
    testImplementation("com.github.lamba92:kresourceloader:1.1.1")
    testRuntime("org.xerial", "sqlite-jdbc", "3.28.0")
}

fun DependencyHandler.ktor(module: String, version: String = ktor_version): Any =
    "io.ktor:ktor-$module:$version"

fun DependencyHandler.ktorm(module: String, version: String = ktorm_version): Any =
    "me.liuwj.ktorm:ktorm-$module:$version"

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val sourcesJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles sources JAR"
    archiveClassifier.set("sources")
    from(sourceSets.getAt("main").allSource)
}

publishing {
    publications {
        create<MavenPublication>(name) {
            from(components["java"])
            artifact(sourcesJar)
        }
    }
}