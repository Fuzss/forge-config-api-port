package fuzs.multiloader

import fuzs.multiloader.extension.defaultDependencies
import fuzs.multiloader.extension.expectPlatform
import fuzs.multiloader.extension.versionCatalog
import fuzs.multiloader.metadata.ModLoaderProvider
import kotlin.jvm.optionals.getOrNull

plugins {
    id("fuzs.multiloader.multiloader-convention-plugins-neoforge-like")
}

project.expectPlatform(ModLoaderProvider.COMMON)

neoForge {
    neoFormVersion = versionCatalog.findVersion("neoform").get().requiredVersion
}

dependencies {
    compileOnly(versionCatalog.findLibrary("mixin.common").get())
    compileOnly(versionCatalog.findLibrary("mixinextras.common").get())

    if (defaultDependencies) {
        versionCatalog.findLibrary("multiloaderaccesswideners.common")
            .getOrNull()
            ?.let { accessTransformers(it) { isTransitive = false } }
    }
}
