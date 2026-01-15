# Hytale Gradle Plugin
<a href="https://mvn.ultradev.app/#/snapshots/app/ultradev/HytaleGradlePlugin">
  <img src="https://mvn.ultradev.app/api/badge/latest/snapshots/app/ultradev/HytaleGradlePlugin?color=40c14a&name=Version" />
</a>

## Features
- Automatically add local Hytale Server to classpath
- `runServer` task to install your plugin and run the server
- Automatically run `/auth login device` to authenticate (You still need to click the link in the console)
Recommended: Run `/auth persistence Encrypted` to avoid needing to re-authenticate every time
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
    id("app.ultradev.hytalegradle") version "1.3.0"
}

hytale {
    allowOp.set(true)        // Add `--allow-op` to server args (allows you to run `/op self` in-game)
    attachSources.set(true)  // Decompile the hytale server and attach as sources to allow browsing the code in IDEs
}
```
