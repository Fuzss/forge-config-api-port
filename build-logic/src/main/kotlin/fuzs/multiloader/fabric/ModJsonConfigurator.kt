package fuzs.multiloader.fabric

import fuzs.multiloader.classtweaker.generatedClassTweakerFile
import fuzs.multiloader.extension.*
import fuzs.multiloader.metadata.LinkProvider
import fuzs.multiloader.metadata.ModLoaderProvider
import net.fabricmc.loom.task.FabricModJsonV1Task
import org.gradle.api.plugins.BasePluginExtension
import kotlin.jvm.optionals.getOrNull

fun FabricModJsonV1Task.setupModJsonTask() {
    val multiLoaderExtension = project.extensions.getByType(MultiLoaderExtension::class.java)
    outputFile.set(project.layout.buildDirectory.file("generated/resources/fabric.mod.json"))

    json {
        modId.set(project.mod.id)
        version.set(project.mod.version)
        name.set(project.mod.name)
        project.mod.authors.forEach { author(it) }
        description.set(project.mod.description)
        addDistributions()
        licenses.add(project.mod.license)
        icon("pack.png")
        if (project.metadata.environments.clientOnly()) {
            client()
        } else {
            // server-only mods also run on singleplayer servers
            environment.set("*")
        }

        addEntrypoints()

        listOf(project.commonProject, project).forEach {
            mixin("${it.mod.id}.${it.name.lowercase()}.mixins.json")
        }

        addDependencies()
        accessWidener.set(project.generatedClassTweakerFile.get().asFile.name)

        if (multiLoaderExtension.modFile.orNull?.library?.orNull == true) {
            customData.put("modmenu", mapOf("badges" to listOf("library")))
        }

        multiLoaderExtension.modFile.orNull?.json?.orNull?.execute(this)
    }
}

private fun FabricModJsonV1Task.addDistributions() {
    json {
        project.metadata.links
            .minByOrNull { it.name.index }
            ?.url()
            ?.let {
                contactInformation.put(
                    "homepage", it
                )
            }

        project.metadata.links
            .firstOrNull { it.name == LinkProvider.GITHUB }
            ?.url()
            ?.let {
                contactInformation.putAll(
                    mapOf(
                        "sources" to it,
                        "issues" to "$it/issues"
                    )
                )
            }
    }
}

private fun FabricModJsonV1Task.addEntrypoints() {
    // Helper to only add an entrypoint if the class file exists.
    fun addIfExists(type: String, className: String) {
        if (project.file("src/main/java/${className.replace('.', '/')}.java").exists()) {
            json.get().entrypoint(type, className)
        }
    }

    // Access the Base plugin extension for archivesName
    val baseExtension = project.extensions.getByType(BasePluginExtension::class.java)
    val archivesName = baseExtension.archivesName.get()
    val multiLoaderExtension = project.extensions.getByType(MultiLoaderExtension::class.java)
    val packagePrefix = multiLoaderExtension.modFile.orNull?.packagePrefix?.orNull
        ?.takeIf { it.isNotEmpty() }
        ?.let { "$it." }
        ?: ""

    // Construct fully qualified class names for main and client entrypoints.
    addIfExists("main", "${project.group}.${project.packageName.lowercase()}.${packagePrefix}${archivesName}Fabric")
    addIfExists(
        "client",
        "${project.group}.${project.packageName.lowercase()}.${packagePrefix}client.${archivesName}FabricClient"
    )
}

private fun FabricModJsonV1Task.addDependencies() {
    fun versionOrAny(alias: String) =
        project.versionCatalog.findVersion(alias).getOrNull()?.requiredVersion?.let { ">=${it}" } ?: "*"

    json {
        depends(
            "minecraft",
            project.versionCatalog.findVersion("minecraft").get().requiredVersion.let { version ->
                if (project.strictVersioning(version)) {
                    ">=${project.lowerBoundVersion(version)}- <${project.upperBoundVersion(version)}-"
                } else {
                    "~${project.lowerBoundVersion(version)}-"
                }
            }
        )

        depends("fabricloader", versionOrAny("fabricloader.min"))

        for (entry in project.metadata.dependencies) {
            if (entry.platforms.any { it.matches(ModLoaderProvider.FABRIC) }) {
                val modId = project.rootProject.externalMods.mods[entry.name]?.mod?.id ?: entry.name
                when (entry.name) {
                    "fabricapi" -> depends(modId, versionOrAny("fabricapi.min"))
                    "puzzleslib" -> depends(modId, versionOrAny("puzzleslib.min"))
                    else -> when {
                        entry.type.required -> depends(modId, "*")
                        entry.type.optional -> recommends(modId, "*")
                        entry.type.unsupported -> breaks(modId, "*")
                    }
                }
            }
        }
    }
}
