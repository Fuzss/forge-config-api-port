package fuzs.forgeconfigapiport.fabric.impl.core;

import fuzs.forgeconfigapiport.fabric.api.v6.ConfigRegistry;
import fuzs.forgeconfigapiport.fabric.fml.config.ConfigTracker;
import fuzs.forgeconfigapiport.fabric.fml.config.ModConfig;
import fuzs.forgeconfigapiport.impl.ForgeConfigAPIPort;
import net.fabricmc.loader.api.FabricLoader;
import net.neoforged.fml.config.IConfigSpec;

public final class ConfigRegistryImpl implements ConfigRegistry {

    @Override
    public void register(String modId, ModConfig.Type type, IConfigSpec spec) {
        if (spec.isEmpty()) {
            // This handles the case where a mod tries to register a config, without any options configured inside it.
            ForgeConfigAPIPort.LOGGER.debug("Attempted to register an empty config for type {} on mod {}", type, modId);
        } else {
            ConfigTracker.INSTANCE.registerConfig(type,
                    spec,
                    FabricLoader.getInstance().getModContainer(modId).orElseThrow());
        }
    }

    @Override
    public void register(String modId, ModConfig.Type type, IConfigSpec spec, String fileName) {
        if (spec.isEmpty()) {
            // This handles the case where a mod tries to register a config, without any options configured inside it.
            ForgeConfigAPIPort.LOGGER.debug(
                    "Attempted to register an empty config for type {} on mod {} using file name {}",
                    type,
                    modId,
                    fileName);
        } else {
            ConfigTracker.INSTANCE.registerConfig(type,
                    spec,
                    FabricLoader.getInstance().getModContainer(modId).orElseThrow(),
                    fileName);
        }
    }

    @Override
    public void register(String modId, ModConfig.Type type, net.minecraftforge.fml.config.IConfigSpec<?> spec) {
        this.register(modId, type, new ForgeConfigSpecAdapter(spec));
    }

    @Override
    public void register(String modId, ModConfig.Type type, net.minecraftforge.fml.config.IConfigSpec<?> spec, String fileName) {
        this.register(modId, type, new ForgeConfigSpecAdapter(spec), fileName);
    }
}
