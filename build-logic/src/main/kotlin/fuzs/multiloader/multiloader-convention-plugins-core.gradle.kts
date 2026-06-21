package fuzs.multiloader

import fuzs.multiloader.extension.*
import fuzs.multiloader.metadata.LinkProvider
import fuzs.multiloader.mixin.MixinConfigJsonTask
import fuzs.multiloader.task.IncrementBuildNumber
import org.gradle.api.internal.tasks.JvmConstants
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

plugins {
    java
    `java-library`
    `maven-publish`
    signing
    idea
    id("me.modmuss50.mod-publish-plugin")
    id("com.diffplug.spotless")
}

extensions.create<MultiLoaderExtension>("multiloader")

base.archivesName = mod.name.replace("[^a-zA-Z]".toRegex(), "")
version = "v${mod.version}-mc${minecraftVersion}-${project.name}"
group = mod.group

repositories {
    mavenCentral()
    mavenLocal()

    maven {
        name = "Fuzs Mod Resources"
        url = uri("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/")
    }

    maven {
        name = "Parchment"
        url = uri("https://maven.parchmentmc.org")
    }

    maven {
        name = "Jared"
        url = uri("https://maven.blamejared.com/")
    }

    maven {
        name = "Jitpack"
        url = uri("https://jitpack.io")
    }

    maven {
        name = "Shedaniel"
        url = uri("https://maven.shedaniel.me/")
    }

    maven {
        name = "Wisp Forest"
        url = uri("https://maven.wispforest.io/releases/")
    }

    maven {
        name = "Su5eD"
        url = uri("https://maven.su5ed.dev/releases/")
    }

    maven {
        name = "Minecraft Forge"
        url = uri("https://maven.minecraftforge.net/")
    }

    maven {
        name = "Terraformers"
        url = uri("https://maven.terraformersmc.com/")
    }

    exclusiveContent {
        forRepository {
            maven {
                name = "AppleSkin"
                url = uri("https://maven.ryanliptak.com/")
            }
        }
        filter {
            includeGroup("squeek.appleskin")
        }
    }

    exclusiveContent {
        forRepository {
            maven {
                name = "KosmX"
                url = uri("https://maven.kosmx.dev/")
            }
        }
        filter {
            includeGroup("dev.kosmx.player-anim")
        }
    }

    maven {
        name = "RedlanceMinecraft"
        url = uri("https://repo.redlance.org/public")
    }

    exclusiveContent {
        forRepository {
            maven {
                name = "GeckoLib"
                url = uri("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
            }
        }
        filter {
            includeGroup("software.bernie.geckolib")
        }
    }

    exclusiveContent {
        forRepository {
            maven {
                name = "SmartBrainLib"
                url = uri("https://dl.cloudsmith.io/public/tslat/sbl/maven/")
            }
        }
        filter {
            includeGroup("net.tslat.smartbrainlib")
        }
    }

    repositories {
        exclusiveContent {
            forRepository {
                maven {
                    name = "Nucleoid"
                    url = uri("https://maven.nucleoid.xyz/releases")
                }
            }
            filter {
                @Suppress("UnstableApiUsage")
                includeGroupAndSubgroups("eu.pb4")
            }
        }
    }

    exclusiveContent {
        forRepository {
            maven {
                name = "CurseForge"
                url = uri("https://cursemaven.com/")
            }
        }
        filter {
            includeGroup("curse.maven")
        }
    }

    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven/")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

java {
    toolchain {
        val javaVersion = versionCatalog.findVersion("java").get().requiredVersion
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }

    withSourcesJar()
    withJavadocJar()
}

tasks.withType<Jar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(rootProject.file("LICENSE.md"))
    from(rootProject.file("LICENSE-ASSETS.md"))
    from(rootProject.file("CHANGELOG.md"))
    from(rootProject.file("metadata.json"))

    manifest {
        attributes(
            mapOf(
                "Specification-Title" to mod.name,
                "Specification-Version" to mod.version,
                "Specification-Vendor" to mod.authors.joinToString(", "),
                "Implementation-Title" to mod.name,
                "Implementation-Version" to mod.version,
                "Implementation-Vendor" to mod.authors.joinToString(", "),
                "Implementation-Timestamp" to ZonedDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"))
            )
        )

        metadata.links.firstOrNull { it.name == LinkProvider.GITHUB }
            ?.url()
            ?.let { attributes["Implementation-URL"] = it }

        attributes(
            mapOf(
                "Build-Jdk-Name" to System.getProperty("java.vm.name"),
                "Build-Jdk-Version" to System.getProperty("java.vm.version"),
                "Build-Jdk-Vendor" to System.getProperty("java.vm.vendor"),
                "Build-Jdk-Spec-Name" to System.getProperty("java.vm.specification.name"),
                "Build-Jdk-Spec-Version" to System.getProperty("java.vm.specification.version"),
                "Build-Jdk-Spec-Vendor" to System.getProperty("java.vm.specification.vendor"),
                "Build-Os-Name" to "${System.getProperty("os.name")} (${System.getProperty("os.arch")})",
                "Build-Os-Version" to System.getProperty("os.version")
            )
        )
    }

    group = "jar"
}

tasks.withType<JavaCompile>().configureEach {
    // Ensure that the encoding is set to UTF-8, no matter what the system default is.
    // This fixes some edge cases with special characters not displaying correctly.
    // See: http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
    // If Javadoc is generated, this must be specified in that task too.
    options.encoding = "UTF-8"
    options.release.set(versionCatalog.findVersion("java").get().requiredVersion.toInt())
    // Disables general compiler warnings.
    options.isWarnings = false
}

tasks.withType<Javadoc>().configureEach {
    // Workaround cast for: https://github.com/gradle/gradle/issues/7038
    val standardJavadocDocletOptions = options as StandardJavadocDocletOptions
    // Prevent Java 8's strict doclint for Javadocs from failing builds.
    standardJavadocDocletOptions.addStringOption("Xdoclint:none", "-quiet")
}

tasks.withType<AbstractArchiveTask>().configureEach {
    // Activate reproducible builds:
    // https://docs.gradle.org/current/userguide/working_with_files.html#sec:reproducible_archives
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

idea {
    module {
        // IDEA no longer automatically downloads Sources / Javadoc jars for dependencies.
        // Now we need to explicitly enable the behavior.
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME) {
    resources {
        srcDir(project.layout.projectDirectory.dir("src/generated/resources"))
    }
}

configurations {
    create("commonJava") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }

    create("commonResources") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }

    create("modApi") {
        this@configurations.named("api") {
            extendsFrom(this@create)
        }
    }

    create("modImplementation") {
        this@configurations.named("implementation") {
            extendsFrom(this@create)
        }
    }

    create("modCompileOnly") {
        this@configurations.named("compileOnly") {
            extendsFrom(this@create)
        }
    }

    create("modCompileOnlyApi") {
        this@configurations.named("compileOnlyApi") {
            extendsFrom(this@create)
        }
    }

    create("modRuntimeOnly") {
        this@configurations.named("runtimeOnly") {
            extendsFrom(this@create)
        }
    }
}

artifacts {
    val main by sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME)
    main.java.srcDirs.forEach { add("commonJava", it) }
    main.resources.srcDirs.forEach { add("commonResources", it) }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "${mod.id}-${project.name.lowercase()}"
            version = mod.version
            groupId = mod.group

            from(components["java"])

            pom {
                name.set("${mod.name} [${project.name}]")
                description.set(mod.description)
                metadata.links.firstOrNull { it.name == LinkProvider.GITHUB }
                    ?.url()
                    ?.let {
                        url.set(it)

                        scm {
                            url.set(it)
                            connection.set(it.replace("https", "scm:git:git") + ".git")
                            developerConnection.set(
                                it.replace(
                                    "https://github.com/",
                                    "scm:git:git@github.com:"
                                ) + ".git"
                            )
                        }

                        issueManagement {
                            system.set("github")
                            url.set("${it}/issues")
                        }
                    }

                licenses {
                    license {
                        name.set(mod.license)
                        url.set("https://spdx.org/licenses/${mod.license}.html")
                    }
                }

                developers {
                    for (author in mod.authors) {
                        developer {
                            id.set(author.lowercase())
                            name.set(author)
                        }
                    }
                }
            }
        }
    }

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

