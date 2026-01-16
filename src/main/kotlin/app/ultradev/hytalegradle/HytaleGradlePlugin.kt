package app.ultradev.hytalegradle

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.tasks.Jar
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path
import kotlin.io.path.exists

class HytaleGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val ext = project.extensions.create("hytale", HytaleExtension::class.java)

        ext.patchline.convention("release")
        ext.hytaleHome.convention(project.layout.dir(project.provider { detectHytaleHome().toFile() }))
        ext.serverJar.convention(project.layout.file(project.provider {
            getHytaleBasePath(
                ext.hytaleHome.get().asFile.toPath(),
                ext.patchline.get()
            ).resolve("Server/HytaleServer.jar").toFile()
        }))
        ext.assetsZip.convention(project.layout.file(project.provider {
            getHytaleBasePath(
                ext.hytaleHome.get().asFile.toPath(),
                ext.patchline.get()
            ).resolve("Assets.zip").toFile()
        }))
        ext.allowOp.convention(false)
        ext.runDirectory.convention(project.layout.projectDirectory.dir("run"))
        ext.includeLocalMods.convention(false)

        val cacheDir = project.layout.buildDirectory.dir("hytale")

        project.repositories.flatDir {
            it.name = "hytaleGenerated"
            it.dir(cacheDir)
        }

        project.dependencies.add("compileOnly", mapOf("name" to "HytaleServer"))

        project.afterEvaluate {
            val installedServer = ext.serverJar.get().asFile.toPath()
            if (!Files.exists(installedServer)) return@afterEvaluate
            val cacheDir = cacheDir.get().asFile.toPath()
            val localCopy = cacheDir.resolve("HytaleServer.jar")

            if (Files.exists(localCopy) && Files.mismatch(
                    installedServer,
                    localCopy
                ) == -1L
            ) return@afterEvaluate // no need to copy

            Files.createDirectories(cacheDir)
            Files.copy(installedServer, localCopy, StandardCopyOption.REPLACE_EXISTING)
            Files.deleteIfExists(cacheDir.resolve("HytaleServer-sources.jar"))
        }

        project.tasks.named("clean", Delete::class.java) {
            it.delete(ext.runDirectory)
        }

        val archiveTaskName =
            if (project.tasks.names.contains("shadowJar")) "shadowJar" else "jar"

        val jarTask = project.tasks.named(archiveTaskName, Jar::class.java)

        val installTask = project.tasks.register("installPlugin", Copy::class.java) { t ->
            t.group = "hytale"
            t.dependsOn(jarTask)
            t.from(jarTask.flatMap { it.archiveFile })
            t.into(ext.runDirectory.dir("mods"))
        }

        project.tasks.register("runServer", JavaExec::class.java) { t ->
            t.group = "hytale"
            t.dependsOn(installTask)

            t.standardInput = System.`in`
            t.workingDir(ext.runDirectory)
            t.classpath(ext.serverJar)

            val args = mutableListOf(
                "--assets", ext.assetsZip.get().asFile.absolutePath,
                "--assets", project.layout.projectDirectory.dir("src/main/resources").asFile.absolutePath,
            )

            if (ext.includeLocalMods.get()) {
                args += "--mods=${ext.hytaleHome.dir("UserData/Mods").get().asFile.absolutePath}"
            }

            if (ext.allowOp.get()) {
                args += "--allow-op"
            }

            t.args(args)
        }

        project.tasks.register("generateSources", GenerateSourcesTask::class.java) { t ->
            t.cacheDir.set(cacheDir)
        }
    }

    fun getHytaleBasePath(home: Path, patchline: String): Path {
        return home.resolve("install/$patchline/package/game/latest")
    }

    fun detectHytaleHome(): Path {
        val basePath = if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            val basePath = Path("${System.getenv("APPDATA")}\\Hytale")
            if (!basePath.exists()) {
                error("Could not find Hytale installation.")
            }
            basePath
        } else if (Os.isFamily(Os.FAMILY_MAC)) {
            val basePath =
                Path("${System.getProperty("user.home")}/Application Support/Hytale")
            if (!basePath.exists()) {
                error("Could not find Hytale installation.")
            }
            basePath
        } else if (Os.isFamily(Os.FAMILY_UNIX)) {
            val basePath =
                Path("${System.getProperty("user.home")}/.var/app/com.hypixel.HytaleLauncher/data/Hytale")
            if (!basePath.exists()) {
                error("Could not find Hytale installation.")
            }
            basePath
        } else {
            error("Unsupported operating system")
        }

        return basePath
    }
}
