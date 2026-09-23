package fuzs.forgeconfigapiport.forge.impl.neoforge;

import fuzs.forgeconfigapiport.forge.api.v6.NeoForgeConfigRegistry;
import fuzs.forgeconfigapiport.impl.ForgeConfigAPIPort;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ModConfig;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class NeoForgeConfigRegistryImpl implements NeoForgeConfigRegistry {

    @Override
    public void register(String modId, ModConfig.Type type, IConfigSpec spec) {
        ModContainer modContainer = this.getModContainer(modId);
        this.register(modContainer, type, spec);
    }

    @Override
    public void register(ModContainer modContainer, ModConfig.Type type, IConfigSpec spec) {
        if (spec.isEmpty()) {
            // This handles the case where a mod tries to register a config, without any options configured inside it.
            ForgeConfigAPIPort.LOGGER.debug("Attempted to register an empty config for type {} on mod {}",
                    type,
                    modContainer.getModId());
        } else {
            ModConfig modConfig = new ModConfig(type,
                    new NeoForgeConfigAdapter(modContainer.getModId(), (ModConfigSpec) spec),
                    modContainer);
            modContainer.addConfig(modConfig);
        }
    }

    @Override
    public void register(String modId, ModConfig.Type type, IConfigSpec spec, String fileName) {
        ModContainer modContainer = this.getModContainer(modId);
        this.register(modContainer, type, spec, fileName);
    }

    @Override
    public void register(ModContainer modContainer, ModConfig.Type type, IConfigSpec spec, String fileName) {
        if (spec.isEmpty()) {
            // This handles the case where a mod tries to register a config, without any options configured inside it.
            ForgeConfigAPIPort.LOGGER.debug(
                    "Attempted to register an empty config for type {} on mod {} using file name {}",
                    type,
                    modContainer.getModId(),
                    fileName);
        } else {
            ModConfig modConfig = new ModConfig(type,
                    new NeoForgeConfigAdapter(modContainer.getModId(), (ModConfigSpec) spec),
                    modContainer,
                    fileName);
            modContainer.addConfig(modConfig);
        }
    }

    private ModContainer getModContainer(String modId) {
        return ModList.getModContainerById(modId)
                .orElseThrow(() -> new IllegalStateException("Invalid mod id '%s'".formatted(modId)));
    }
}
