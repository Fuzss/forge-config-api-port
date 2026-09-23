package fuzs.forgeconfigapiport.fabric.impl.core;

import fuzs.forgeconfigapiport.fabric.api.v6.ModConfigEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.neoforged.fml.config.ModConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ModConfigEventsHelper {

    private ModConfigEventsHelper() {
        // NO-OP
    }

    public static void onLoading(ModConfig modConfig) {
        ModConfigEvents.loading(modConfig.getModId()).invoker().onModConfigLoading(modConfig);
    }

    public static void onReloading(ModConfig modConfig) {
        ModConfigEvents.reloading(modConfig.getModId()).invoker().onModConfigReloading(modConfig);
    }

    public static void onUnloading(ModConfig modConfig) {
        ModConfigEvents.unloading(modConfig.getModId()).invoker().onModConfigUnloading(modConfig);
    }

    public static Event<ModConfigEvents.Loading> getLoadingEvent(String modId) {
        return ConfigEvents.get(modId).loading();
    }

    public static Event<ModConfigEvents.Reloading> getReloadingEvent(String modId) {
        return ConfigEvents.get(modId).reloading();
    }

    public static Event<ModConfigEvents.Unloading> getUnloadingEvent(String modId) {
        return ConfigEvents.get(modId).unloading();
    }

    private record ConfigEvents(String modId,
                                Event<ModConfigEvents.Loading> loading,
                                Event<ModConfigEvents.Reloading> reloading,
                                Event<ModConfigEvents.Unloading> unloading) {
        private static final Map<String, ConfigEvents> HOLDERS_BY_MOD_ID = new ConcurrentHashMap<>();

        private ConfigEvents(String modId) {
            this(modId, createLoadingEvent(), createReloadingEvent(), createUnloadingEvent());
        }

        static ConfigEvents get(String modId) {
            return HOLDERS_BY_MOD_ID.computeIfAbsent(modId, ConfigEvents::new);
        }

        static Event<ModConfigEvents.Loading> createLoadingEvent() {
            return EventFactory.createArrayBacked(ModConfigEvents.Loading.class, callbacks -> (config) -> {
                for (ModConfigEvents.Loading callback : callbacks) {
                    callback.onModConfigLoading(config);
                }
            });
        }

        static Event<ModConfigEvents.Reloading> createReloadingEvent() {
            return EventFactory.createArrayBacked(ModConfigEvents.Reloading.class, callbacks -> (config) -> {
                for (ModConfigEvents.Reloading callback : callbacks) {
                    callback.onModConfigReloading(config);
                }
            });
        }

        static Event<ModConfigEvents.Unloading> createUnloadingEvent() {
            return EventFactory.createArrayBacked(ModConfigEvents.Unloading.class, callbacks -> (config) -> {
                for (ModConfigEvents.Unloading callback : callbacks) {
                    callback.onModConfigUnloading(config);
                }
            });
        }
    }
}
