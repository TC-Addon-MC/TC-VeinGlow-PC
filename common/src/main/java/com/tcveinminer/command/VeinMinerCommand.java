package com.tcveinminer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.network.NetworkManager;
import com.tcveinminer.network.payload.ConfigSyncData;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class VeinMinerCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tcveinminer")
            .requires(src -> src.hasPermission(2))
            .then(Commands.literal("config")
                .then(Commands.literal("reload")
                    .executes(ctx -> {
                        ConfigManager.load();
                        broadcastConfigSync(ctx.getSource().getServer().getPlayerList().getPlayers());
                        ctx.getSource().sendSuccess(() -> Component.literal("[TC VeinGlow] Config reloaded!"), false);
                        return 1;
                    })
                )
                .then(Commands.literal("set")
                    .then(Commands.argument("property", StringArgumentType.string())
                        .suggests((ctx, builder) -> {
                            List<String> fields = new ArrayList<>();
                            for (var f : ModConfig.class.getFields()) {
                                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                                    fields.add(f.getName());
                                }
                            }
                            return net.minecraft.commands.SharedSuggestionProvider.suggest(fields, builder);
                        })
                        .then(Commands.argument("value", StringArgumentType.string())
                            .suggests((ctx, builder) -> {
                                try {
                                    String prop = StringArgumentType.getString(ctx, "property");
                                    var field = ModConfig.class.getField(prop);
                                    if (field.getType() == boolean.class) {
                                        return net.minecraft.commands.SharedSuggestionProvider.suggest(
                                                List.of("true", "false"), builder);
                                    }
                                } catch (Exception ignored) {}
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                String prop = StringArgumentType.getString(ctx, "property");
                                String val  = StringArgumentType.getString(ctx, "value");
                                try {
                                    var field = ModConfig.class.getField(prop);
                                    if (field.getType() == int.class) {
                                        field.setInt(ConfigManager.get(), Integer.parseInt(val));
                                    } else if (field.getType() == boolean.class) {
                                        field.setBoolean(ConfigManager.get(), Boolean.parseBoolean(val));
                                    } else if (field.getType() == String.class) {
                                        field.set(ConfigManager.get(), val);
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("Unsupported property type."));
                                        return 0;
                                    }
                                    ConfigManager.save();
                                    broadcastConfigSync(ctx.getSource().getServer().getPlayerList().getPlayers());
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("[TC VeinGlow] Set " + prop + " = " + val), true);
                                    return 1;
                                } catch (NoSuchFieldException e) {
                                    ctx.getSource().sendFailure(Component.literal("Property not found: " + prop));
                                } catch (Exception e) {
                                    ctx.getSource().sendFailure(Component.literal("Error setting property: " + e.getMessage()));
                                }
                                return 0;
                            })
                        )
                    )
                )
            )
        );
    }

    private static void broadcastConfigSync(List<ServerPlayer> players) {
        var cfg = ConfigManager.get();
        var packet = new ConfigSyncData(cfg.maxBlocks, new ArrayList<>(cfg.blacklistedBlocks));
        for (var player : players) {
            NetworkManager.sendToPlayer(player, packet);
        }
    }
}
