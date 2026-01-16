package app.ultradev.hytalegradle

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.div
import kotlin.io.path.exists

class HytaleGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val ext = project.extensions.create("hytale", HytaleExtension::class.java)

        ext.patchline.convention("release")
        ext.basePath.convention(project.layout.dir(project.provider {
            detectHytaleBaseDir().toFile()
        }))
        ext.allowOp.convention(false)
        ext.includesPack.convention(true)
        ext.loadUserMods.convention(false)
        ext.runDirectory.convention(project.layout.projectDirectory.dir("run"))

        val basePath = ext.basePath.get().asFile.toPath()
        val installPath = getHytaleInstallPath(basePath, ext.patchline.get())
        val serverJarPath = installPath / "Server" / "HytaleServer.jar"

        val cacheDir = project.layout.buildDirectory.dir("hytale")

        project.repositories.flatDir {
            it.name = "hytaleGenerated"
            it.dir(cacheDir)
        }

        project.dependencies.add("compileOnly", mapOf("name" to "HytaleServer"))

        project.afterEvaluate {
            if (!Files.exists(serverJarPath)) return@afterEvaluate
            val cacheDir = cacheDir.get().asFile.toPath()
            val localCopy = cacheDir.resolve("HytaleServer.jar")

            if (Files.exists(localCopy) && Files.mismatch(serverJarPath, localCopy) == -1L) return@afterEvaluate // no need to copy

            Files.createDirectories(cacheDir)
            Files.copy(serverJarPath, localCopy, StandardCopyOption.REPLACE_EXISTING)
            Files.delete(cacheDir.resolve("HytaleServer-sources.jar"))
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
            t.classpath(serverJarPath.toFile())

            val args = mutableListOf(
                "--assets", (installPath / "Assets.zip").absolutePathString()
            )

            if (ext.allowOp.get()) {
                args += "--allow-op"
            }

            if (ext.includesPack.get()) {
                val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
                val mainSourceSet = sourceSets.getByName("main")
                val mainSourceSetPath = mainSourceSet.resources.srcDirs.first().parentFile.absolutePath
                args += "--mods=${mainSourceSetPath}"
            }

            if (ext.loadUserMods.get()) {
                val userModsPath = (basePath / "UserData" / "Mods").absolutePathString()
                args += "--mods=${userModsPath}"
            }

            t.args(args)
        }

        project.tasks.register("generateSources", GenerateSourcesTask::class.java) { t ->
            t.cacheDir.set(cacheDir)
        }
    }

    fun getHytaleInstallPath(basePath: Path, patchline: String): Path {
        return basePath / "install" / patchline / "package" / "game" / "latest"
    }

    fun detectHytaleBaseDir(): Path {
        val basePath = if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            Path("${System.getenv("APPDATA")}\\Hytale")
        } else if (Os.isFamily(Os.FAMILY_MAC)) {
            Path("${System.getProperty("user.home")}/Application Support/Hytale")
        } else if (Os.isFamily(Os.FAMILY_UNIX)) {
            Path("${System.getProperty("user.home")}/.var/app/com.hypixel.HytaleLauncher/data/Hytale")
        } else {
            error("Unsupported operating system")
        }

        if (!basePath.exists()) {
            error("Could not find Hytale installation.")
        }

        return basePath
    }
}
