package com.tcveinminer.forge.registry;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;

public final class ForgeCommandRegistrar {
    public static void register() {
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> {
            com.tcveinminer.command.CommandApi.registerRaw(event.getDispatcher());
        });
    }
}
