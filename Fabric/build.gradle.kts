import fuzs.multiloader.vendoredsources.CheckVendoredSourcesTask
import fuzs.multiloader.vendoredsources.SourceSpec
import fuzs.multiloader.vendoredsources.SyncVendoredSourcesTask
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("fuzs.multiloader.multiloader-convention-plugins-fabric")
}

dependencies {
    listOf(project(":Common-ForgeApi"), project(":Common-NeoForgeApi")).forEach {
        add("commonJava", project(mapOf("path" to it.path, "configuration" to "commonJava")))
        add("commonResources", project(mapOf("path" to it.path, "configuration" to "commonResources")))
        // This is only required for the IDE to see the common classes.
        compileOnly(project(it.path)) { isTransitive = false }
    }

    modApi(sharedLibs.fabricapi.fabric)
    api(sharedLibs.nightconfigcore.common)
    include(sharedLibs.nightconfigcore.common)
    api(sharedLibs.nightconfigtoml.common)
    include(sharedLibs.nightconfigtoml.common)
    modCompileOnly(sharedLibs.modmenu.fabric) { isTransitive = false }
    modLocalRuntime(sharedLibs.modmenu.fabric) { isTransitive = false }
}

tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE-FORGE.md"))
    from(rootProject.file("LICENSE-NIGHT-CONFIG.md"))
    from(rootProject.file("LICENSING.md"))
}

multiloader {
    modFile {
        packagePrefix.set("impl")
        library.set(true)
        json {
            entrypoint(
                "modmenu",
                "${project.group}.${project.name.lowercase()}.impl.integration.modmenu.ModMenuApiImpl"
            )
        }
    }
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
