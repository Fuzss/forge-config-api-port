/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import java.nio.file.Path;
import org.jetbrains.annotations.Nullable;

record LoadedConfig(CommentedConfig config, @Nullable Path path, ModConfig modConfig) implements IConfigSpec.ILoadedConfig {
    @Override
    public void save() {
        if (path != null) {
            ConfigTracker.writeConfig(path, config);
        }
        modConfig.lock.lock();
        try {
            fuzs.forgeconfigapiport.fabric.impl.core.ModConfigEventsHelper.onReloading(modConfig);
        } finally {
            modConfig.lock.unlock();
        }
    }
}
