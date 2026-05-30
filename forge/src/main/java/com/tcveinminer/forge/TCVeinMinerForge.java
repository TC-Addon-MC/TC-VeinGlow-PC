package com.tcveinminer.forge;

import com.tcveinminer.api.addon.AddonLoader;
import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.api.addon.AddonContextImpl;
import com.tcveinminer.forge.event.ForgeEventBridge;
import com.tcveinminer.forge.network.ForgePacketChannel;
import com.tcveinminer.forge.registry.ForgeCommandRegistrar;
import com.tcveinminer.network.NetworkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Forge server-side mod entrypoint.
 * Wires platform, networking, events, commands, and addons via abstractions.
 */
@Mod("tc_veinminer")
public final class TCVeinMinerForge {

    public TCVeinMinerForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 1. Load config
        ConfigManager.load();

        // 2. Networking
        ForgePacketChannel channel = new ForgePacketChannel();
        NetworkManager.setChannel(channel);
        channel.registerPackets();

        // 3. Game event listeners on the FORGE game bus
        MinecraftForge.EVENT_BUS.register(new ForgeEventBridge.GameEventListener());

        // 4. Commands
        ForgeCommandRegistrar.register();

        // 5. Addons
        modBus.addListener((FMLCommonSetupEvent event) ->
                event.enqueueWork(() -> AddonLoader.loadAll(new AddonContextImpl()))
        );
    }
}
