# Hytale Gradle Plugin
<a href="https://mvn.ultradev.app/#/snapshots/app/ultradev/HytaleGradlePlugin">
  <img src="https://mvn.ultradev.app/api/badge/latest/snapshots/app/ultradev/HytaleGradlePlugin?color=40c14a&name=Version" />
</a>

## Features
- Automatically add local Hytale Server to classpath
- `runServer` task to install your plugin and run the server
- Generate decompiled sources to allow browsing server code in IDEs
- Automatically update selected values in `manifest.json`

## Installation
Add the plugin to your `build.gradle.kts` (check the latest version in the badge below the title)

> [!WARNING]
> If you use shadowJar to bundle dependencies make sure the hytalegradle plugin is applied **AFTER** shadowJar as in the example below

```kotlin
plugins {
    id("com.gradleup.shadow") version "9.3.1"
    id("app.ultradev.hytalegradle") version "2.0.2"
}

hytale {
    // Add `--allow-op` to server args (allows you to run `/op self` in-game)
    allowOp.set(true)
    
    // Set the patchline to use, currently there are "release" and "pre-release"
    patchline.set("pre-release")
    
    // Load mods from the local Hytale installation
    includeLocalMods.set(true)

    // Replace the version in the manifest with the project version
    manifest {
        version.set(project.version.toString())
    }
}
```

### Hot Reload
Running the `runServer` task in debug mode with your IDE will usually allow you to hot reload classes.

For better hot reloading, use the JetBrains Runtime (JBR) and add the following jvm argument
```kotlin
tasks.runServer {
    jvmArgs = jvmArgs + "-XX:+AllowEnhancedClassRedefinition"
}
```

## Gradle Tasks

`./gradlew generateSources` generates a decompiled jar of the Hytale server sources to improve IDE indexing.

`./gradlew runServer` runs the Hytale server with your plugin installed.

`./gradlew installPlugin` copies your plugin jar to the server's mods folder but does not start the server, useful if you want to reload your plugin without restarting the server.

`./gradlew updateManifest` updates the manifest with values from the `hytale` extension, automatically runs during build
