package fuzs.forgeconfigapiport.neoforge.impl.forge;

import fuzs.forgeconfigapiport.neoforge.api.v6.ForgeConfigRegistry;
import net.minecraftforge.fml.config.IConfigSpec;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;

public final class ForgeConfigRegistryImpl implements ForgeConfigRegistry {

    @Override
    public void register(String modId, ModConfig.Type type, IConfigSpec<?> spec) {
        ModContainer modContainer = this.getModContainer(modId);
        this.register(modContainer, type, spec);
    }

    @Override
    public void register(ModContainer modContainer, ModConfig.Type type, IConfigSpec<?> spec) {
        modContainer.registerConfig(type, new ForgeConfigAdapter(spec));
    }

    @Override
    public void register(String modId, ModConfig.Type type, IConfigSpec<?> spec, String fileName) {
        ModContainer modContainer = this.getModContainer(modId);
        this.register(modContainer, type, spec, fileName);
    }

    @Override
    public void register(ModContainer modContainer, ModConfig.Type type, IConfigSpec<?> spec, String fileName) {
        modContainer.registerConfig(type, new ForgeConfigAdapter(spec), fileName);
    }

    private ModContainer getModContainer(String modId) {
        return ModList.get()
                .getModContainerById(modId)
                .orElseThrow(() -> new IllegalStateException("Invalid mod id '%s'".formatted(modId)));
    }
}