signing {
    sign(publishing.publications.named<MavenPublication>("mavenJava").get())
}

val generateMixinConfig = tasks.register<MixinConfigJsonTask>("generateMixinConfig") {
    val multiLoaderExtension = project.extensions.getByType(MultiLoaderExtension::class.java)
    outputFile.set(project.layout.buildDirectory.file("generated/resources/${mod.id}.${project.name.lowercase()}.mixins.json"))

    json {
        mixinPackage.set("${project.group}.${project.packageName}.mixin")
        minVersion.set("0.8.0")
        required.set(true)
        compatibilityLevel.set("JAVA_${project.versionCatalog.findVersion("java").get().requiredVersion}")
        injectors {
            defaultRequire.set(1)
        }

        overwrites {
            requireAnnotations.set(true)
        }

        mixinExtras {
            minVersion.set("0.5.0")
        }

        multiLoaderExtension.mixins.orNull?.execute(this)
    }
}

tasks.named<ProcessResources>(JvmConstants.PROCESS_RESOURCES_TASK_NAME) {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(generateMixinConfig)
    from(project.layout.buildDirectory.dir("generated/resources"))
}

val copyDevelopmentJar = tasks.register<Copy>("copyDevelopmentJar") {
    val buildOutput = providers.gradleProperty("fuzs.multiloader.build.output")
    onlyIf { buildOutput.isPresent }

    val incrementBuildNumber = rootProject.tasks.named<IncrementBuildNumber>("incrementBuildNumber")
    dependsOn(incrementBuildNumber)

    val jarTask = tasks.named<AbstractArchiveTask>(JvmConstants.JAR_TASK_NAME)
    dependsOn(jarTask)

    if (buildOutput.isPresent) {
        from(jarTask.flatMap { it.archiveFile })
        into(buildOutput.get())

        // This runs at configuration time before the properties file is updated.
        // But that is fine as the value from the last run is still unique.
        val buildPropertiesFile = rootProject.layout.buildDirectory.file("build.properties").get().asFile
        val projectBuildNumber = Properties().apply {
            if (buildPropertiesFile.exists()) load(buildPropertiesFile.inputStream())
        }.getProperty("project.build") ?: "1"
        val oldValue = "v${mod.version}-mc"
        val newValue = "v${mod.version}-dev.${projectBuildNumber}-mc"

        rename { it.replace(oldValue, newValue) }
    }
}

