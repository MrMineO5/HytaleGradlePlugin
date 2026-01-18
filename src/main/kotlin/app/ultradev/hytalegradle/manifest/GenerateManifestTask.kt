package app.ultradev.hytalegradle.manifest

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import javax.inject.Inject

@CacheableTask
abstract class GenerateManifestTask @Inject constructor() : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val templateManifest: RegularFileProperty

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
        val templateFile = templateManifest.get().asFile
        val outFile = outputManifest.get().asFile
        outFile.parentFile.mkdirs()

        val root = if (templateFile.exists()) {
            JsonSlurper().parse(templateFile) as? MutableMap<String, Any>
                ?: throw IllegalStateException("manifest.json root must be a JSON object: ${templateFile.absolutePath}")
        } else mutableMapOf()

        var anyChanged: Boolean = false

        fun MutableMap<String, Any>.putIfPresent(key: String, p: Property<String>) {
            if (p.isPresent) {
                anyChanged = this[key] != p.get()
                this[key] = p.get()
            }
        }
        fun MutableMap<String, Any>.putIfPresent(key: String, p: Property<Boolean>) {
            if (p.isPresent) {
                anyChanged = this[key] != p.get()
                this[key] = p.get()
            }
        }
        fun MutableMap<String, Any>.putIfPresent(key: String, p: ListProperty<String>) {
            if (p.isPresent) {
                anyChanged = this[key] != p.get()
                this[key] = p.get()
            }
        }
        fun MutableMap<String, Any>.putIfPresent(key: String, p: MapProperty<String, String>) {
            if (p.isPresent) {
                anyChanged = this[key] != p.get()
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
