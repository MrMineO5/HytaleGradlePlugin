plugins {
    kotlin("jvm") version "2.2.20"
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "2.0.0"
}

group = "app.ultradev"
version = "2.0.2"

repositories {
    mavenCentral()
    maven("https://central.sonatype.com/repository/maven-snapshots/")
}

dependencies {
    implementation("net.java.dev.jna:jna-platform:5.14.0")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    website.set("https://github.com/MrMineO5/HytaleGradlePlugin")
    vcsUrl.set("https://github.com/MrMineO5/HytaleGradlePlugin")
    plugins {
        create("hytalegradle") {
            id = "app.ultradev.hytalegradle"
            implementationClass = "app.ultradev.hytalegradle.HytaleGradlePlugin"
            displayName = "Hytale Gradle Plugin"
            description = "Gradle plugin to simplify Hytale plugin development"
            tags.set(listOf("hytale"))
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

java {
    withSourcesJar()
    // optional, but nice for Maven consumers
    withJavadocJar()
}

publishing {
    repositories {
        maven {
            name = "ultradevRepository"
            url = uri("https://mvn.ultradev.app/snapshots")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("kotlinJvm") {
            from(components["java"])
        }
    }
}

