pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = "Architectury"
            url = uri("https://maven.architectury.dev/")
        }
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "NeoForge"
            url = uri("https://maven.neoforged.net/releases/")
        }
        maven {
            name = "Minecraft Forge"
            url = uri("https://maven.minecraftforge.net/")
        }
        exclusiveContent {
            forRepository {
                maven {
                    name = "Fuzs Mod Resources"
                    url = uri("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/")
                }
            }
            filter {
                @Suppress("UnstableApiUsage")
                includeGroupAndSubgroups("fuzs.multiloader")
            }
        }
    }
}

plugins {
    id("fuzs.multiloader.multiloader-convention-plugins-settings") version providers.gradleProperty("project.plugins")
}
