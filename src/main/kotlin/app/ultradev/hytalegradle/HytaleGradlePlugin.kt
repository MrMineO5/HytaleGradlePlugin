package app.ultradev.hytalegradle

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

class HytaleGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val ext = project.extensions.create("hytale", HytaleExtension::class.java)

        ext.patchline.convention("release")
        ext.basePath.convention(project.layout.dir(project.provider {
            detectHytaleBaseDir(ext.patchline.get()).toFile()
        }))
        ext.allowOp.convention(false)
        ext.attachSources.convention(false)

        project.afterEvaluate {
            if (ext.attachSources.get()) {
                val cacheDir = project.layout.buildDirectory.dir("hytale").get().asFile.toPath()
                val localRepo = GenerateSources.generateSources(project.logger, cacheDir, ext.basePath.get().asFile.toPath())
                project.repositories.flatDir {
                    it.name = "hytaleGenerated"
                    it.dir(localRepo)
                }

                project.dependencies.add("compileOnly", mapOf("name" to "HytaleServer"))
            } else {
                project.dependencies.add(
                    "compileOnly", project.files(
                        File(
                            ext.basePath.get().asFile,
                            "Server${File.separator}HytaleServer.jar"
                        )
                    )
                )
            }
        }

        val archiveTaskName =
            if (project.tasks.names.contains("shadowJar")) "shadowJar" else "jar"

        val jarTask = project.tasks.named(archiveTaskName, Jar::class.java)

        project.tasks.register("runServer", RunServerTask::class.java) { t ->
            t.dependsOn(jarTask)
            t.sourceJar.set(jarTask.flatMap { it.archiveFile })

            t.runDir.set(project.layout.projectDirectory.dir("run"))

            t.basePath.set(ext.basePath)
            t.allowOp.set(ext.allowOp)
        }
    }

    fun detectHytaleBaseDir(patchline: String): Path {
        val basePath = if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            val basePath = Path("${System.getenv("APPDATA")}\\Hytale\\install\\$patchline\\package\\game\\latest")
            if (!basePath.exists()) {
                error("Could not find Hytale installation.")
            }
            basePath
        } else if (Os.isFamily(Os.FAMILY_MAC)) {
            val basePath = Path("${System.getProperty("user.home")}/Application Support/Hytale/install/$patchline/package/game/latest")
            if (!basePath.exists()) {
                error("Could not find Hytale installation.")
            }
            basePath
        } else if (Os.isFamily(Os.FAMILY_UNIX)) {
            val basePath =
                Path("${System.getProperty("user.home")}/.var/app/com.hypixel.HytaleLauncher/data/Hytale/install/$patchline/package/game/latest/")
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
