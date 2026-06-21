import org.gradle.api.publish.maven.internal.publication.MavenPomInternal
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.w3c.dom.Element

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    java
    `java-library`
    `maven-publish`
    signing
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

version = providers.gradleProperty("mod.version").get()
group = providers.gradleProperty("mod.group").get()

repositories {
    gradlePluginPortal()
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net/")
    }
    maven {
        name = "NeoForge"
        url = uri("https://maven.neoforged.net/releases/")
    }
}

dependencies {
    implementation(libs.foojay.resolver.convention)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktoml)
    implementation(libs.fabric.loom)
    implementation(libs.mod.dev.gradle)
    implementation(libs.mod.publish.plugin)
    implementation(libs.spotless)
}

gradlePlugin {
    plugins {
        register("settings") {
            id = "${project.providers.gradleProperty("mod.group").get()}.${
                project.providers.gradleProperty("mod.id").get()
            }-settings"
            implementationClass = "fuzs.multiloader.plugin.SettingsConventionPlugin"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.java.get()))
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }

    withSourcesJar()
}

tasks.withType<Jar>().configureEach {
    from(project.file("CHANGELOG.md"))
    from(project.file("LICENSE.md"))
    manifest {
        attributes("Implementation-Version" to project.version)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(libs.versions.java.get().toInt())
}

publishing {
    repositories {
        project.providers.gradleProperty("fuzs.multiloader.project.resources").orNull
            ?.let { "$it/maven" }
            ?.let {
                maven {
                    name = "FuzsModResources"
                    url = uri(it)
                }
            }
    }
}

afterEvaluate {
    publishing {
        // The pluginMaven publication is only available after evaluation
        val pluginMaven = publications.named<MavenPublication>("pluginMaven") {
            artifactId = project.providers.gradleProperty("mod.id").get()
        }

        publications.withType<MavenPublication>().toList().forEach {
            publications.create<MavenPublication>("${it.name}Snapshot") {
                groupId = it.groupId
                artifactId = it.artifactId
                version = it.version.substringBeforeLast('.') + "-SNAPSHOT"

                // Copy additional artifacts, which includes anything set from components (if any)
                it.artifacts.forEach { artifact(it) }

                // Copy POM dependencies (if any)
                (pom as MavenPomInternal).dependencies.set((it.pom as MavenPomInternal).dependencies)

                // Manually add root project dependency for plugin markers, see org.gradle.plugin.devel.plugins.MavenPluginPublishPlugin
                if (it.artifactId.endsWith(".gradle.plugin")) {
                    pom.withXml {
                        val root: Element = asElement()
                        val document = root.getOwnerDocument()
                        val dependencies = root.appendChild(document.createElement("dependencies"))
                        val dependency = dependencies.appendChild(document.createElement("dependency"))
                        val groupId = dependency.appendChild(document.createElement("groupId"))
                        groupId.setTextContent(pluginMaven.get().groupId)
                        val artifactId = dependency.appendChild(document.createElement("artifactId"))
                        artifactId.setTextContent(pluginMaven.get().artifactId)
                        val version = dependency.appendChild(document.createElement("version"))
                        version.setTextContent(pluginMaven.get().version)
                    }
                }
            }
        }
    }
}

tasks.register("all-build") {
    group = "multiloader/build"
    val task = tasks.named("build")
    description = task.get().description
    dependsOn(task)
}

tasks.register("all-clean") {
    group = "multiloader/build"
    val task = tasks.named("clean")
    description = task.get().description
    dependsOn(task)
}

tasks.register("all-publish") {
    group = "multiloader/publish"
    val task = project.tasks.named("publishAllPublicationsToFuzsModResourcesRepository")
    description = task.get().description
    dependsOn(task)
}
