# Hytale Gradle Plugin
<a href="https://mvn.ultradev.app/#/snapshots/app/ultradev/HytaleGradlePlugin">
  <img src="https://mvn.ultradev.app/api/badge/latest/snapshots/app/ultradev/HytaleGradlePlugin?color=40c14a&name=Version" />
</a>

## Features
- Automatically add local Hytale Server to classpath
- `runServer` task to install your plugin and run the server
- Generate decompiled sources to allow browsing server code in IDEs

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
    id("app.ultradev.hytalegradle") version "1.4.0"
}

hytale {
    // Add `--allow-op` to server args (allows you to run `/op self` in-game)
    allowOp.set(true)
    
    // Set the patchline to use, currently there are "release" and "pre-release"
    patchline.set("pre-release")
}
```

## Browsing Hytale Source
If you need IDE indexing of the Hytale server code, e.g. to find usages inside the server, you can generate the sources using `./gradlew generateSources`
