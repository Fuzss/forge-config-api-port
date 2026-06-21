package fuzs.multiloader

import fuzs.multiloader.classtweaker.classTweakerFile
import fuzs.multiloader.classtweaker.generateClassTweakerFile
import fuzs.multiloader.classtweaker.generatedClassTweakerFile
import fuzs.multiloader.extension.*
import fuzs.multiloader.fabric.setupModJsonTask
import fuzs.multiloader.metadata.ModLoaderProvider
import net.fabricmc.loom.task.FabricModJsonV1Task
import org.gradle.api.internal.tasks.JvmConstants
import kotlin.jvm.optionals.getOrNull

plugins {
    id("fuzs.multiloader.multiloader-convention-plugins-platform")
    id("net.fabricmc.fabric-loom")
}

project.expectPlatform(ModLoaderProvider.FABRIC)
generateClassTweakerFile(classTweakerFile, generatedClassTweakerFile.get().asFile)

configurations {
    create("modLocalRuntime") {
        this@configurations.named("localRuntime") {
            extendsFrom(this@create)
        }
    }
}

loom {
    accessWidenerPath.set(project.generatedClassTweakerFile)

    decompilers {
        get("vineflower").apply {
            // Shows the method name of lambdas in a comment.
            options.put("mark-corresponding-synthetics", "1")
        }
    }

    runs {
        configureEach {
            name("${project.name} ${this.name.replaceFirstChar { it.titlecase() }} ${project.minecraftVersion}")
            runDir("../run")
            ideConfigGenerated(true)
            vmArgs(
                "-Xms1G",
                "-Xmx4G",
                "-Dmixin.debug.export=true",
                "-Dlog4j2.configurationFile=${
                    this@configureEach.javaClass.classLoader.getResource("log4j.xml")
                        ?: throw IllegalStateException("log4j.xml not found in plugin resources")
                }",
                "-Dfabric-tag-conventions-v2.missingTagTranslationWarning=silenced",
                "-Dfabric-tag-conventions-v1.legacyTagWarning=silenced",
                "-Dpuzzleslib.isDevelopmentEnvironment=true",
                "-D${mod.id}.isDevelopmentEnvironment=true"
            )

            if (System.getProperty("os.name").startsWith("Mac")) {
                vmArgs("-XstartOnFirstThread")
            }
        }

        named("client") {
            client()
        }

        named("server") {
            server()
            runDir("../run/server")
            programArgs("--nogui")
        }
    }
}

repositories {
    maven {
        name = "Modmuss"
        url = uri("https://maven.modmuss50.me/")
    }
    maven {
        name = "Ladysnake Libs"
        url = uri("https://maven.ladysnake.org/releases/")
    }
    maven {
        name = "jamieswhiteshirt"
        url = uri("https://maven.jamieswhiteshirt.com/libs-release/")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${versionCatalog.findVersion("game").get()}")
    implementation(versionCatalog.findLibrary("fabricloader.fabric").get())

    if (defaultDependencies) {
        versionCatalog.findLibrary("modmenu.fabric")
            .getOrNull()
            ?.let { localRuntime(it) { isTransitive = false } }
    }
}

val generateModJson = tasks.register<FabricModJsonV1Task>("generateModJson") {
    setupModJsonTask()
}

tasks.named<ProcessResources>(JvmConstants.PROCESS_RESOURCES_TASK_NAME) {
    dependsOn(generateModJson)
}

tasks.register("${project.name.lowercase()}-sources") {
    group = "multiloader/sources"
    val task = tasks.named("genSourcesWithVineflower")
    description = task.get().description
    dependsOn(task)
}

tasks.register("${project.name.lowercase()}-validate") {
    group = "multiloader/sources"
    val task = tasks.named("validateAccessWidener")
    description = task.get().description
    dependsOn(task)
}
