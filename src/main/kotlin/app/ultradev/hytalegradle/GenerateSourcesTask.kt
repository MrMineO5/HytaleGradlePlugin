package app.ultradev.hytalegradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.jvm.Jvm
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI
import java.nio.file.Files

abstract class GenerateSourcesTask : DefaultTask() {
    companion object {
        private val fernflowerUrl =
            "https://github.com/Vineflower/vineflower/releases/download/1.11.2/vineflower-1.11.2-slim.jar"
    }

    @get:InputDirectory
    abstract val cacheDir: DirectoryProperty

    init {
        group = "hytale"
        description = "Decompiles Hytale to a source jar to provide better IDE indexing"
    }

    @TaskAction
    fun decompileServer() {
        val javaExecutable = Jvm.current().javaExecutable

        val cacheDir = cacheDir.get().asFile.toPath()

        val fernflowerPath = cacheDir.resolve("vineflower.jar")

        if (!Files.exists(fernflowerPath)) {
            logger.lifecycle("Downloading Fernflower...")
            Files.copy(URI(fernflowerUrl).toURL().openStream(), fernflowerPath)
        }

        logger.lifecycle("Decompiling Hytale...")
        val cmd = mutableListOf(javaExecutable.absolutePath)
        cmd.addAll(
            listOf(
                "-jar", fernflowerPath.toAbsolutePath().toString(),
                "--only=com/hypixel",
                "HytaleServer.jar",
                "HytaleServer-sources.jar"
            )
        )

        val process = ProcessBuilder(cmd)
            .directory(cacheDir.toFile())
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
    }
}