tasks.named("build") {
    finalizedBy(copyDevelopmentJar)
}

spotless {
    // Prevent Gradle's check task from running spotlessCheck
    isEnforceCheck = false

    java {
        endWithNewline()
        removeUnusedImports()
    }

    format("TinyTakeover") {
        target("src/main/java/**/*.java")

        replaceRegex(
            "Change GuiGraphics to GuiGraphicsExtractor",
            "\\bGuiGraphics\\b",
            "GuiGraphicsExtractor"
        )

        replaceRegex(
            "Update CameraRenderState import",
            "\\bimport\\s+net\\.minecraft\\.client\\.renderer\\.state\\.CameraRenderState;",
            "import net.minecraft.client.renderer.state.level.CameraRenderState;"
        )

        replaceRegex(
            "Update BlockStateModel import",
            "\\bimport\\s+net\\.minecraft\\.client\\.renderer\\.block\\.model\\.BlockStateModel;",
            "import net.minecraft.client.renderer.block.dispatch.BlockStateModel;"
        )

        replaceRegex(
            "Update Puzzles Lib common import",
            "\\bimport\\s+fuzs\\.puzzleslib\\.api\\.",
            "import fuzs.puzzleslib.common.api."
        )
    }

    format("MountsOfMayhem") {
        target("src/main/java/**/*.java")

        replaceRegex(
            "Update @Nullable import",
            "\\bimport\\s+org\\.jetbrains\\.annotations\\.Nullable;",
            "import org.jspecify.annotations.Nullable;"
        )

        replaceRegex(
            "Update @NotNull import",
            "\\bimport\\s+org\\.jetbrains\\.annotations\\.NotNull;",
            "import org.jspecify.annotations.NonNull;"
        )

        replaceRegex(
            "Change @NotNull to @NonNull",
            "@NotNull\\b",
            "@NonNull"
        )

        replaceRegex(
            "Update Util import",
            "\\bimport\\s+net\\.minecraft\\.Util;",
            "import net.minecraft.util.Util;"
        )

        replaceRegex(
            "Update ResourceLocation import",
            "\\bimport\\s+net\\.minecraft\\.resources\\.ResourceLocation;",
            "import net.minecraft.resources.Identifier;"
        )

        replaceRegex(
            "Change ResourceLocation to Identifier",
            "\\bResourceLocation\\b",
            "Identifier"
        )

        replaceRegex(
            "Update ResourceLocation variables and comments",
            "\\b(resourceLocation|resource location)\\b",
            "identifier"
        )

        replaceRegex(
            "Update ResourceLocationHelper import",
            "\\bimport\\s+fuzs\\.puzzleslib\\.api\\.core\\.v1\\.utility\\.ResourceLocationHelper;",
            "import net.minecraft.resources.Identifier;"
        )

        replaceRegex(
            "Change ResourceLocationHelper to Identifier",
            "\\bResourceLocationHelper\\b",
            "Identifier"
        )

        replaceRegex(
            "Update RenderType import",
            "\\bimport\\s+net\\.minecraft\\.client\\.renderer\\.RenderType;",
            "import net.minecraft.client.renderer.rendertype.RenderType;"
        )

        replaceRegex(
            "Update Advancement Criterion import",
            "\\bimport\\s+net\\.minecraft\\.advancements\\.critereon\\.",
            "import net.minecraft.advancements.criterion."
        )
    }

    format("TheCopperAge") {
        target("src/main/java/**/*.java")

        replaceRegex(
            "Revert @Nullable import",
            "\\bimport\\s+org\\.jspecify\\.annotations\\.Nullable;",
            "import org.jetbrains.annotations.Nullable;"
        )

        replaceRegex(
            "Revert @NonNull import",
            "\\bimport\\s+org\\.jspecify\\.annotations\\.NonNull;",
            "import org.jetbrains.annotations.NotNull;"
        )

        replaceRegex(
            "Change @NonNull back to @NotNull",
            "@NonNull\\b",
            "@NotNull"
        )

        replaceRegex(
            "Revert Util import",
            "\\bimport\\s+net\\.minecraft\\.util\\.Util;",
            "import net.minecraft.Util;"
        )

        replaceRegex(
            "Revert Identifier import to ResourceLocation",
            "\\bimport\\s+net\\.minecraft\\.resources\\.Identifier;",
            "import net.minecraft.resources.ResourceLocation;"
        )

        replaceRegex(
            "Change Identifier back to ResourceLocation",
            "\\bIdentifier\\b",
            "ResourceLocation"
        )

        replaceRegex(
            "Revert RenderType import",
            "\\bimport\\s+net\\.minecraft\\.client\\.renderer\\.rendertype\\.RenderType;",
            "import net.minecraft.client.renderer.RenderType;"
        )

        replaceRegex(
            "Revert Advancement Criterion import",
            "\\bimport\\s+net\\.minecraft\\.advancements\\.criterion\\.",
            "import net.minecraft.advancements.critereon."
        )
    }
}

