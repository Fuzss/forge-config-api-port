package fuzs.forgeconfigapiport.fabric.impl.client.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import fuzs.forgeconfigapiport.fabric.fml.config.ModConfig;
import fuzs.forgeconfigapiport.fabric.fml.config.ModConfigs;
import fuzs.forgeconfigapiport.fabric.impl.client.commands.arguments.EnumArgument;
import fuzs.forgeconfigapiport.fabric.impl.client.commands.arguments.ModIdArgument;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

public class ClientConfigCommand {
    private static final Dynamic2CommandExceptionType ERROR_NO_CONFIG = new Dynamic2CommandExceptionType((Object modId, Object type) -> Component.translatable(
            "commands.config.noconfig",
            modId,
            type));

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("config")
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("showfile")
                        .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("mod",
                                        ModIdArgument.modIdArgument(ClientConfigCommand::hasAnyModConfig))
                                .then(RequiredArgumentBuilder.<FabricClientCommandSource, ModConfig.Type>argument("type",
                                                EnumArgument.enumArgument(ModConfig.Type.class))
                                        .executes(ClientConfigCommand::showFile)))));
    }

    static boolean hasAnyModConfig(String modId) {
        return Stream.of(ModConfig.Type.values())
                .flatMap((ModConfig.Type type) -> ModConfigs.getConfigFileNames(modId, type).stream())
                .findAny()
                .isPresent();
    }

    private static int showFile(final CommandContext<FabricClientCommandSource> context) throws CommandSyntaxException {
        final String modId = context.getArgument("mod", String.class);
        final ModConfig.Type type = context.getArgument("type", ModConfig.Type.class);
        List<String> configFileNames = ModConfigs.getConfigFileNames(modId, type);
        if (configFileNames.isEmpty()) {
            throw ERROR_NO_CONFIG.create(modId, type.getSerializedName());
        } else {
            for (String configFileName : configFileNames) {
                File f = new File(configFileName);
                MutableComponent fileComponent = Component.literal(f.getName()).withStyle(ChatFormatting.UNDERLINE);
                fileComponent.withStyle((style) -> style.withClickEvent(new ClickEvent.OpenFile(f)));
                context.getSource()
                        .sendFeedback(Component.translatable("commands.config.getwithtype",
                                modId,
                                type.getSerializedName(),
                                fileComponent));
            }
        }
        return configFileNames.size();
    }
}
