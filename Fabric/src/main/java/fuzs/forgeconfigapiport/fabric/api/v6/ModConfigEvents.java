package fuzs.forgeconfigapiport.fabric.api.v6;

import fuzs.forgeconfigapiport.fabric.fml.config.ModConfig;
import fuzs.forgeconfigapiport.fabric.impl.core.ModConfigEventsHelper;
import net.fabricmc.fabric.api.event.Event;

import java.util.Objects;

/**
 * Mod config events adapted for Fabric's callback event style.
 */
public final class ModConfigEvents {

    private ModConfigEvents() {
        // NO-OP
    }

    /**
     * Access to mod-specific loading event.
     *
     * @param modId the mod id to access config event for
     * @return the loading event
     */
    public static Event<Loading> loading(String modId) {
        Objects.requireNonNull(modId, "mod id is null");
        return ModConfigEventsHelper.getLoadingEvent(modId);
    }

    /**
     * Access to mod-specific reloading event.
     *
     * @param modId the mod id to access config event for
     * @return the reloading event
     */
    public static Event<Reloading> reloading(String modId) {
        Objects.requireNonNull(modId, "mod id is null");
        return ModConfigEventsHelper.getReloadingEvent(modId);
    }

    /**
     * Access to mod-specific unloading event.
     *
     * @param modId the mod id to access config event for
     * @return the unloading event
     */
    public static Event<Unloading> unloading(String modId) {
        Objects.requireNonNull(modId, "mod id is null");
        return ModConfigEventsHelper.getUnloadingEvent(modId);
    }

    @FunctionalInterface
    public interface Loading {

        /**
         * Called when a config is loaded for the first time.
         * <p>
         * This happens when a config is first opened, and on clients after connecting to a server when default server
         * configs are loaded after not receiving config data from the server.
         *
         * @param modConfig the mod config that is loading
         */
        void onModConfigLoading(ModConfig modConfig);
    }

    @FunctionalInterface
    public interface Reloading {

        /**
         * Called when a config is reloaded.
         * <p>
         * This happens when an update to the config file is found by the
         * {@link com.electronwill.nightconfig.core.file.FileWatcher}, and when a synced config is received on the
         * client from connecting to a server.
         *
         * @param modConfig the mod config that is reloading
         */
        void onModConfigReloading(ModConfig modConfig);
    }

    @FunctionalInterface
    public interface Unloading {

        /**
         * Called when a config is unloaded.
         * <p>
         * This only happens for server configs when a server (integrated or dedicated) shuts down or when a player
         * disconnects from a remote server.
         *
         * @param modConfig the mod config that is unloading
         */
        void onModConfigUnloading(ModConfig modConfig);
    }
}
