package app.ultradev.hytalegradle.manifest

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import javax.inject.Inject

@CacheableTask
abstract class GenerateManifestTask @Inject constructor() : DefaultTask() {

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val templateManifests: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputManifest: RegularFileProperty

    // ---- values to overlay (all optional) ----
    @get:Input @get:Optional abstract val manifestGroup: Property<String>
    @get:Input @get:Optional abstract val manifestName: Property<String>
    @get:Input @get:Optional abstract val manifestVersion: Property<String>
    @get:Input @get:Optional abstract val manifestDescription: Property<String>
    @get:Input @get:Optional abstract val manifestMainClass: Property<String>
    @get:Input @get:Optional abstract val manifestAuthors: ListProperty<String>
    @get:Input @get:Optional abstract val manifestWebsite: Property<String>
    @get:Input @get:Optional abstract val manifestServerVersion: Property<String>
    @get:Input @get:Optional abstract val manifestDependencies: MapProperty<String, String>
    @get:Input @get:Optional abstract val manifestOptionalDependencies: MapProperty<String, String>
    @get:Input @get:Optional abstract val manifestLoadBefore: MapProperty<String, String>
    @get:Input @get:Optional abstract val manifestDisabledByDefault: Property<Boolean>
    @get:Input @get:Optional abstract val manifestIncludesAssetPack: Property<Boolean>

    @TaskAction
    fun generate() {
        val templateFile = templateManifests.singleFile
        val outFile = outputManifest.get().asFile
        outFile.parentFile.mkdirs()

        val root = if (templateFile.exists()) {
            try {
                JsonSlurper().parse(templateFile) as? MutableMap<String, Any>
                    ?: throw IllegalStateException("manifest.json root must be a JSON object: ${templateFile.absolutePath}")
            } catch(e: Throwable) {
                throw GradleException(
                    "Unable to parse existing manifest: ",
                    e
                )
            }
        } else mutableMapOf<String, Any>( // Add template when generating a new manifest
            "Group" to "Example",
            "Name" to "ExamplePlugin",
            "Main" to "com.example.plugin.Plugin",
            "Version" to "1.0.0"
        )

        var anyChanged = false

        fun MutableMap<String, Any>.putIfPresent(key: String, p: Property<String>) {
            if (p.isPresent) {
                anyChanged = anyChanged || this[key] != p.get()
                this[key] = p.get()
            }
        }
        fun MutableMap<String, Any>.putIfPresent(key: String, p: Property<Boolean>) {
            if (p.isPresent) {
                anyChanged = anyChanged || this[key] != p.get()
                this[key] = p.get()
            }
        }
        fun MutableMap<String, Any>.putIfPresent(key: String, p: ListProperty<String>) {
            if (p.isPresent) {
                anyChanged = anyChanged || this[key] != p.get()
                this[key] = p.get()
            }
        }
        fun MutableMap<String, Any>.putIfPresent(key: String, p: MapProperty<String, String>) {
            if (p.isPresent) {
                anyChanged = anyChanged || this[key] != p.get()
                this[key] = p.get()
            }
        }

        // Map your extension properties -> JSON keys (adjust to your manifest schema!)
        root.putIfPresent("Group", manifestGroup)
        root.putIfPresent("Name", manifestName)
        root.putIfPresent("Version", manifestVersion)
        root.putIfPresent("Description", manifestDescription)
        root.putIfPresent("MainClass", manifestMainClass)
        root.putIfPresent("Authors", manifestAuthors)
        root.putIfPresent("Website", manifestWebsite)
        root.putIfPresent("ServerVersion", manifestServerVersion)
        root.putIfPresent("Dependencies", manifestDependencies)
        root.putIfPresent("OptionalDependencies", manifestOptionalDependencies)
        root.putIfPresent("LoadBefore", manifestLoadBefore)
        root.putIfPresent("DisabledByDefault", manifestDisabledByDefault)
        root.putIfPresent("IncludesAssetPack", manifestIncludesAssetPack)

        if (!anyChanged) return

        outFile.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(root)))
    }
}
