package app.ultradev.hytalegradle

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class HytaleExtension @Inject constructor(objects: ObjectFactory) {
    /** Hypixel installation path */
    abstract val hytaleHome: DirectoryProperty


    /** Path to HytaleServer.jar */
    abstract val serverJar: RegularFileProperty

    /** Path to Assets.zip */
    abstract val assetsZip: RegularFileProperty


    /** Hytale patchline to use (overridden by basePath) */
    abstract val patchline: Property<String>

    /** Adds `--allow-op` to the server arguments */
    abstract val allowOp: Property<Boolean>

    /** Directory to run the server in */
    abstract val runDirectory: DirectoryProperty

    /** Includes local mods in the server startup command */
    abstract val includeLocalMods: Property<Boolean>
}
