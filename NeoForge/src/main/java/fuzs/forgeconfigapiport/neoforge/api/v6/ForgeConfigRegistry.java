package fuzs.forgeconfigapiport.neoforge.api.v6;

import fuzs.forgeconfigapiport.neoforge.impl.forge.ForgeConfigRegistryImpl;
import net.minecraftforge.fml.config.IConfigSpec;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

/**
 * Registry for adding configs built using {@link net.neoforged.neoforge.common.ModConfigSpec}. Serves as a bridge
 * between NeoForge's and Forge's config systems.
 * <p>
 * Methods must be called only during {@link net.neoforged.fml.event.lifecycle.FMLConstructModEvent}, as would be done
 * for Forge's configs. Do not call during any of the setup events.
 */
public interface ForgeConfigRegistry {
    ForgeConfigRegistry INSTANCE = new ForgeConfigRegistryImpl();

    /**
     * Register a new mod config.
     *
     * @param modId mod id to register config for
     * @param type  type of this mod config
     * @param spec  the built config spec
     */
    void register(String modId, ModConfig.Type type, IConfigSpec<?> spec);

    /**
     * Register a new mod config.
     *
     * @param modContainer mod container to register config for
     * @param type         type of this mod config
     * @param spec         the built config spec
     */
    void register(ModContainer modContainer, ModConfig.Type type, IConfigSpec<?> spec);

    /**
     * Register a new mod config.
     *
     * @param modId    mod id to register config for
     * @param type     type of this mod config
     * @param spec     the built config spec
     * @param fileName file name to use instead of default
     */
    void register(String modId, ModConfig.Type type, IConfigSpec<?> spec, String fileName);

    /**
     * Register a new mod config.
     *
     * @param modContainer mod container to register config for
     * @param type         type of this mod config
     * @param spec         the built config spec
     * @param fileName     file name to use instead of default
     */
    void register(ModContainer modContainer, ModConfig.Type type, IConfigSpec<?> spec, String fileName);
}
