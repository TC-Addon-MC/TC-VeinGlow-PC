package com.tcveinminer.neoforge.registry;

import com.tcveinminer.command.VeinMinerCommand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class NeoForgeCommandRegistrar {
    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> {
            com.tcveinminer.command.CommandApi.registerRaw(event.getDispatcher());
        });
    }
}
