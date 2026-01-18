package app.ultradev.hytalegradle.manifest

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class ManifestExtension @Inject constructor(
    objects: ObjectFactory
) {
    abstract val group: Property<String>
    abstract val name: Property<String>
    abstract val version: Property<String>
    abstract val description: Property<String>
    abstract val mainClass: Property<String>
    abstract val authors: ListProperty<String>
    abstract val website: Property<String>
    abstract val serverVersion: Property<String>
    abstract val dependencies: MapProperty<String, String>
    abstract val optionalDependencies: MapProperty<String, String>
    abstract val loadBefore: MapProperty<String, String>
    abstract val disabledByDefault: Property<Boolean>
    abstract val includesAssetPack: Property<Boolean>
}
