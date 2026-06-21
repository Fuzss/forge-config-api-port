package fuzs.multiloader.metadata

import kotlinx.serialization.Serializable

@Serializable
data class ExternalModMetadata(
    val mod: ExternalModEntry,
    val links: List<DistributionEntry>,
    val environments: EnvironmentsEntry,
    val platforms: List<ModLoaderProvider>
)

@Serializable
data class ExternalModEntry(
    val id: String,
    val name: String
)

data class ExternalMods(val mods: MutableMap<String, ExternalModMetadata>) {
    companion object {
        val ACCESSORIES: ExternalModMetadata = ExternalModMetadata(
            ExternalModEntry("accessories", "Accessories"),
            listOf(
                DistributionEntry(LinkProvider.CURSEFORGE, "accessories", "938917"),
                DistributionEntry(LinkProvider.MODRINTH, "accessories", "jtmvUHXj")
            ),
            EnvironmentsEntry(DependencyType.REQUIRED, DependencyType.REQUIRED),
            listOf(ModLoaderProvider.FABRIC, ModLoaderProvider.NEOFORGE)
        )
        val BIOLITH: ExternalModMetadata = ExternalModMetadata(
            ExternalModEntry("biolith", "Biolith"),
            listOf(
                DistributionEntry(LinkProvider.CURSEFORGE, "biolith", "852512"),
                DistributionEntry(LinkProvider.MODRINTH, "biolith", "iGEl6Crx")
            ),
            EnvironmentsEntry(DependencyType.REQUIRED, DependencyType.REQUIRED),
            listOf(ModLoaderProvider.FABRIC, ModLoaderProvider.NEOFORGE)
        )
        val CURIOS: ExternalModMetadata = ExternalModMetadata(
            ExternalModEntry("curios", "Curios"),
            listOf(
                DistributionEntry(LinkProvider.CURSEFORGE, "curios", "309927"),
                DistributionEntry(LinkProvider.MODRINTH, "curios", "vvuO3ImH")
            ),
            EnvironmentsEntry(DependencyType.REQUIRED, DependencyType.REQUIRED),
            listOf(ModLoaderProvider.NEOFORGE)
        )
        val FABRIC_API: ExternalModMetadata = ExternalModMetadata(
            ExternalModEntry("fabric-api", "Fabric API"),
            listOf(
                DistributionEntry(LinkProvider.CURSEFORGE, "fabric-api", "306612"),
                DistributionEntry(LinkProvider.MODRINTH, "fabric-api", "P7dR8mSH")
            ),
            EnvironmentsEntry(DependencyType.REQUIRED, DependencyType.REQUIRED),
            listOf(ModLoaderProvider.FABRIC)
        )
        val FORGE_CONFIG_API_PORT: ExternalModMetadata = ExternalModMetadata(
            ExternalModEntry("forgeconfigapiport", "Forge Config API Port"),
            listOf(
                DistributionEntry(LinkProvider.CURSEFORGE, "forge-config-api-port", "547434"),
                DistributionEntry(LinkProvider.MODRINTH, "forge-config-api-port", "ohNO6lps")
            ),
            EnvironmentsEntry(DependencyType.REQUIRED, DependencyType.REQUIRED),
            listOf(ModLoaderProvider.FABRIC)
        )
        val GECKOLIB: ExternalModMetadata = ExternalModMetadata(
            ExternalModEntry("geckolib", "GeckoLib"),
            listOf(
                DistributionEntry(LinkProvider.CURSEFORGE, "geckolib", "388172"),
                DistributionEntry(LinkProvider.MODRINTH, "geckolib", "8BmcQJ2H")
            ),
            EnvironmentsEntry(DependencyType.REQUIRED, DependencyType.REQUIRED),
            listOf(ModLoaderProvider.FABRIC, ModLoaderProvider.NEOFORGE)
        )
        val JUST_ENOUGH_ITEMS: ExternalModMetadata = ExternalModMetadata(
            ExternalModEntry("jei", "Just Enough Items"),
            listOf(
                DistributionEntry(LinkProvider.CURSEFORGE, "jei", "238222"),
                DistributionEntry(LinkProvider.MODRINTH, "jei", "u6dRKJwZ")
            ),
            EnvironmentsEntry(DependencyType.REQUIRED, DependencyType.REQUIRED),
            listOf(ModLoaderProvider.FABRIC, ModLoaderProvider.NEOFORGE)
        )
        val MOD_MENU: ExternalModMetadata = ExternalModMetadata(
            ExternalModEntry("modmenu", "Mod Menu"),
            listOf(
                DistributionEntry(LinkProvider.CURSEFORGE, "modmenu", "308702"),
                DistributionEntry(LinkProvider.MODRINTH, "modmenu", "mOgUt4GM")
            ),
            EnvironmentsEntry(DependencyType.REQUIRED, DependencyType.UNSUPPORTED),
            listOf(ModLoaderProvider.FABRIC)
        )
        val PLAYER_ANIMATION_LIBRARY: ExternalModMetadata = ExternalModMetadata(
            ExternalModEntry("player_animation_library", "Player Animation Library"),
            listOf(
                DistributionEntry(LinkProvider.CURSEFORGE, "player-animation-library", "1283899"),
                DistributionEntry(LinkProvider.MODRINTH, "player-animation-library", "ha1mEyJS")
            ),
            EnvironmentsEntry(DependencyType.REQUIRED, DependencyType.UNSUPPORTED),
            listOf(ModLoaderProvider.FABRIC)
        )
        val PUZZLES_LIB: ExternalModMetadata = ExternalModMetadata(
            ExternalModEntry("puzzleslib", "Puzzles Lib"),
            listOf(
                DistributionEntry(LinkProvider.CURSEFORGE, "puzzles-lib", "495476"),
                DistributionEntry(LinkProvider.MODRINTH, "puzzles-lib", "QAGBst4M")
            ),
            EnvironmentsEntry(DependencyType.REQUIRED, DependencyType.REQUIRED),
            listOf(ModLoaderProvider.FABRIC, ModLoaderProvider.NEOFORGE)
        )
        val ROUGHLY_ENOUGH_ITEMS: ExternalModMetadata = ExternalModMetadata(
            ExternalModEntry("roughlyenoughitems", "Roughly Enough Items"),
            listOf(
                DistributionEntry(LinkProvider.CURSEFORGE, "roughly-enough-items", "310111"),
                DistributionEntry(LinkProvider.MODRINTH, "rei", "nfn13YXA")
            ),
            EnvironmentsEntry(DependencyType.REQUIRED, DependencyType.REQUIRED),
            listOf(ModLoaderProvider.FABRIC, ModLoaderProvider.NEOFORGE)
        )
        val SMART_BRAIN_LIB: ExternalModMetadata = ExternalModMetadata(
            ExternalModEntry("smartbrainlib", "SmartBrainLib"),
            listOf(
                DistributionEntry(LinkProvider.CURSEFORGE, "smartbrainlib", "661293"),
                DistributionEntry(LinkProvider.MODRINTH, "smartbrainlib", "PuyPazRT")
            ),
            EnvironmentsEntry(DependencyType.REQUIRED, DependencyType.REQUIRED),
            listOf(ModLoaderProvider.FABRIC, ModLoaderProvider.NEOFORGE)
        )
        val TRINKETS: ExternalModMetadata = ExternalModMetadata(
            ExternalModEntry("trinkets_updated", "Trinkets"),
            listOf(
                DistributionEntry(LinkProvider.CURSEFORGE, "trinkets-updated", "1509777"),
                DistributionEntry(LinkProvider.MODRINTH, "trinkets-updated", "XaT8sLP6")
            ),
            EnvironmentsEntry(DependencyType.REQUIRED, DependencyType.REQUIRED),
            listOf(ModLoaderProvider.FABRIC)
        )
        val BY_ID = mapOf(
            "accessories" to ACCESSORIES,
            "biolith" to BIOLITH,
            "curios" to CURIOS,
            "fabricapi" to FABRIC_API,
            "forgeconfigapiport" to FORGE_CONFIG_API_PORT,
            "geckolib" to GECKOLIB,
            "rei" to JUST_ENOUGH_ITEMS,
            "modmenu" to MOD_MENU,
            "playeranimationlibrary" to PLAYER_ANIMATION_LIBRARY,
            "puzzleslib" to PUZZLES_LIB,
            "rei" to ROUGHLY_ENOUGH_ITEMS,
            "smartbrainlib" to SMART_BRAIN_LIB,
            "trinkets" to TRINKETS
        )
    }
}
