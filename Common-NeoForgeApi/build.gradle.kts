import fuzs.multiloader.extension.commonProject
import fuzs.multiloader.extension.expectPlatform
import fuzs.multiloader.metadata.ModLoaderProvider
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
