package com.tcveinminer;

import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.engine.MiningEngine;
import com.tcveinminer.network.HoldKeyPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.world.ServerWorld;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TCVeinMinerMod implements ModInitializer {

    /** Players đang giữ phím — synced qua packet từ client. */
    public static final Set<UUID> playersHoldingV = new HashSet<>();

    @Override
    public void onInitialize() {
        ConfigManager.load();

        // Packet registration
        PayloadTypeRegistry.playC2S().register(HoldKeyPayload.ID, HoldKeyPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(HoldKeyPayload.ID, (payload, context) -> {
            UUID uuid = context.player().getUuid();
            if (payload.isHolding()) playersHoldingV.add(uuid);
            else playersHoldingV.remove(uuid);
        });

        // Break event: trigger engine scan + queue build
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, be) -> {
            if (world instanceof ServerWorld sw) {
                MiningEngine.forPlayer(player.getUuid())
                            .onBreakTrigger(player, sw, pos, state);
            }
        });

        // Server tick: execute queued blocks (tick-sliced)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                for (var player : world.getPlayers()) {
                    MiningEngine.forPlayer(player.getUuid())
                                .onServerTick(player, world);
                }
            }
        });
    }
}
