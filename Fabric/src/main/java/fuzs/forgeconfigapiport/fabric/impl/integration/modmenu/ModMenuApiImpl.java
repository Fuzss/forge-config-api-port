package fuzs.forgeconfigapiport.fabric.impl.integration.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import fuzs.forgeconfigapiport.fabric.api.v6.client.ConfigScreenFactoryRegistry;
import fuzs.forgeconfigapiport.fabric.impl.client.core.ConfigScreenFactoryRegistryImpl;
import fuzs.forgeconfigapiport.impl.ForgeConfigAPIPort;
import fuzs.forgeconfigapiport.impl.services.CommonAbstractions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

import java.util.Map;
import java.util.function.UnaryOperator;

public final class ModMenuApiImpl implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // cannot provide our own config screen factory below unfortunately
        if (CommonAbstractions.INSTANCE.isDevelopmentEnvironment(ForgeConfigAPIPort.MOD_ID)) {
            return (Screen screen) -> {
                return new ConfigurationScreen(FabricLoader.getInstance()
                        .getModContainer(ForgeConfigAPIPort.MOD_ID)
                        .orElseThrow(), screen);
            };
        } else {
            return ModMenuApi.super.getModConfigScreenFactory();
        }
    }

    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        return ((ConfigScreenFactoryRegistryImpl) ConfigScreenFactoryRegistry.INSTANCE).getConfigScreenFactories((UnaryOperator<Screen> operator) -> {
            return operator::apply;
        });
    }
}
