package fuzs.forgeconfigapiport.fabric.impl.client;

import fuzs.forgeconfigapiport.fabric.fml.config.ConfigTracker;
import fuzs.forgeconfigapiport.fabric.fml.config.ModConfig;
import fuzs.forgeconfigapiport.fabric.fml.config.ModConfigs;
import fuzs.forgeconfigapiport.fabric.impl.network.ConfigSyncHelper;
import fuzs.forgeconfigapiport.fabric.neoforge.network.ConfigSync;
import fuzs.forgeconfigapiport.fabric.neoforge.network.payload.ConfigFilePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientConfigurationPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ForgeConfigAPIPortFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        registerEventHandlers();
        registerMessages();
    }

    private static void registerEventHandlers() {
        ClientConfigurationConnectionEvents.COMPLETE.register((ClientConfigurationPacketListenerImpl handler, Minecraft client) -> {
            ConfigSyncHelper.handleClientLoginSuccess();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((ClientPacketListener handler, Minecraft client) -> {
            // Reset WORLD type config caches
            ModConfigs.getFileMap().values().forEach(config -> {
                if (config.getSpec() instanceof ModConfigSpec spec) {
                    spec.resetCaches(ModConfigSpec.RestartType.WORLD);
                }
            });

            // Unload SERVER configs only when disconnecting from a remote server
            if (handler.getConnection() != null && !handler.getConnection().isMemoryConnection()) {
                ConfigTracker.INSTANCE.unloadConfigs(ModConfig.Type.SERVER);
            }
        });
    }

    private static void registerMessages() {
        ClientConfigurationNetworking.registerGlobalReceiver(ConfigFilePayload.TYPE,
                (ConfigFilePayload payload, ClientConfigurationNetworking.Context context) -> {
                    if (!context.packetListener().connection.isMemoryConnection()) {
                        ConfigSync.receiveSyncedConfig(payload.contents(), payload.fileName());
                    }
                });
        ClientPlayNetworking.registerGlobalReceiver(ConfigFilePayload.TYPE,
                (ConfigFilePayload payload, ClientPlayNetworking.Context context) -> {
                    if (!context.client().getConnection().getConnection().isMemoryConnection()) {
                        ConfigSync.receiveSyncedConfig(payload.contents(), payload.fileName());
                    }
                });
    }
}
