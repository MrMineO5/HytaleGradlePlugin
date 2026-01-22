package app.ultradev.hytalegradle

import app.ultradev.hytalegradle.manifest.GenerateManifestTask
import app.ultradev.hytalegradle.tasks.GenerateManifestTaskSetup
import app.ultradev.hytalegradle.tasks.RunServerTaskSetup
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.tasks.Jar
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

class HytaleGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val ext = project.extensions.create("hytale", HytaleExtension::class.java)

        ext.legacyMode.convention(false)
        ext.patchline.convention("release")
        ext.version.convention("latest.release")

        ext.hytaleHome.convention(project.layout.dir(project.provider { HytaleInstallation.detectHytaleHome().toFile() }))
        ext.serverJar.convention(project.layout.file(project.provider {
            HytaleInstallation.getHytaleBasePath(
                ext.hytaleHome.get().asFile.toPath(), ext.patchline.get()
            ).resolve("Server/HytaleServer.jar").toFile()
        }))
        ext.assetsZip.convention(project.layout.file(project.provider {
            HytaleInstallation.getHytaleBasePath(
                ext.hytaleHome.get().asFile.toPath(), ext.patchline.get()
            ).resolve("Assets.zip").toFile()
        }))
        ext.allowOp.convention(false)
        ext.runDirectory.convention(project.layout.projectDirectory.dir("run"))
        ext.includeLocalMods.convention(false)


        val serverConfiguration = project.configurations.create("hytaleServer") {
            it.isCanBeConsumed = false
            it.isCanBeResolved = true
        }

        project.configurations.getAt("compileOnly").extendsFrom(serverConfiguration)
        project.configurations.getAt("testCompileOnly").extendsFrom(serverConfiguration)

        val cacheDir = project.layout.buildDirectory.dir("hytale")

        project.afterEvaluate {
            cacheDir.get().asFile.mkdirs()

            if (ext.legacyMode.get()) {
                project.repositories.flatDir {
                    it.name = "hytaleGenerated"
                    it.dir(cacheDir)
                }

                project.dependencies.add(serverConfiguration.name, mapOf("name" to "HytaleServer"))

                val installedServer = ext.serverJar.get().asFile.toPath()
                val assetsZip = ext.assetsZip.get().asFile.toPath()
                if (!Files.exists(installedServer) || !Files.exists(assetsZip)) {
                    throw GradleException(
                        "Missing local Hytale installation files:\n" + (if (!installedServer.exists()) " - Server jar: ${installedServer.absolutePathString()}\n" else "") + (if (!assetsZip.exists()) " - Assets zip: ${assetsZip.absolutePathString()}\n" else "") + "Make sure you have Hytale installed, or manually configure serverJar and assetsZip to the correct files"
                    )
                }
                val cacheDirPath = cacheDir.get().asFile.toPath()
                val localCopy = cacheDirPath.resolve("HytaleServer.jar")

                if (Files.exists(localCopy) && Files.mismatch(
                        installedServer, localCopy
                    ) == -1L
                ) return@afterEvaluate // no need to copy

                Files.createDirectories(cacheDirPath)
                Files.copy(installedServer, localCopy, StandardCopyOption.REPLACE_EXISTING)
                Files.deleteIfExists(cacheDirPath.resolve("HytaleServer-sources.jar"))
            } else {
                project.repositories.maven {
                    it.name = "hytale-sources"
                    it.url = cacheDir.get().asFile.toURI()
                }
                project.repositories.maven {
                    it.name = "hytale-${ext.patchline.get()}"
                    it.url = URI("https://maven.hytale.com/${ext.patchline.get()}")
                }
                project.dependencies.add(serverConfiguration.name, "com.hypixel.hytale:Server:${ext.version.get()}")
            }
        }

        project.tasks.named("clean", Delete::class.java) {
            it.delete(ext.runDirectory)
        }

        val updateManifest = project.tasks.register(
            "updateManifest", GenerateManifestTask::class.java, GenerateManifestTaskSetup(project, ext)
        )

        project.tasks.named("processResources") {
            it.dependsOn(updateManifest)
        }


        val archiveTaskName = if (project.tasks.names.contains("shadowJar")) "shadowJar" else "jar"

        val jarTask = project.tasks.named(archiveTaskName, Jar::class.java)

        val installTask = project.tasks.register("installPlugin", Copy::class.java) { t ->
            t.group = "hytale"
            t.dependsOn(jarTask)
            t.from(jarTask.flatMap { it.archiveFile })
            t.into(ext.runDirectory.dir("mods"))
        }

        project.tasks.register(
            "runServer", JavaExec::class.java, RunServerTaskSetup(
                serverConfiguration, installTask, cacheDir, project, ext
            )
        )

        project.tasks.register("generateSources", GenerateSourcesTask::class.java) { t ->
            t.doFirst {
                if (!ext.legacyMode.get()) {
                    val serverJar = serverConfiguration.singleFile
                    if (serverJar.exists()) {
                        serverJar.copyTo(cacheDir.get().asFile.resolve("HytaleServer.jar"), true)
                    }
                }
            }
            t.cacheDir.set(cacheDir)

            t.doLast {
                if (!ext.legacyMode.get()) { // TODO: Is there a better way to handle this? IntelliJ won't attach sources from a different repo
                    val cacheRoot = cacheDir.get().asFile

                    val generatedSources = cacheRoot.resolve("HytaleServer-sources.jar")
                    if (!generatedSources.exists()) return@doLast

                    val resolvedArtifact = serverConfiguration
                        .resolvedConfiguration
                        .resolvedArtifacts
                        .single()

                    val id = resolvedArtifact.moduleVersion.id
                    val group = id.group
                    val name = id.name
                    val version = id.version

                    val mavenDir = cacheRoot.resolve(
                        "${group.replace('.', '/')}/$name/$version"
                    )
                    mavenDir.mkdirs()

                    generatedSources.copyTo(
                        mavenDir.resolve("$name-$version-sources.jar"),
                        overwrite = true
                    )
                    resolvedArtifact.file.copyTo(
                        mavenDir.resolve("$name-$version.jar"),
                        overwrite = true
                    )

                    val pomDep = project.dependencies.create("$group:$name:$version@pom")
                    val pomCfg = project.configurations.detachedConfiguration(pomDep).apply {
                        isCanBeConsumed = false
                        isCanBeResolved = true
                        isTransitive = false
                    }

                    val pomFile = pomCfg.singleFile
                    pomFile.copyTo(
                        mavenDir.resolve("$name-$version.pom"),
                        overwrite = true
                    )
                }
            }

        }
    }


}