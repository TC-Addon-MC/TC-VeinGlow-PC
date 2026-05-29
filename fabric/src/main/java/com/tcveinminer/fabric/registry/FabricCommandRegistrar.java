package com.tcveinminer.fabric.registry;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/**
 * Registers /tcveinminer commands using Fabric's CommandRegistrationCallback.
 * All Fabric command API imports isolated here.
 */
public final class FabricCommandRegistrar {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            com.tcveinminer.command.VeinMinerCommand.register(dispatcher);
        });
    }

    private FabricCommandRegistrar() {}
}
