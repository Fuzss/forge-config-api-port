package fuzs.forgeconfigapiport.forge.impl;

import fuzs.forgeconfigapiport.forge.api.v6.NeoForgeConfigRegistry;
import fuzs.forgeconfigapiport.impl.ForgeConfigAPIPort;
import fuzs.forgeconfigapiport.impl.services.CommonAbstractions;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.neoforge.common.ModConfigSpec;

@Mod(ForgeConfigAPIPort.MOD_ID)
public class ForgeConfigAPIPortForge {

    public ForgeConfigAPIPortForge(FMLJavaModLoadingContext context) {
        setupDevelopmentEnvironment(context);
    }

    private static void setupDevelopmentEnvironment(FMLJavaModLoadingContext context) {
        if (!CommonAbstractions.INSTANCE.isDevelopmentEnvironment(ForgeConfigAPIPort.MOD_ID)) {
            return;
        }

        NeoForgeConfigRegistry.INSTANCE.register(ForgeConfigAPIPort.MOD_ID,
                ModConfig.Type.SERVER,
                new ModConfigSpec.Builder().comment("hello world").define("dummy_entry", true).next().build(),
                "forgeconfigapiport-server-neoforge.toml");
        context.registerConfig(ModConfig.Type.SERVER,
                new ForgeConfigSpec.Builder().comment("hello world").define("dummy_entry", true).next().build(),
                "forgeconfigapiport-server-forge.toml");
    }
}
