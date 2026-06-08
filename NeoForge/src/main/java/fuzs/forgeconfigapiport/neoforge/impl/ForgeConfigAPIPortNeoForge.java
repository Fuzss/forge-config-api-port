package fuzs.forgeconfigapiport.neoforge.impl;

import fuzs.forgeconfigapiport.impl.ForgeConfigAPIPort;
import fuzs.forgeconfigapiport.impl.services.CommonAbstractions;
import fuzs.forgeconfigapiport.neoforge.api.v5.ForgeConfigRegistry;
import net.minecraft.DetectedVersion;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraftforge.common.ForgeConfigSpec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@Mod(ForgeConfigAPIPort.MOD_ID)
public class ForgeConfigAPIPortNeoForge {

    public ForgeConfigAPIPortNeoForge(ModContainer modContainer) {
        registerLoadingHandlers(modContainer.getEventBus());
        setupDevelopmentEnvironment(modContainer);
    }

    private static void registerLoadingHandlers(IEventBus eventBus) {
        eventBus.addListener((final GatherDataEvent.Client event) -> {
            // Set only the major version here to stay compatible across different minor Minecraft versions.
            event.getGenerator()
                    .addProvider(true,
                            new PackMetadataGenerator(event.getGenerator()
                                    .getPackOutput()).add(PackMetadataSection.SERVER_TYPE,
                                    new PackMetadataSection(Component.literal(event.getModContainer()
                                            .getModInfo()
                                            .getDescription()),
                                            PackFormat.of(DetectedVersion.BUILT_IN.packVersion(PackType.SERVER_DATA)
                                                    .major()).minorRange())));
        });
    }

    private static void setupDevelopmentEnvironment(ModContainer modContainer) {
        if (!CommonAbstractions.INSTANCE.isDevelopmentEnvironment(ForgeConfigAPIPort.MOD_ID)) {
            return;
        }

        modContainer.registerConfig(ModConfig.Type.SERVER,
                new ModConfigSpec.Builder().comment("hello world").define("dummy_entry", true).next().build(),
                "forgeconfigapiport-server-neoforge.toml");
        ForgeConfigRegistry.INSTANCE.register(modContainer.getModId(),
                ModConfig.Type.SERVER,
                new ForgeConfigSpec.Builder().translation("dummy_entry")
                        .comment("hello world")
                        .define("dummy_entry", true)
                        .next()
                        .build(),
                "forgeconfigapiport-server-forge.toml");
    }
}
