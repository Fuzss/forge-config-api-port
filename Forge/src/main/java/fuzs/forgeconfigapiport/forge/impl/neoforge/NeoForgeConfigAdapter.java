package fuzs.forgeconfigapiport.forge.impl.neoforge;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.core.utils.UnmodifiableConfigWrapper;
import com.electronwill.nightconfig.toml.TomlWriter;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A bridge for NeoForge's and Forge's config specs.
 */
public final class NeoForgeConfigAdapter extends UnmodifiableConfigWrapper<UnmodifiableConfig> implements net.minecraftforge.fml.config.IConfigSpec<NeoForgeConfigAdapter> {
    private static final Map<String, ReentrantLock> LOCKS_BY_MOD = new ConcurrentHashMap<>();

    private final ModConfigSpec spec;
    private final ReentrantLock lock;

    public NeoForgeConfigAdapter(String modId, ModConfigSpec spec) {
        super(spec.getSpec());
        this.spec = spec;
        this.lock = LOCKS_BY_MOD.computeIfAbsent(modId, $ -> new ReentrantLock());
        this.registerEventHandlers();
    }

    void registerEventHandlers() {
        ServerStoppingEvent.BUS.addListener((final ServerStoppingEvent evt) -> {
            // Reset WORLD type config caches
            ConfigTracker.fileMap().values().forEach(config -> {
                if (config.getSpec() == this) {
                    this.spec.resetCaches(ModConfigSpec.RestartType.WORLD);
                }
            });
        });
        if (FMLEnvironment.dist.isClient()) {
            ClientPlayerNetworkEvent.LoggingOut.BUS.addListener((final ClientPlayerNetworkEvent.LoggingOut event) -> {
                // Reset WORLD type config caches
                ConfigTracker.fileMap().values().forEach(config -> {
                    if (config.getSpec() == this) {
                        this.spec.resetCaches(ModConfigSpec.RestartType.WORLD);
                    }
                });

                // Unload SERVER configs only when disconnecting from a remote server
                if (event.getConnection() != null && !event.getConnection().isMemoryConnection()) {
                    ConfigTracker.unloadConfigs(ModConfig.Type.SERVER, FMLPaths.CONFIGDIR.get());
                }
            });
        }
    }

    @Override
    public void acceptConfig(@Nullable CommentedConfig data) {
        this.lock.lock();
        this.spec.acceptConfig(data != null ? new IConfigSpec.ILoadedConfig() {

            @Override
            public CommentedConfig config() {
                return data;
            }

            @Override
            public void save() {
                if (data instanceof FileConfig fileConfig) {
                    // copied from NeoForge ConfigTracker::writeConfig
                    new TomlWriter().write(data, fileConfig.getNioPath(), WritingMode.REPLACE_ATOMIC);
                }
            }
        } : null);
        this.lock.unlock();
    }

    @Override
    public boolean isCorrecting() {
        return this.lock.isLocked();
    }

    @Override
    public boolean isCorrect(CommentedConfig commentedFileConfig) {
        return this.spec.isCorrect(commentedFileConfig);
    }

    @Override
    public int correct(CommentedConfig commentedFileConfig) {
        this.lock.lock();
        this.spec.correct(commentedFileConfig);
        this.lock.unlock();
        // return value is never used
        return 0;
    }

    @Override
    public void afterReload() {
        this.spec.afterReload();
    }
}
