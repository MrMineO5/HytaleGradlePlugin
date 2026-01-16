package app.ultradev.hytalegradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.internal.jvm.Jvm
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

abstract class RunServerTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceJar: RegularFileProperty

    @get:OutputDirectory
    abstract val runDir: DirectoryProperty

    @get:InputDirectory
    abstract val basePath: DirectoryProperty

    @get:Input
    abstract val jvmArgs: ListProperty<String>

    @get:Input
    abstract val allowOp: Property<Boolean>

    init {
        group = "hytale"
        description = "Builds plugin, installs it into run/plugins, and runs the Hytale server"
        jvmArgs.convention(listOf("-Xms4G", "-Xmx8G"))
    }

    @TaskAction
    fun runServer() {
        // install plugin jar
        val jarFile = sourceJar.get().asFile
        val runDirectory = runDir.get().asFile
        val pluginsDir = File(runDirectory, "mods")
        pluginsDir.mkdirs()
        jarFile.copyTo(File(pluginsDir, jarFile.name), overwrite = true)

        // resolve server + assets
        val base = basePath.get().asFile
        val serverJar = File(base, "Server/HytaleServer.jar")
        val assetsZip = File(base, "Assets.zip")

        if (!serverJar.exists()) error("Hytale server JAR not found: ${serverJar.absolutePath}")
        if (!assetsZip.exists()) error("Assets.zip not found: ${assetsZip.absolutePath}")

        val javaExecutable = Jvm.current().javaExecutable

        val cmd = mutableListOf(javaExecutable.absolutePath)
        cmd.addAll(jvmArgs.get())
        cmd.addAll(
            listOf(
                "-jar", serverJar.absolutePath,
                "--assets", assetsZip.absolutePath
            )
        )

        if (allowOp.get()) {
            cmd.add("--allow-op")
        }

        logger.lifecycle("Starting Hytale server:")
        logger.lifecycle(cmd.joinToString(" "))

        val process = ProcessBuilder(cmd)
            .directory(runDirectory)
            .start()

        val outThread = pump(process.inputStream, isErr = false, process)
        val errThread = pump(process.errorStream, isErr = true, process)

        try {
            val exitCode = process.waitFor()
            outThread.join(1000)
            errThread.join(1000)
            if (exitCode != 0) {
                throw RuntimeException("Server exited with code $exitCode")
            }
        } catch (ie: InterruptedException) {
            // Task cancelled (Ctrl+C, stop button in IDE, etc.)
            logger.lifecycle("Build cancelled. Stopping server...")
            killProcessTree(process)
            Thread.currentThread().interrupt()
        } finally {
            // Ensure no zombie processes if something else throws
            if (process.isAlive) killProcessTree(process)
        }
    }


    private fun pump(stream: java.io.InputStream, isErr: Boolean, process: Process): Thread {
        val t = Thread {
            BufferedReader(InputStreamReader(stream)).useLines { lines ->
                lines.forEach { line ->
                    if (isErr) logger.error(line) else logger.lifecycle(line)

                    if (line.contains("Use /auth login to authenticate.")) {
                        logger.lifecycle("Attempting to run auth login...")

                        Thread {
                            "/auth login device\n".forEach {
                                process.outputStream.write(it.code)
                                process.outputStream.flush()
                            }
                        }.start()
                    }
                }
            }
        }
        t.isDaemon = true
        t.start()
        return t
    }

    private fun killProcessTree(process: Process) {
        val handle = process.toHandle()

        // First ask nicely
        handle.descendants().forEach { it.destroy() }
        handle.destroy()

        // Give it a moment
        if (!process.waitFor(3, TimeUnit.SECONDS)) {
            // Then force kill
            handle.descendants().forEach { it.destroyForcibly() }
            handle.destroyForcibly()
            process.waitFor(3, TimeUnit.SECONDS)
        }
    }
}
