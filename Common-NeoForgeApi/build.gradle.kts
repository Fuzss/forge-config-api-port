import fuzs.multiloader.extension.commonProject
import fuzs.multiloader.extension.expectPlatform
import fuzs.multiloader.metadata.ModLoaderProvider
import fuzs.multiloader.vendoredsources.CheckVendoredSourcesTask
import fuzs.multiloader.vendoredsources.SourceSpec
import fuzs.multiloader.vendoredsources.SyncVendoredSourcesTask
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.internal.tasks.JvmConstants

plugins {
    id("fuzs.multiloader.multiloader-convention-plugins-neoforge-like")
}

project.expectPlatform(ModLoaderProvider.COMMON)

neoForge {
    enable {
        neoFormVersion = sharedLibs.versions.neoform.get()
        isDisableRecompilation = true
    }
}

configurations {
    named("commonJava") {
        isCanBeResolved = true
    }
    named("commonResources") {
        isCanBeResolved = true
    }
}

dependencies {
    add("commonJava", project(mapOf("path" to project.commonProject.path, "configuration" to "commonJava")))
    add("commonResources", project(mapOf("path" to project.commonProject.path, "configuration" to "commonResources")))
    // This is only required for the IDE to see the common classes.
    compileOnly(project(project.commonProject.path)) { isTransitive = false }

    compileOnly(sharedLibs.mixin.common)
    compileOnly(sharedLibs.mixinextras.common)
    compileOnlyApi(sharedLibs.nightconfigcore.common)
    compileOnlyApi(sharedLibs.nightconfigtoml.common)
}

tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE-FORGE.md"))
    from(rootProject.file("LICENSING.md"))
}

tasks.named<JavaCompile>(JvmConstants.COMPILE_JAVA_TASK_NAME) {
    dependsOn(configurations.named("commonJava"))
    source(configurations.named("commonJava"))
}

tasks.named<ProcessResources>(JvmConstants.PROCESS_RESOURCES_TASK_NAME) {
    dependsOn(configurations.named("commonResources"))
    from(configurations.named("commonResources")) {
        exclude("**/*.classtweaker", "**/*.accesswidener")
    }

    dependsOn(project.commonProject.tasks.named<ProcessResources>(JvmConstants.PROCESS_RESOURCES_TASK_NAME))
    from(project.commonProject.layout.buildDirectory.dir("generated/resources")) {
        exclude("**/*.classtweaker", "**/*.accesswidener", "**/*.cfg", "architectury.common.json")
    }
}

tasks.named<Jar>("sourcesJar") {
    dependsOn(configurations.named("commonJava"))
    from(configurations.named("commonJava"))

    dependsOn(configurations.named("commonResources"))
    from(configurations.named("commonResources"))
}

tasks.named<Javadoc>(JvmConstants.JAVADOC_TASK_NAME) {
    dependsOn(configurations.named("commonJava"))
    source(configurations.named("commonJava"))
}

tasks.named("generateMixinConfig") {
    enabled = false
}

val neoforgeVendoredVersion: String = extensions.getByType<VersionCatalogsExtension>()
    .named("sharedLibs")
    .findVersion("neoforge.version")
    .get()
    .requiredVersion
val fmlVendoredVersion: String = libs.versions.fancymodloader.get()

repositories {
    maven("https://maven.neoforged.net/releases/") {
        name = "NeoForge"
        content {
            includeGroupAndSubgroups("net.neoforged")
        }
    }
}

val neoforgeVendoredSources = configurations.create("neoforgeVendoredSources") {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}
val fmlVendoredSources = configurations.create("fmlVendoredSources") {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

dependencies {
    add(neoforgeVendoredSources.name, "net.neoforged:neoforge:$neoforgeVendoredVersion:sources")
    add(fmlVendoredSources.name, "net.neoforged.fancymodloader:loader:$fmlVendoredVersion:sources")
}

val vendoredSources = provider {
    listOf(
        SourceSpec("neoforge", neoforgeVendoredVersion, "net.neoforged.neoforge", null, neoforgeVendoredSources.singleFile),
        SourceSpec("fml", fmlVendoredVersion, "net.neoforged.fml", null, fmlVendoredSources.singleFile),
    )
}

tasks.register<SyncVendoredSourcesTask>("syncVendoredSources") {
    group = "vendored sources"
    description = "Regenerates the vendored upstream sources from the committed patches."
    manifestFile.set(layout.projectDirectory.file("patches/manifest"))
    patchesDir.set(layout.projectDirectory.dir("patches"))
    outputDir.set(layout.projectDirectory.dir("src/main/java"))
    workDir.set(layout.buildDirectory.dir("vendored-sources"))
    lockFile.set(layout.projectDirectory.file("patches/lock"))
    sources.set(vendoredSources)
}

tasks.register<CheckVendoredSourcesTask>("checkVendoredSources") {
    group = "vendored sources"
    description = "Verifies the vendored upstream sources match upstream plus the committed patches."
    manifestFile.set(layout.projectDirectory.file("patches/manifest"))
    patchesDir.set(layout.projectDirectory.dir("patches"))
    committedDir.set(layout.projectDirectory.dir("src/main/java"))
    workDir.set(layout.buildDirectory.dir("vendored-sources-check"))
    lockFile.set(layout.projectDirectory.file("patches/lock"))
    sources.set(vendoredSources)
}

tasks.named("check") {
    dependsOn("checkVendoredSources")
}
