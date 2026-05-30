package com.tcveinminer.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;

public final class CommandApi {
    @SuppressWarnings("unchecked")
    public static void registerRaw(Object dispatcher) {
        VeinMinerCommand.register((CommandDispatcher<ServerCommandSource>) dispatcher);
    }
    
    private CommandApi() {}
}
