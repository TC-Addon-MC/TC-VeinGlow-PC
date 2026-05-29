package com.tcveinminer.fabric;

import com.tcveinminer.api.addon.AddonLoader;
import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.fabric.event.FabricEventBridge;
import com.tcveinminer.fabric.network.FabricPacketChannel;
import com.tcveinminer.fabric.registry.FabricCommandRegistrar;
import com.tcveinminer.network.NetworkManager;
import net.fabricmc.api.ModInitializer;

/**
 * Fabric server-side mod entrypoint.
 * <p>
 * Replaces the old {@code TCVeinMinerMod} which directly imported Fabric API
 * throughout the engine. All Fabric-specific code is now confined to:
 * <ul>
 *   <li>{@code fabric/} module classes</li>
 *   <li>This entrypoint wires them to the common module via abstractions</li>
 * </ul>
 */
public final class TCVeinMinerFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        // 1. Load server config (uses Services.PLATFORM() → FabricPlatformHelper)
        ConfigManager.load();

        // 2. Initialize networking backend
        FabricPacketChannel channel = new FabricPacketChannel();
        NetworkManager.setChannel(channel);
        channel.registerServerPackets();

        // 3. Bridge Fabric game events → common engine logic
        FabricEventBridge.register();

        // 4. Register commands
        FabricCommandRegistrar.register();

        // 5. Load addons via ServiceLoader
        AddonLoader.loadAll(new com.tcveinminer.api.addon.AddonContextImpl());
    }
}
