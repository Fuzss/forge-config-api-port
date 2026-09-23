package fuzs.forgeconfigapiport.fabric.impl.network;

import fuzs.forgeconfigapiport.fabric.fml.config.ConfigTracker;
import fuzs.forgeconfigapiport.fabric.neoforge.network.payload.ConfigFilePayload;
import fuzs.forgeconfigapiport.impl.ForgeConfigAPIPort;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;

/**
 * Fabric-side config sync handling that has no NeoForge counterpart (NeoForge performs this in
 * {@code NetworkRegistry::initializeNonModdedConnection}).
 */
public final class ConfigSyncHelper {
    private ConfigSyncHelper() {
        // NO-OP
    }

    public static void handleClientLoginSuccess() {
        if (ClientConfigurationNetworking.canSend(ConfigFilePayload.TYPE)) {
            ForgeConfigAPIPort.LOGGER.debug("Connected to a modded server.");
        } else {
            ForgeConfigAPIPort.LOGGER.debug("Connected to a vanilla server. Catching up missing behaviour.");
            ConfigTracker.INSTANCE.loadDefaultServerConfigs();
        }
    }
}
