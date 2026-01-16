package app.ultradev.hytalegradle

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class HytaleExtension @Inject constructor(objects: ObjectFactory) {
    /** Base directory of Hytale installation (contains install/ and UserData/) */
    abstract val basePath: DirectoryProperty

    /** Hytale patchline to use (overridden by basePath) */
    abstract val patchline: Property<String>

    /** Adds `--allow-op` to the server arguments */
    abstract val allowOp: Property<Boolean>

    /** Directory to run the server in */
    abstract val runDirectory: DirectoryProperty
}
