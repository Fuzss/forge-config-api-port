/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package fuzs.forgeconfigapiport.fabric.impl.network;

import fuzs.forgeconfigapiport.fabric.api.v6.ModConfigEvents;
import fuzs.forgeconfigapiport.fabric.impl.network.payload.ConfigFilePayload;
import fuzs.forgeconfigapiport.impl.ForgeConfigAPIPort;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

@ApiStatus.Internal
public final class ConfigSync {
    private static final Object lock = new Object();
    /**
     * Connection -> Config file name -> byte[] of the config serialized to TOML.
     *
     * <p>Pending config updates get sent to players in the PLAY phase only,
     * but start being tracked as soon as the {@link SyncConfig} configuration task runs. This ensures that all updates
     * during the configuration phase will eventually arrive to the clients.
     *
     * <p>Connections get removed when GC'ed thanks to the WeakHashMap.
     */
    private static final Map<Connection, Map<String, byte[]>> configsToSync = new WeakHashMap<>();

    private ConfigSync() {
    }

    public static void syncAllConfigs(ServerConfigurationPacketListener listener) {
        // Forge Config API Port: update for Fabric
        var connection = ((ServerCommonPacketListenerImpl) listener).connection;
        if (connection.isMemoryConnection()) {
            return; // Do not sync server configs with ourselves
        }

        synchronized (lock) {
            configsToSync.put(connection, new LinkedHashMap<>());
        }

        final Map<String, byte[]> configData = ModConfigs.getConfigSet(ModConfig.Type.SERVER)
                .stream()
                .collect(Collectors.toMap(ModConfig::getFileName, mc -> {
                    try {
                        return Files.readAllBytes(mc.getFullPath());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }));

        for (var entry : configData.entrySet()) {
            // Forge Config API Port: update for Fabric
            ((ServerCommonPacketListenerImpl) listener).send(ServerConfigurationNetworking.createClientboundPacket(new ConfigFilePayload(
                    entry.getKey(),
                    entry.getValue())));
        }
    }

    /**
     * Registers a listener for {@code ModConfigEvent.Reloading} for all mod busses, that will sync changes to server
     * configs to connected clients.
     */
    public static void registerEventListeners() {
        for (ModContainer modContainer : FabricLoader.getInstance().getAllMods()) {
            ModConfigEvents.reloading(modContainer.getMetadata().getId()).register(config -> {
                if (config.getType() != ModConfig.Type.SERVER) {
                    return;
                }
                var loadedConfig = config.getLoadedConfig();
                if (loadedConfig == null) {
                    return;
                }

                var configFormat = loadedConfig.config().configFormat();
                if (configFormat.isInMemory()) {
                    return; // An in-memory format indicates that we received this config from a server and shouldn't try to sync it
                }

                // Write config bytes and queue for syncing to all connected players.
                var bytes = configFormat.createWriter()
                        .writeToString(loadedConfig.config())
                        .getBytes(StandardCharsets.UTF_8);
                synchronized (lock) {
                    for (var toSync : configsToSync.values()) {
                        toSync.put(config.getFileName(), bytes);
                    }
                }
            });
        }
    }

    public static void syncPendingConfigs(MinecraftServer server) {
        synchronized (lock) {
            for (var player : server.getPlayerList().getPlayers()) {
                // Forge Config API Port: update for Fabric
                if (!ServerPlayNetworking.canSend(player.connection, ConfigFilePayload.TYPE)) {
                    continue; // Only sync configs to NeoForge clients supporting config sync
                }

                // Forge Config API Port: update for Fabric
                if (player.connection.connection.isMemoryConnection()) {
                    continue; // Do not sync server configs with ourselves
                }

                // Forge Config API Port: update for Fabric
                var toSync = configsToSync.get(player.connection.connection);
                if (toSync == null) {
                    // null for GameTestPlayer. Should not happen for normal players though.
                    if (player.getClass() == ServerPlayer.class) {
                        throw new IllegalStateException(
                                "configsToSync should contain an entry for player " + player.getName());
                    } else {
                        continue;
                    }
                }
                toSync.forEach((fileName, data) -> {
                    // Forge Config API Port: update for Fabric
                    player.connection.send(ServerPlayNetworking.createClientboundPacket(new ConfigFilePayload(fileName,
                            data)));
                });
                toSync.clear();
            }
        }
    }

    public static void receiveSyncedConfig(final byte[] contents, final String fileName) {
        Optional.ofNullable(ModConfigs.getFileMap().get(fileName))
                .ifPresent(mc -> ConfigTracker.acceptSyncedConfig(mc, contents));
    }

    // Forge Config API Port: custom method similar to NeoForge's NetworkRegistry::initializeNonModdedConnection
    public static void handleClientLoginSuccess() {
        if (ClientConfigurationNetworking.canSend(ConfigFilePayload.TYPE)) {
            ForgeConfigAPIPort.LOGGER.debug("Connected to a modded server.");
        } else {
            ForgeConfigAPIPort.LOGGER.debug("Connected to a vanilla server. Catching up missing behaviour.");
            ConfigTracker.INSTANCE.loadDefaultServerConfigs();
        }
    }
}
