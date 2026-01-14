# Hytale Gradle Plugin


## Features
- Automatically add local Hytale Server to classpath
- `runServer` task to install your plugin and run the server

## Usage
Add the repository to `settings.gradle.kts`
```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://mvn.ultradev.app/snapshots")
    }
}
```

Add the plugin to your `build.gradle.kts`
```kotlin
plugins {
    id("app.ultradev.hytalegradle") version "1.0-SNAPSHOT"
}
```
