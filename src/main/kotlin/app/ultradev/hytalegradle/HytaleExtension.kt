package app.ultradev.hytalegradle

import app.ultradev.hytalegradle.manifest.ManifestExtension
import org.gradle.api.Action
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class HytaleExtension @Inject constructor(objects: ObjectFactory) {
    /**
     * If `true`, gets the Hytale installation from the local install rather than maven
     */
    abstract val legacyMode: Property<Boolean>

    /**
     * Hytale patchline to use (overridden by basePath)
     *
     * Available patchlines: `release`, `pre-release`
     */
    abstract val patchline: Property<String>

    /**
     * Maven dependency version, defaults to `latest.release`
     */
    abstract val version: Property<String>


    /**
     * Hypixel installation path
     */
    abstract val hytaleHome: DirectoryProperty

    /**
     * Path to HytaleServer.jar
     */
    abstract val serverJar: RegularFileProperty

    /**
     * Path to Assets.zip
     */
    abstract val assetsZip: RegularFileProperty

    /**
     * Adds `--allow-op` to the server arguments
     *
     * This allows players to run `/op self` to get permissions
     */
    abstract val allowOp: Property<Boolean>

    /** Directory to run the server in */
    abstract val runDirectory: DirectoryProperty

    /**
     * If `true`, deletes the run directory with the `clean` task
     */
    abstract val cleanDeletesRunDirectory: Property<Boolean>

    /**
     * Includes local mods from the `hytaleHome` directory in the server startup command
     */
    abstract val includeLocalMods: Property<Boolean>


    /**
     * Optionally pass a session token for automatic authentication
     *
     * NOTE: You need both a session token and an identity token for this to work
     */
    abstract val sessionToken: Property<String>
    /**
     * Optionally pass an identity token for automatic authentication
     *
     * NOTE: You need both a session token and an identity token for this to work
     */
    abstract val identityToken: Property<String>


    val manifest: ManifestExtension =
        objects.newInstance(ManifestExtension::class.java)

    fun manifest(action: Action<ManifestExtension>) {
        action.execute(manifest)
    }
}
