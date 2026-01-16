package app.ultradev.hytalegradle

import org.gradle.api.logging.Logger
import org.gradle.internal.jvm.Jvm
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.sequences.forEach

object GenerateSources {
    val fernflowerUrl = "https://github.com/Vineflower/vineflower/releases/download/1.11.2/vineflower-1.11.2-slim.jar"

    private fun getFernflower(cacheDir: Path): Path {
        val targetPath = cacheDir.resolve("vineflower.jar")

        if (!Files.exists(targetPath)) {
            Files.copy(URI(fernflowerUrl).toURL().openStream(), targetPath)
        }

        return targetPath
    }

    fun generateSources(logger: Logger, cacheDir: Path, hytaleBaseDir: Path): Path {
        val libsDir = cacheDir.resolve("libs")
        Files.createDirectories(libsDir)

        val installedServer = hytaleBaseDir.resolve("Server${File.separator}HytaleServer.jar")
        val localCopy = libsDir.resolve("HytaleServer.jar")
        val outputJar = libsDir.resolve("HytaleServer-sources.jar")

        if (!Files.exists(localCopy) || !Files.exists(outputJar) || Files.mismatch(installedServer, localCopy) != -1L) {
            logger.lifecycle("New server version detected, generating sources...")

            Files.copy(installedServer, localCopy, StandardCopyOption.REPLACE_EXISTING)

            logger.lifecycle("Running Vineflower...")

            val javaExecutable = Jvm.current().javaExecutable
            val fernflower = getFernflower(cacheDir)

            val cmd = mutableListOf(javaExecutable.absolutePath)
            cmd.addAll(listOf(
                "-jar", fernflower.toAbsolutePath().toString(),
                "--only=com/hypixel",
                "HytaleServer.jar",
                "HytaleServer-sources.jar"
            ))

            val process = ProcessBuilder(cmd)
                .directory(libsDir.toFile())
                .start()

            val t = Thread {
                BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
                    lines.forEach { line ->
                        logger.lifecycle(line)
                    }
                }
            }
            t.isDaemon = true
            t.start()

            process.waitFor()

            t.join(1000)
        } else {
            logger.lifecycle("Using cached sources...")
        }

        return libsDir
    }
}