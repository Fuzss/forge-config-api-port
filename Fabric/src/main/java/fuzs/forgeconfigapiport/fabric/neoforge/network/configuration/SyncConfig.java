/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package fuzs.forgeconfigapiport.fabric.neoforge.network.configuration;

import java.util.function.Consumer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import fuzs.forgeconfigapiport.fabric.neoforge.network.ConfigSync;
import org.jetbrains.annotations.ApiStatus;

/**
 * Configuration task that syncs the config files to the client
 * 
 * @param listener the listener to indicate to that the task is complete
 */
@ApiStatus.Internal
public record SyncConfig(ServerConfigurationPacketListenerImpl listener) implements ICustomConfigurationTask {
    private static final Identifier ID = fuzs.forgeconfigapiport.impl.ForgeConfigAPIPort.id("sync_config");
    public static Type TYPE = new Type(ID.toString());

    @Override
    public void run(Consumer<CustomPacketPayload> sender) {
        ConfigSync.syncAllConfigs(listener);
        listener().completeTask(type());
    }

    @Override
    public Type type() {
        return TYPE;
    }
}
