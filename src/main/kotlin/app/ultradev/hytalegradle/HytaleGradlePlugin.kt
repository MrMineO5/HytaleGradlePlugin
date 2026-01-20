package app.ultradev.hytalegradle

import app.ultradev.hytalegradle.manifest.GenerateManifestTask
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.tasks.Jar
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import org.gradle.api.GradleException

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
            val assetsZip = ext.assetsZip.get().asFile.toPath()
            if (!Files.exists(installedServer) || !Files.exists(assetsZip)) {
                throw GradleException(
                    "Missing local Hytale installation files:\n" +
                            (if (!installedServer.exists()) " - Server jar: ${installedServer.absolutePathString()}\n" else "") +
                            (if (!assetsZip.exists()) " - Assets zip: ${assetsZip.absolutePathString()}\n" else "") +
                    "Make sure you have Hytale installed, or manually configure serverJar and assetsZip to the correct files"
                )
            }
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

        val updateManifest = project.tasks.register("updateManifest", GenerateManifestTask::class.java) { t ->
            t.group = "hytale"
            t.description = "Updates manifest.json if configured"

            val manifestFile = project.layout.projectDirectory.file("src/main/resources/manifest.json")
            t.templateManifests.from(manifestFile)
            t.outputManifest.set(manifestFile)

            // overlay values from extension (only applied if present)
            t.manifestGroup.set(ext.manifest.group)
            t.manifestName.set(ext.manifest.name)
            t.manifestVersion.set(ext.manifest.version)
            t.manifestDescription.set(ext.manifest.description)
            t.manifestMainClass.set(ext.manifest.mainClass)
            t.manifestAuthors.set(ext.manifest.authors.map {
                it.map { author ->
                    val jsonAuthor = mutableMapOf<String, String>()
                    jsonAuthor["Name"] = author.name
                    if (author.email != null) {
                        jsonAuthor["Email"] = author.email
                    }
                    if (author.url != null) {
                        jsonAuthor["Website"] = author.url
                    }
                    jsonAuthor
                }
            })
            t.manifestWebsite.set(ext.manifest.website)
            t.manifestServerVersion.set(ext.manifest.serverVersion)
            t.manifestDependencies.set(ext.manifest.dependencies)
            t.manifestOptionalDependencies.set(ext.manifest.optionalDependencies)
            t.manifestLoadBefore.set(ext.manifest.loadBefore)
            t.manifestDisabledByDefault.set(ext.manifest.disabledByDefault)
            t.manifestIncludesAssetPack.set(ext.manifest.includesAssetPack)
        }

        project.tasks.named("processResources") {
            it.dependsOn(updateManifest)
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
            t.classpath(cacheDir.get().file("HytaleServer.jar"))

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

        project.tasks.register("generateSources", GenerateSourcesTask::class.java) { t ->
            t.cacheDir.set(cacheDir)
        }
    }

    fun getHytaleBasePath(home: Path, patchline: String): Path {
        return home.resolve("install/$patchline/package/game/latest")
    }

    fun detectHytaleHome(): Path {
        val basePath = if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            val basePath = Path(Advapi32Util.registryGetStringValue(
                WinReg.HKEY_LOCAL_MACHINE,
                "SOFTWARE\\Hypixel Studios\\Hytale",
                "GameInstallPath"
            ))
            if (!basePath.exists()) {
                error("Could not find Hytale installation.")
            }
            basePath
        } else if (Os.isFamily(Os.FAMILY_MAC)) {
            val basePath =
                Path("${System.getProperty("user.home")}/Library/Application Support/Hytale")
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