tasks.register("${project.name.lowercase()}-build") {
    group = "multiloader/build"
    val task = tasks.named("build")
    description = task.get().description
    dependsOn(task)
}

tasks.register("${project.name.lowercase()}-clean") {
    group = "multiloader/build"
    val task = tasks.named("clean")
    description = task.get().description
    dependsOn(task)
}

tasks.register("${project.name.lowercase()}-publish") {
    group = "multiloader/publish"
    val task = tasks.named("publishMavenJavaPublicationToFuzsModResourcesRepository")
    description = task.get().description
    dependsOn(task)
}

tasks.register("${project.name.lowercase()}-java-apply") {
    group = "multiloader/spotless"
    val task = tasks.named("spotlessJavaApply")
    description = task.get().description
    dependsOn(task)
}

tasks.register("${project.name.lowercase()}-java-check") {
    group = "multiloader/spotless"
    val task = tasks.named("spotlessJavaCheck")
    description = task.get().description
    dependsOn(task)
}

tasks.register("${project.name.lowercase()}-tinytakeover-apply") {
    group = "multiloader/spotless"
    val task = tasks.named("spotlessTinyTakeoverApply")
    description = task.get().description
    dependsOn(task)
}

tasks.register("${project.name.lowercase()}-tinytakeover-check") {
    group = "multiloader/spotless"
    val task = tasks.named("spotlessTinyTakeoverCheck")
    description = task.get().description
    dependsOn(task)
}

tasks.register("${project.name.lowercase()}-mountsofmayhem-apply") {
    group = "multiloader/spotless"
    val task = tasks.named("spotlessMountsOfMayhemApply")
    description = task.get().description
    dependsOn(task)
}

tasks.register("${project.name.lowercase()}-mountsofmayhem-check") {
    group = "multiloader/spotless"
    val task = tasks.named("spotlessMountsOfMayhemCheck")
    description = task.get().description
    dependsOn(task)
}

tasks.register("${project.name.lowercase()}-thecopperage-apply") {
    group = "multiloader/spotless"
    val task = tasks.named("spotlessTheCopperAgeApply")
    description = task.get().description
    dependsOn(task)
}

tasks.register("${project.name.lowercase()}-thecopperage-check") {
    group = "multiloader/spotless"
    val task = tasks.named("spotlessTheCopperAgeCheck")
    description = task.get().description
    dependsOn(task)
}
