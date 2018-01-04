import org.jetbrains.kotlin.gradle.dsl.Coroutines

plugins {
    application
    kotlin("jvm") version "1.2.10"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClassName = "com.netopyr.reduxfx.examples.helloworld.HelloWorld"
}

kotlin {
    experimental.coroutines = Coroutines.ENABLE
}

repositories {
    jcenter()
}

dependencies {
    compile(kotlin("stdlib"))
    compile("io.vavr:vavr-kotlin:0.9.2")
    compile("no.tornado:tornadofx:1.7.11")
    compile("org.jetbrains.kotlinx:kotlinx-html-jvm:0.6.6")
    compile("io.github.microutils:kotlin-logging:1.4.9")
    compile("org.slf4j:slf4j-simple:1.7.25")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.21")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:0.21")

    testCompile("junit:junit:4.12")
}