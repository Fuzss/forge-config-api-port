package fuzs.forgeconfigapiport.fabric.impl.config;

import com.electronwill.nightconfig.core.Config;
import fuzs.forgeconfigapiport.fabric.fml.config.ModConfig;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.function.Consumer;

/**
 * Fabric-side home for the spec validation that {@code ModConfigSpec} cannot perform on the common module (it has no
 * access to {@link ModConfig}).
 */
public final class SpecValidator {
    private SpecValidator() {
        // NO-OP
    }

    public static void validateSpec(IConfigSpec spec, ModConfig modConfig) {
        if (spec instanceof ModConfigSpec) {
            forEachValue(((ModConfigSpec) spec).getValues().valueMap().values(), configValue -> {
                if (configValue.getSpec().restartType() == ModConfigSpec.RestartType.GAME
                        && modConfig.getType() == ModConfig.Type.SERVER) {
                    throw new IllegalArgumentException("Configuration value " + String.join(".", configValue.getPath())
                            + " defined in config " + modConfig.getFileName() + " has restart of type "
                            + configValue.getSpec().restartType() + " which cannot be used for configs of type "
                            + modConfig.getType());
                }
            });
        }
    }

    private static void forEachValue(Iterable<Object> configValues, Consumer<ModConfigSpec.ConfigValue<?>> consumer) {
        configValues.forEach(value -> {
            if (value instanceof ModConfigSpec.ConfigValue<?> configValue) {
                consumer.accept(configValue);
            } else if (value instanceof Config innerConfig) {
                forEachValue(innerConfig.valueMap().values(), consumer);
            }
        });
    }
}
