import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project
val kotlin_version: String by project
val ktorm_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "1.3.41"
}

group = "com.lgithub.lamba92"
version = "0.0.1"

repositories {
    mavenLocal()
    jcenter()
    maven("https://kotlin.bintray.com/ktor")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("me.liuwj.ktorm:ktorm-core:$ktorm_version")

    implementation(ktor("server-core"))
    implementation(ktor("jackson"))
    implementation(ktor("auth"))
    testImplementation(ktor("server-tests"))
}

fun DependencyHandler.ktor(module: String, version: String = ktor_version): Any =
    "io.ktor:ktor-$module:$version"

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}