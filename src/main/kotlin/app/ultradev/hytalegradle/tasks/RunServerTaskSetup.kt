package app.ultradev.hytalegradle.tasks

import app.ultradev.hytalegradle.HytaleExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskProvider

class RunServerTaskSetup(
    private val serverConfiguration: Configuration,
    private val installTask: TaskProvider<*>,
    private val cacheDir: Provider<Directory>,
    private val project: Project,
    private val ext: HytaleExtension,
) : Action<JavaExec> {
    override fun execute(t: JavaExec) {
        t.group = "hytale"
        t.dependsOn(installTask)

        t.doFirst {
            if (!ext.assetsZip.get().asFile.exists()) {
                throw IllegalStateException("Assets zip file does not exist at ${ext.assetsZip.get().asFile.absolutePath}")
            }
        }

        t.standardInput = System.`in`
        t.workingDir(ext.runDirectory)
        if (ext.legacyMode.get()) {
            t.classpath(cacheDir.get().file("HytaleServer.jar"))
        } else {
            t.classpath(serverConfiguration)
        }

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

        if (ext.disableSentry.get()) {
            args += "--disable-sentry"
        }

        if (ext.sessionToken.isPresent) {
            args += "--session-token"
            args += ext.sessionToken.get()
        }
        if (ext.identityToken.isPresent) {
            args += "--identity-token"
            args += ext.identityToken.get()
        }

        t.args(args)
    }
}