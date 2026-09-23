import fuzs.multiloader.classtweaker.classTweakerFile
import fuzs.multiloader.classtweaker.generateAccessTransformerFile
import fuzs.multiloader.classtweaker.generatedAccessTransformerFile
import fuzs.multiloader.extension.commonProject
import fuzs.multiloader.extension.expectPlatform
import fuzs.multiloader.extension.mod
import fuzs.multiloader.metadata.ModLoaderProvider
import fuzs.multiloader.mixin.MixinConfigJsonTask
import fuzs.multiloader.neoforge.setupModsTomlTask
import fuzs.multiloader.neoforge.toml.NeoForgeModsTomlSpec
import fuzs.multiloader.neoforge.toml.NeoForgeModsTomlTask
import org.gradle.api.internal.tasks.JvmConstants

plugins {
    id("fuzs.multiloader.multiloader-convention-plugins-platform")
    alias(libs.plugins.forgegradle)
}

project.expectPlatform(ModLoaderProvider.FORGE)
generateAccessTransformerFile(classTweakerFile, generatedAccessTransformerFile.get().asFile)

tasks.withType<Jar>().configureEach {
    manifest {
        attributes(
            mapOf(
                "MixinConfigs" to listOf(project.commonProject, project).joinToString(",") {
                    "${it.mod.id}.${it.name.lowercase()}.mixins.json"
                }
            )
        )
    }
}

repositories {
    minecraft.mavenizer(this)
    maven(fg.forgeMaven)
    maven(fg.minecraftLibsMaven)
}

dependencies {
    add("commonJava", project(mapOf("path" to ":Common-NeoForgeApi", "configuration" to "commonJava")))
    add("commonResources", project(mapOf("path" to ":Common-NeoForgeApi", "configuration" to "commonResources")))
    // This is only required for the IDE to see the common classes.
    compileOnly(project(":Common-NeoForgeApi")) { isTransitive = false }

    implementation(minecraft.dependency(sharedLibs.minecraftforge.forge))
}

tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE-FORGE.md"))
    from(rootProject.file("LICENSING.md"))
}

val generateModsToml = tasks.register<NeoForgeModsTomlTask>("generateModsToml") {
    setupModsTomlTask()
    outputFile.set(project.layout.buildDirectory.file("generated/resources/META-INF/mods.toml"))

    toml {
        dependencies.set(
            dependencies.get().filterNot {
                it.properties.get().modId.orNull == "neoforge"
            }
        )

        dependency(project.mod.id) {
            modId.set("forge")
            type.set(NeoForgeModsTomlSpec.DependencySpec.Type.REQUIRED)
            versionRange.set("[${sharedLibs.versions.minecraftforge.min.get()},)")
        }
    }
}

tasks.named<ProcessResources>(JvmConstants.PROCESS_RESOURCES_TASK_NAME) {
    dependsOn(generateModsToml)
}

listOf(project.commonProject, project).forEach { project ->
    project.tasks.named<MixinConfigJsonTask>("generateMixinConfig") {
        json {
            compatibilityLevel.set("JAVA_21")
        }
    }
}
