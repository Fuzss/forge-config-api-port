package fuzs.forgeconfigapiport.fabric.api.v5;

import fuzs.forgeconfigapiport.fabric.impl.core.ConfigRegistryImpl;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;

/**
 * Registry for adding your configs.
 * <p>
 * Note that opposed to NeoForge / Forge, configs are loaded and usable immediately after registration due to the lack
 * of mod loading stages on Fabric.
 * <p>
 * TODO Bump to v6 of the API when the NeoForge classes are moved to a separate Fabric-specific package.
 */
public interface ConfigRegistry {
    ConfigRegistry INSTANCE = new ConfigRegistryImpl();

    /**
     * Register a new NeoForge mod config.
     *
     * @param modId mod id of your mod
     * @param type  type of this mod config
     * @param spec  the built config spec
     */
    void register(String modId, ModConfig.Type type, IConfigSpec spec);

    /**
     * Register a new NeoForge mod config.
     *
     * @param modId    mod id of your mod
     * @param type     type of this mod config
     * @param spec     the built config spec
     * @param fileName file name to use instead of default
     */
    void register(String modId, ModConfig.Type type, IConfigSpec spec, String fileName);

    /**
     * Register a new Forge mod config.
     *
     * @param modId mod id of your mod
     * @param type  type of this mod config
     * @param spec  the built config spec
     */
    void register(String modId, ModConfig.Type type, net.minecraftforge.fml.config.IConfigSpec<?> spec);

    /**
     * Register a new Forge mod config.
     *
     * @param modId    mod id of your mod
     * @param type     type of this mod config
     * @param spec     the built config spec
     * @param fileName file name to use instead of default
     */
    void register(String modId, ModConfig.Type type, net.minecraftforge.fml.config.IConfigSpec<?> spec, String fileName);
}
