package app.ultradev.hytalegradle

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class HytaleExtension @Inject constructor(objects: ObjectFactory) {
    /** Base directory of the Hytale "latest" install (contains Server/ and Assets.zip) */
    abstract val basePath: DirectoryProperty

    /** Adds `--allow-op` to the server arguments */
    abstract val allowOp: Property<Boolean>

    /** Attaches a decompiled source jar to allow searching server code in IDEs */
    abstract val attachSources: Property<Boolean>
}
