package app.ultradev.hytalegradle.tasks

import app.ultradev.hytalegradle.HytaleExtension
import app.ultradev.hytalegradle.manifest.GenerateManifestTask
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskProvider

class GenerateManifestTaskSetup(
    private val project: Project,
    private val ext: HytaleExtension,
) : Action<GenerateManifestTask> {
    override fun execute(t: GenerateManifestTask) {
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
}