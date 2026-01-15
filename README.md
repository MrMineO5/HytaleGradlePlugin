# Hytale Gradle Plugin
<img src="https://mvn.ultradev.app/api/badge/latest/snapshots/app/ultradev/HytaleGradlePlugin?color=40c14a&name=Version" />

## Features
- Automatically add local Hytale Server to classpath
- `runServer` task to install your plugin and run the server
- Automatically run `/auth login device` to authenticate (You still need to click the link in the console)
Recommended: Run `/auth persistence Encrypted` to avoid needing to re-authenticate every time

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

Add the plugin to your `build.gradle.kts` (check the latest version in the badge below the title)
```kotlin
plugins {
    id("app.ultradev.hytalegradle") version "1.1.0"
}

hytale {
    allowOp.set(true)
}
```
