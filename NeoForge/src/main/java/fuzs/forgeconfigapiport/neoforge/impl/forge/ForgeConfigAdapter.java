package fuzs.forgeconfigapiport.neoforge.impl.forge;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;
import net.minecraftforge.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.nio.file.Path;

/**
 * A bridge for NeoForge's and Forge's config specs.
 */
public record ForgeConfigAdapter(IConfigSpec<?> spec) implements net.neoforged.fml.config.IConfigSpec {

    @Override
    public boolean isEmpty() {
        return this.spec.isEmpty();
    }

    @Override
    public void validateSpec(ModConfig config) {
        // NO-OP
    }

    @Override
    public boolean isCorrect(UnmodifiableCommentedConfig config) {
        return this.spec.isCorrect((CommentedConfig) config);
    }

    @Override
    public void correct(CommentedConfig config) {
        this.spec.correct(config);
    }

    @Override
    public void acceptConfig(@Nullable net.neoforged.fml.config.IConfigSpec.ILoadedConfig loadedConfig) {
        CommentedConfig config;
        if (loadedConfig != null) {
            Path path = getConfigPath(loadedConfig);
            // if there is a path present in LoadedConfig::path this must be a file config, so it can be saved
            if (path != null) {
                config = new FileConfigWrapper(loadedConfig.config(), path, loadedConfig::save);
            } else {
                config = loadedConfig.config();
            }
        } else {
            config = null;
        }
        this.spec.acceptConfig(config);
    }

    @Nullable
    static Path getConfigPath(net.neoforged.fml.config.IConfigSpec.ILoadedConfig loadedConfig) {
        // looking for LoadedConfig::path
        for (Field field : loadedConfig.getClass().getDeclaredFields()) {
            if (field.getType() == Path.class) {
                field.setAccessible(true);
                try {
                    return (Path) field.get(loadedConfig);
                } catch (IllegalAccessException ignored) {
                    // NO-OP
                }
            }
        }

        return null;
    }
}
