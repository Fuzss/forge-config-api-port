package fuzs.multiloader

import fuzs.multiloader.classtweaker.*
import org.gradle.api.internal.tasks.JvmConstants

plugins {
    id("fuzs.multiloader.multiloader-convention-plugins-core")
    id("net.neoforged.moddev")
}

generateAccessTransformerFile(classTweakerFile, generatedAccessTransformerFile.get().asFile)

configurations {
    named("accessTransformers") {
        extendsFrom(
            named("modApi").get(),
            named("modImplementation").get(),
            named("modCompileOnly").get(),
            named("modCompileOnlyApi").get()
        )
    }
}

neoForge {
    validateAccessTransformers.set(true)
    accessTransformers {
        files.setFrom(generatedAccessTransformerFile)
        publish(generatedTransitiveAccessTransformerFile)
    }
}

tasks.named<ProcessResources>(JvmConstants.PROCESS_RESOURCES_TASK_NAME) {
    exclude("**/*.classtweaker")
}

val generateTransitiveAccessTransformerFile =
    tasks.register<GenerateAccessTransformerTask>("generateTransitiveAccessTransformerFile") {
        inputFile.set(classTweakerFile)
        outputFile.set(generatedTransitiveAccessTransformerFile)
        accessLevels.set(TRANSITIVE_CLASS_TWEAKER_ACCESS_LEVELS)
    }

tasks.named("copyAccessTransformersPublications") {
    dependsOn(generateTransitiveAccessTransformerFile)
}
