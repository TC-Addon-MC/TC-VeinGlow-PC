package com.tcveinminer.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public final class CommandApi {
    @SuppressWarnings("unchecked")
    public static void registerRaw(Object dispatcher) {
        VeinMinerCommand.register((CommandDispatcher<CommandSourceStack>) dispatcher);
    }
    
    private CommandApi() {}
}
