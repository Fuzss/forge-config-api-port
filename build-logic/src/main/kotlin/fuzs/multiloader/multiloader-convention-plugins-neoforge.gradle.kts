package fuzs.multiloader

import fuzs.multiloader.extension.*
import fuzs.multiloader.metadata.ModLoaderProvider
import fuzs.multiloader.neoforge.setupModsTomlTask
import fuzs.multiloader.neoforge.toml.NeoForgeModsTomlTask
import org.gradle.api.internal.tasks.JvmConstants
import kotlin.jvm.optionals.getOrNull

plugins {
    id("fuzs.multiloader.multiloader-convention-plugins-neoforge-like")
    id("fuzs.multiloader.multiloader-convention-plugins-platform")
}

project.expectPlatform(ModLoaderProvider.NEOFORGE)

configurations {
    create("localRuntime") {
        isCanBeConsumed = false
        isCanBeResolved = false
        this@configurations.named("runtimeClasspath") {
            extendsFrom(this@create)
        }
    }

    create("modLocalRuntime") {
        isCanBeConsumed = false
        isCanBeResolved = false
        this@configurations.named("runtimeClasspath") {
            extendsFrom(this@create)
        }
    }

    create("include") {
        this@configurations.named("jarJar") {
            extendsFrom(this@create)
        }
    }
}

neoForge {
    enable {
        version = versionCatalog.findVersion("neoforge.version").get().requiredVersion
    }

    runs {
        configureEach {
            ideName.set("${project.name} ${this.name.replaceFirstChar { it.titlecase() }} ${project.minecraftVersion} (${project.path})")
            gameDirectory.set(rootProject.file("run"))
            jvmArguments.addAll(
                "-Xms1G",
                "-Xmx4G",
                "-Dmixin.debug.export=true",
                "-Dpuzzleslib.isDevelopmentEnvironment=true",
                "-D${mod.id}.isDevelopmentEnvironment=true"
            )

            if (System.getProperty("os.name").startsWith("Mac")) {
                jvmArguments.add("-XstartOnFirstThread")
            }

            // We cannot set the -Dlog4j2.configurationFile property, as MDG always overrides it.
            loggingConfigFile.set(
                project.providers.provider {
                    val output = project.layout.buildDirectory
                        .file("moddev/logging/${name}/log4j2.xml")
                        .get()

                    val file = output.asFile
                    if (!file.exists()) {
                        file.parentFile.mkdirs()

                        val text = this@configureEach.javaClass.classLoader
                            .getResource("log4j.xml")
                            ?.readText()
                            ?: error("log4j.xml not found in plugin resources")

                        file.writeText(text)
                    }

                    output
                }
            )
        }

        register("client") {
            client()
            programArguments.addAll("--username", "Player####")
        }

        register("server") {
            server()
            gameDirectory.set(rootProject.file("run/server"))
            programArguments.addAll("--nogui")
        }

        register("data") {
            clientData()
            programArguments.addAll(
                "--all",
                "--mod",
                mod.id,
                "--existing",
                project.commonProject.file("src/main/resources").absolutePath,
                "--output",
                project.commonProject.file("src/generated/resources").absolutePath
            )
        }

        mods {
            create(mod.id) {
                sourceSet(sourceSets.main.get())
            }
        }
    }
}

repositories {
    maven {
        name = "NeoForged"
        url = uri("https://maven.neoforged.net/releases/")
    }
    maven {
        name = "TheIllusiveC4"
        url = uri("https://maven.theillusivec4.top/")
    }
    maven {
        name = "OctoStudios"
        url = uri("https://maven.octo-studios.com/releases/")
    }
}

dependencies {
    if (defaultDependencies) {
        versionCatalog.findLibrary("bettermodsbutton.neoforge")
            .getOrNull()
            ?.let { "localRuntime"(it) { isTransitive = false } }
    }
}

val generateModsToml = tasks.register<NeoForgeModsTomlTask>("generateModsToml") {
    setupModsTomlTask()
}

tasks.named<ProcessResources>(JvmConstants.PROCESS_RESOURCES_TASK_NAME) {
    dependsOn(generateModsToml)
}

tasks.register("${project.name.lowercase()}-data") {
    group = "multiloader/run"
    val task = tasks.named("runData")
    description = task.get().description
    dependsOn(task)
}
