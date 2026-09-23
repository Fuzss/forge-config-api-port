package fuzs.forgeconfigapiport.fabric.api.v5.client;

import fuzs.forgeconfigapiport.fabric.impl.client.core.ConfigScreenFactoryRegistryImpl;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.BiFunction;

/**
 * A registry for enabling an in-game config screen for your mod via the <a
 * href="https://github.com/TerraformersMC/ModMenu">Mod Menu</a> mod (if present). The screen will be accessible from
 * the mod list in-game.
 * <p>
 * Generally, this should be used to enable NeoForge's built-in config screen from
 * {@link net.neoforged.neoforge.client.gui.ConfigurationScreen}, which can optionally be extended.
 * <p>
 * Please note that the NeoForge screen is only compatible with {@link net.neoforged.neoforge.common.ModConfigSpec}, no
 * other {@link net.neoforged.fml.config.IConfigSpec} implementation is supported.
 * <p>
 * TODO The factory should become BiFunction<ModContainer, Screen, Screen> when the API is bumped to v6.
 */
public interface ConfigScreenFactoryRegistry {
    ConfigScreenFactoryRegistry INSTANCE = new ConfigScreenFactoryRegistryImpl();

    /**
     * Registers a config screen factory for your mod.
     * <p>
     * This will use the default {@link net.neoforged.neoforge.client.gui.ConfigurationScreen}.
     *
     * @param modId the id of your mod
     */
    void register(String modId);

    /**
     * Registers a config screen factory for your mod.
     *
     * @param modId   the id of your mod
     * @param factory the config screen factory, incoming parameters are your mod id and the last screen (the mod list
     *                screen)
     */
    void register(String modId, BiFunction<String, Screen, Screen> factory);
}
