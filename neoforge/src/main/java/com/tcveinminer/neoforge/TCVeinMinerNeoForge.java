package com.tcveinminer.neoforge;

import com.tcveinminer.api.addon.AddonLoader;
import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.api.addon.AddonContextImpl;
import com.tcveinminer.neoforge.event.NeoForgeEventBridge;
import com.tcveinminer.neoforge.network.NeoForgePacketChannel;
import com.tcveinminer.neoforge.registry.NeoForgeCommandRegistrar;
import com.tcveinminer.network.NetworkManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * NeoForge server-side mod entrypoint.
 * <p>
 * Uses the mod bus for setup and registration, game bus for gameplay events.
 * All NeoForge-specific code lives in this module; common engine is untouched.
 */
@Mod("tc_veinminer")
public final class TCVeinMinerNeoForge {

    public TCVeinMinerNeoForge(IEventBus modBus) {
        // 1. Load config
        ConfigManager.load();

        // 2. Setup networking (register payload types on mod bus)
        NeoForgePacketChannel channel = new NeoForgePacketChannel();
        NetworkManager.setChannel(channel);
        modBus.addListener(channel::registerPackets);

        // 3. Register gameplay event listeners on the GAME bus
        NeoForgeEventBridge.registerGameBusListeners();

        // 4. Register commands
        NeoForgeCommandRegistrar.register(modBus);

        // 5. Load addons on FMLCommonSetup (after mod loading completes)
        modBus.addListener((FMLCommonSetupEvent event) ->
                event.enqueueWork(() -> AddonLoader.loadAll(new AddonContextImpl()))
        );
    }
}
