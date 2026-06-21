package fuzs.multiloader.extension

import fuzs.multiloader.metadata.*
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.getByType

// Expose our shared version catalog.
val Project.versionCatalog: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("sharedLibs")

// Load external mods once per project.
val Project.externalMods: ExternalMods
    get() {
        if (this == rootProject) {
            return extensions.findByType(ExternalMods::class.java)
                ?: ExternalMods(ExternalMods.BY_ID.toMutableMap()).also {
                    extensions.add(ExternalMods::class.java, "externalMods", it)
                }
        } else {
            throw GradleException("External mod metadata is only available for the root project")
        }
    }

// Load metadata once per project.
val Project.metadata: ModMetadata
    get() = extensions.findByType(ModMetadata::class.java) ?: loadMetadata().also {
        extensions.add(ModMetadata::class.java, "metadata", it)
    }

// Expose the mod entry from metadata.
val Project.mod: ModEntry
    get() = metadata.mod

val Project.packageName: String
    get() = name.lowercase().replace(Regex("[^a-z0-9]+"), ".").trim('.')

// Load the loom.platform Architectury Loom property.
val Project.projectPlatform: ModLoaderProvider
    get() = (if (extra.has("loom.platform")) extra["loom.platform"] as? String else null)?.uppercase()
        ?.let { ModLoaderProvider.valueOf(it) } ?: ModLoaderProvider.COMMON

fun Project.expectPlatform(platform: ModLoaderProvider) {
    if (platform != projectPlatform) {
        throw GradleException("Mismatched platform: $projectPlatform != ${platform}, define via loom.platform=${platform} in gradle.properties")
    }
}

// Get the Common project from the Loom property.
val Project.commonProject: Project
    get() = rootProject.subprojects.first { !it.projectPlatform.platform }

// Get the platform projects from the Loom property.
val Project.platformProjects: List<Project>
    get() = rootProject.subprojects.filter { it.projectPlatform.platform }

// The "project.debug" property controls whether uploads to remotes (e.g., CurseForge, Discord) should be run in debug mode.
val Project.debugRemoteUploads: Boolean
    get() = providers.gradleProperty("project.debug").orNull.toBoolean()

// The "project.isolated" property controls whether some default mods are omitted as dependencies.
val Project.defaultDependencies: Boolean
    get() = providers.gradleProperty("project.isolated").orNull.toBoolean().not()

// The "project.strict" property controls whether built artifacts are required to run only on the version they have been compiled against.
// This is forcibly enabled for all versions using the 1.x.x versioning scheme.
fun Project.strictVersioning(version: String): Boolean {
    return version.startsWith("1.") || providers.gradleProperty("project.strict").orNull.toBoolean()
}

// The Minecraft version representing the supported range of versions e.g., 1.21.1, 26.1.x.
// This is also used when referencing the GitHub branch for the current Minecraft version.
val Project.minecraftVersion: String
    get() = artifactVersion(versionCatalog.findVersion("minecraft").get().requiredVersion)
