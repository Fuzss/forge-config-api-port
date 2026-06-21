package fuzs.multiloader

import fuzs.multiloader.discord.changelogVersion
import fuzs.multiloader.discord.verifyChangelogVersion
import fuzs.multiloader.extension.*
import fuzs.multiloader.metadata.DependencyType
import fuzs.multiloader.metadata.LinkProvider
import me.modmuss50.mpp.PublishModTask
import org.gradle.api.internal.tasks.JvmConstants

plugins {
    id("fuzs.multiloader.multiloader-convention-plugins-core")
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
    "commonJava"(project(mapOf("path" to project.commonProject.path, "configuration" to "commonJava")))
    "commonResources"(project(mapOf("path" to project.commonProject.path, "configuration" to "commonResources")))
    // This is only required for the IDE to see the common classes.
    compileOnly(project(project.commonProject.path)) { isTransitive = false }
}

tasks.named<JavaCompile>(JvmConstants.COMPILE_JAVA_TASK_NAME) {
    dependsOn(configurations.named("commonJava"))
    source(configurations.named("commonJava"))
}

tasks.named<ProcessResources>(JvmConstants.PROCESS_RESOURCES_TASK_NAME) {
    dependsOn(configurations.named("commonResources"))
    from(configurations.named("commonResources")) {
        exclude("**/*.classtweaker")
    }

    dependsOn(project.commonProject.tasks.named<ProcessResources>("processResources"))
    from(project.commonProject.layout.buildDirectory.dir("generated/resources")) {
        exclude("**/*.cfg")
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

tasks.withType<PublishModTask>().configureEach {
    notCompatibleWithConfigurationCache("The plugin stores a reference to the Gradle project object.")
    val versionString = project.changelogVersion
    doFirst {
        verifyChangelogVersion(file("../CHANGELOG.md"), versionString)
    }
}

publishMods {
    val changelogFile = file("../CHANGELOG.md")
    val changelogText = changelogFile.readText()

    val jarTask = tasks.named<AbstractArchiveTask>("jar")
    file.set(jarTask.get().archiveFile)

    displayName.set("[${name.uppercase()}] [${project.minecraftVersion}] v${mod.version}")
    type.set(STABLE)
    version.set(mod.version)
    modLoaders.add(projectPlatform.name.lowercase())
    dryRun.set(debugRemoteUploads)

    val minecraftVersion = versionCatalog.findVersion("minecraft").get().requiredVersion

    for (link in metadata.links) {
        val remoteToken =
            providers.gradleProperty("fuzs.multiloader.remote.${link.name.name.lowercase()}.token")

        if (remoteToken.isPresent) {
            when (link.name) {
                LinkProvider.CURSEFORGE -> {
                    curseforge {
                        accessToken.set(remoteToken)
                        projectId.set(link.id)
                        changelog.set(changelogText)
                        if (strictVersioning(minecraftVersion)) {
                            minecraftVersions.add(minecraftVersion)
                        } else {
                            minecraftVersions.addAll(supportedVersions(minecraftVersion))
                        }

                        val javaVersion = versionCatalog.findVersion("java").get().requiredVersion
                        javaVersions.add(JavaVersion.toVersion(javaVersion))

                        if (metadata.environments.forClient()) {
                            client.set(true)
                        }

                        if (metadata.environments.forServer()) {
                            server.set(true)
                        }

                        for (entry in metadata.dependencies) {
                            if (entry.platforms.any { it.matches(projectPlatform) }) {
                                rootProject.externalMods.mods[entry.name]?.links?.firstOrNull { it.name == link.name }?.slug.let {
                                    if (it != null) {
                                        when (entry.type) {
                                            DependencyType.REQUIRED -> requires(it)
                                            DependencyType.EMBEDDED -> embeds(it)
                                            DependencyType.OPTIONAL -> optional(it)
                                            DependencyType.UNSUPPORTED -> Unit
                                        }
                                    } else if (entry.type.mandatory) {
                                        throw GradleException("Unable to link dependency: $entry")
                                    }
                                }
                            }
                        }
                    }
                }

                LinkProvider.MODRINTH -> {
                    modrinth {
                        accessToken.set(remoteToken)
                        projectId.set(link.id)
                        changelog.set(changelogText)
                        if (strictVersioning(minecraftVersion)) {
                            minecraftVersions.add(minecraftVersion)
                        } else {
                            minecraftVersions.addAll(supportedVersions(minecraftVersion))
                        }

                        for (entry in metadata.dependencies) {
                            if (entry.platforms.any { it.matches(projectPlatform) }) {
                                rootProject.externalMods.mods[entry.name]?.links?.firstOrNull { it.name == link.name }?.slug.let {
                                    if (it != null) {
                                        when (entry.type) {
                                            DependencyType.REQUIRED -> requires(it)
                                            DependencyType.EMBEDDED -> embeds(it)
                                            DependencyType.OPTIONAL -> optional(it)
                                            DependencyType.UNSUPPORTED -> Unit
                                        }
                                    } else if (entry.type.mandatory) {
                                        throw GradleException("Unable to link dependency: $entry")
                                    }
                                }
                            }
                        }
                    }
                }

                LinkProvider.GITHUB -> {
                    github {
                        accessToken.set(remoteToken)
                        repository.set(link.url().replace("https://github.com/", ""))
                        commitish.set(project.minecraftVersion)
                        tagName.set("v${mod.version}-mc$minecraftVersion/${project.name.lowercase()}")

                        // Only include the relevant changelog section.
                        val changelogSections = changelogText.split(Regex("(?m)^## \\["), limit = 3)
                        changelog.set("## [" + changelogSections.getOrNull(1)?.trim())

                        val sourcesJar = tasks.named<Jar>("sourcesJar")
                        additionalFiles.from(sourcesJar.get().archiveFile)
                        val javadocJar = tasks.named<Jar>("javadocJar")
                        additionalFiles.from(javadocJar.get().archiveFile)
                    }
                }
            }
        }
    }
}

tasks.register("${project.name.lowercase()}-client") {
    group = "multiloader/run"
    val task = tasks.named("runClient")
    description = task.get().description
    dependsOn(task)
}

tasks.register("${project.name.lowercase()}-server") {
    group = "multiloader/run"
    val task = tasks.named("runServer")
    description = task.get().description
    dependsOn(task)
}

if (metadata.links.firstOrNull { it.name == LinkProvider.CURSEFORGE } != null) {
    tasks.register("${project.name.lowercase()}-curseforge") {
        group = "multiloader/remote"
        val task = tasks.named("publishCurseforge")
        description = task.get().description
        dependsOn(task)
    }
}

if (metadata.links.firstOrNull { it.name == LinkProvider.GITHUB } != null) {
    tasks.register("${project.name.lowercase()}-github") {
        group = "multiloader/remote"
        val task = tasks.named("publishGithub")
        description = task.get().description
        dependsOn(task)
    }
}

if (metadata.links.firstOrNull { it.name == LinkProvider.MODRINTH } != null) {
    tasks.register("${project.name.lowercase()}-modrinth") {
        group = "multiloader/remote"
        val task = tasks.named("publishModrinth")
        description = task.get().description
        dependsOn(task)
    }
}
