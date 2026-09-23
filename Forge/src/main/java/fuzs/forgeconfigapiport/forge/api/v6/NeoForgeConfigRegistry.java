package fuzs.forgeconfigapiport.forge.api.v6;

import fuzs.forgeconfigapiport.forge.impl.neoforge.NeoForgeConfigRegistryImpl;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.config.ModConfig;
import net.neoforged.fml.config.IConfigSpec;

/**
 * Registry for adding configs built using {@link net.neoforged.neoforge.common.ModConfigSpec}. Serves as a bridge
 * between NeoForge's and Forge's config systems.
 * <p>
 * Methods must be called only during {@link net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent}, as would be
 * done for Forge's configs. Do not call during any of the setup events.
 */
public interface NeoForgeConfigRegistry {
    NeoForgeConfigRegistry INSTANCE = new NeoForgeConfigRegistryImpl();

    /**
     * Register a new mod config.
     *
     * @param modId mod id to register config for
     * @param type  type of this mod config
     * @param spec  the built config spec
     */
    void register(String modId, ModConfig.Type type, IConfigSpec spec);

    /**
     * Register a new mod config.
     *
     * @param modContainer mod container to register config for
     * @param type         type of this mod config
     * @param spec         the built config spec
     */
    void register(ModContainer modContainer, ModConfig.Type type, IConfigSpec spec);

    /**
     * Register a new mod config.
     *
     * @param modId    mod id to register config for
     * @param type     type of this mod config
     * @param spec     the built config spec
     * @param fileName file name to use instead of default
     */
    void register(String modId, ModConfig.Type type, IConfigSpec spec, String fileName);

    /**
     * Register a new mod config.
     *
     * @param modContainer mod container to register config for
     * @param type         type of this mod config
     * @param spec         the built config spec
     * @param fileName     file name to use instead of default
     */
    void register(ModContainer modContainer, ModConfig.Type type, IConfigSpec spec, String fileName);
}
