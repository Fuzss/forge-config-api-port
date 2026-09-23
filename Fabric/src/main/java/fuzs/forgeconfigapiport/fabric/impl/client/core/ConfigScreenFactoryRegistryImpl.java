package fuzs.forgeconfigapiport.fabric.impl.client.core;

import fuzs.forgeconfigapiport.fabric.api.v5.client.ConfigScreenFactoryRegistry;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public final class ConfigScreenFactoryRegistryImpl implements ConfigScreenFactoryRegistry {
    private final Map<String, UnaryOperator<Screen>> factories = new HashMap<>();

    @Override
    public void register(String modId) {
        this.register(modId, ConfigurationScreen::new);
    }

    @Override
    public void register(String modId, BiFunction<String, Screen, Screen> factory) {
        this.factories.put(modId, (Screen screen) -> factory.apply(modId, screen));
    }

    public <T> Map<String, T> getConfigScreenFactories(Function<UnaryOperator<Screen>, T> converter) {
        return this.factories.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, (Map.Entry<String, UnaryOperator<Screen>> entry) -> {
                    return converter.apply(entry.getValue());
                }));
    }
}
