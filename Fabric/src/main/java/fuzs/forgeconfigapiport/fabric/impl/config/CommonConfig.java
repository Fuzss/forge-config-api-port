/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package fuzs.forgeconfigapiport.fabric.impl.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.InMemoryFormat;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.mojang.logging.LogUtils;
import fuzs.forgeconfigapiport.impl.ForgeConfigAPIPort;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * See {@code net.minecraftforge.fml.loading.FMLConfig}
 */
public class CommonConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ConfigSpec SPEC = new ConfigSpec(InMemoryFormat.withUniversalSupport()
            .createConfig(LinkedHashMap::new));
    private static final CommentedConfig COMMENTS = CommentedConfig.inMemory();
    private static final CommonConfig INSTANCE;

    static {
        for (ConfigValue<?> configValue : ConfigValue.VALUES) {
            configValue.buildConfigEntry(SPEC, COMMENTS);
        }

        INSTANCE = new CommonConfig();
    }

    @Nullable
    private CommentedFileConfig configData;

    private CommonConfig() {
        // NO-OP
    }

    private void loadFrom(Path configFile) {
        this.configData = CommentedFileConfig.builder(configFile)
                .sync()
                .onFileNotFound(FileNotFoundAction.CREATE_EMPTY)
                .writingMode(WritingMode.REPLACE)
                .build();
        try {
            this.configData.load();
        } catch (ParsingException exception) {
            try {
                Files.delete(this.configData.getNioPath());
                this.configData.load();
                LOGGER.warn("Configuration file {} could not be parsed. Correcting",
                        this.configData.getNioPath(),
                        exception);
            } catch (ParsingException ignored) {
                // don't let this fail just because some random rarely used config cannot be properly loaded
            } catch (Throwable throwable) {
                throw new RuntimeException(
                        "Failed to load " + ForgeConfigAPIPort.MOD_NAME + " config from " + configFile, throwable);
            }
        }

        if (!SPEC.isCorrect(this.configData)) {
            LOGGER.warn("Configuration file {} is not correct. Correcting", configFile);
            SPEC.correct(this.configData,
                    (action, path, incorrectValue, correctedValue) -> LOGGER.info(
                            "Incorrect key {} was corrected from {} to {}",
                            path,
                            incorrectValue,
                            correctedValue));
        }

        this.configData.putAllComments(COMMENTS);
        this.configData.save();
    }

    public static void load() {
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve(ForgeConfigAPIPort.MOD_ID + ".toml");
        INSTANCE.loadFrom(configFile);
        LOGGER.trace("Loaded {} config from {}", ForgeConfigAPIPort.MOD_NAME, configFile);
        for (ConfigValue<?> configValue : ConfigValue.VALUES) {
            LOGGER.trace("{} {} is {}",
                    ForgeConfigAPIPort.MOD_NAME,
                    configValue.entry,
                    configValue.getConfigValue(INSTANCE.configData));
        }

        getOrCreateGameRelativePath(Paths.get(getConfigValue(ConfigValue.DEFAULT_CONFIG_PATH)));
    }

    public static <T> T getConfigValue(ConfigValue<T> configValue) {
        if (INSTANCE.configData == null) {
            load();
        }

        return configValue.getConfigValue(INSTANCE.configData);
    }

    public static Path getDefaultConfigsDirectory() {
        return FabricLoader.getInstance().getGameDir().resolve(getConfigValue(ConfigValue.DEFAULT_CONFIG_PATH));
    }

    /**
     * See {@code net.neoforged.fml.loading.FMLPaths}.
     */
    private static Path getOrCreateGameRelativePath(Path path) {
        Path gameFolderPath = FabricLoader.getInstance().getGameDir().resolve(path);
        if (!Files.isDirectory(gameFolderPath)) {
            try {
                Files.createDirectories(gameFolderPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return gameFolderPath;
    }

    public static final class ConfigValue<T> {
        public static final ConfigValue<String> DEFAULT_CONFIG_PATH = new ConfigValue<>("defaultConfigPath",
                "defaultconfigs",
                "Path to load default configs from, intended for setting global server configs for newly created worlds, but also works when recreating client and common configs.");
        public static final ConfigValue<Boolean> DISABLE_CONFIG_WATCHER = new ConfigValue<>("disableConfigWatcher",
                Boolean.FALSE,
                "Disables File Watcher. Used to automatically update config if its file has been modified.");
        public static final ConfigValue<Boolean> LOG_UNTRANSLATED_CONFIGURATION_WARNINGS = new ConfigValue<>(
                "logUntranslatedConfigurationWarnings",
                Boolean.TRUE,
                "A config option mainly for developers. Logs out configuration values that do not have translations when running a client in a development environment.");
        static final List<ConfigValue<?>> VALUES = List.of(DEFAULT_CONFIG_PATH,
                DISABLE_CONFIG_WATCHER,
                LOG_UNTRANSLATED_CONFIGURATION_WARNINGS);

        private final String entry;
        private final T defaultValue;
        private final String comment;
        private final UnaryOperator<T> entryFunction;

        ConfigValue(final String entry, final T defaultValue, final String comment) {
            this(entry, defaultValue, comment, UnaryOperator.identity());
        }

        ConfigValue(final String entry, final T defaultValue, final String comment, UnaryOperator<T> entryFunction) {
            this.entry = entry;
            this.defaultValue = defaultValue;
            this.comment = comment;
            this.entryFunction = entryFunction;
        }

        void buildConfigEntry(ConfigSpec spec, CommentedConfig config) {
            if (this.defaultValue instanceof List<?> list) {
                spec.defineList(this.entry, list, e -> e instanceof String);
            } else {
                spec.define(this.entry, this.defaultValue);
            }

            config.add(this.entry, this.defaultValue);
            config.setComment(this.entry, this.comment);
        }

        T getConfigValue(@Nullable CommentedConfig config) {
            return this.entryFunction.apply(
                    config != null && config.contains(this.entry) ? config.get(this.entry) : this.defaultValue);
        }

        void setConfigValue(CommentedConfig config, T value) {
            config.set(this.entry, value);
        }
    }
}
