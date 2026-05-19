package com.tcveinminer;

import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.engine.MiningEngine;
import com.tcveinminer.network.HoldKeyPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.world.ServerWorld;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TCVeinMinerMod implements ModInitializer {

    /**
     * Players currently holding the key.
     * ConcurrentHashMap.newKeySet() = thread-safe Set.
     * Only mutated on server main thread (via server.execute()).
     */
    public static final Set<UUID> playersHoldingV =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Override
    public void onInitialize() {
        ConfigManager.load();

        PayloadTypeRegistry.playC2S().register(HoldKeyPayload.ID, HoldKeyPayload.CODEC);

        // FIXED: network receiver runs on network thread — dispatch everything
        // to main thread before touching any mutable server state.
        ServerPlayNetworking.registerGlobalReceiver(HoldKeyPayload.ID, (payload, context) -> {
            UUID uuid = context.player().getUuid();
            // Server-side clamp: never trust client maxBlocks value.
            int safeMax = Math.min(payload.maxBlocks(), ConfigManager.get().maxBlocks);
            context.server().execute(() -> {
                if (payload.isHolding()) {
                    playersHoldingV.add(uuid);
                    MiningEngine.forPlayer(uuid).updatePlayerConfig(payload.shapeId(), safeMax);
                } else {
                    playersHoldingV.remove(uuid);
                }
            });
        });

        // Break event triggers engine scan. Guard flag inside engine prevents re-entry.
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, be) -> {
            if (world instanceof ServerWorld sw) {
                MiningEngine.forPlayer(player.getUuid())
                            .onBreakTrigger(player, sw, pos, state);
            }
        });

        // Tick-sliced mining execution.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                for (var player : world.getPlayers()) {
                    MiningEngine.forPlayer(player.getUuid())
                                .onServerTick(player, world);
                }
            }
        });

        // FIXED: cleanup all player state on disconnect to prevent memory leaks.
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUuid();
            server.execute(() -> {
                playersHoldingV.remove(uuid);
                MiningEngine.removePlayer(uuid);
            });
        });
    }
}
