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

    public static final Set<UUID> playersHoldingV =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Override
    public void onInitialize() {
        ConfigManager.load();

        PayloadTypeRegistry.playC2S().register(HoldKeyPayload.ID, HoldKeyPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(HoldKeyPayload.ID, (payload, context) -> {
            UUID uuid = context.player().getUuid();
            int safeMax = Math.min(payload.maxBlocks(), ConfigManager.get().maxBlocks);

            context.server().execute(() -> {
                // [ĐÃ SỬA LỖI]: LUÔN LUÔN cập nhật chế độ đào (shape) kể cả khi thả phím.
                // Tránh việc Client báo đổi chế độ nhưng Server phớt lờ vì đang không nhấn V.
                MiningEngine.forPlayer(uuid).updatePlayerConfig(payload.shapeId(), safeMax);

                // Sau đó mới cập nhật trạng thái có đang giữ phím hay không
                if (payload.isHolding()) {
                    playersHoldingV.add(uuid);
                } else {
                    playersHoldingV.remove(uuid);
                }
            });
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, be) -> {
            if (world instanceof ServerWorld sw) {
                MiningEngine.forPlayer(player.getUuid())
                        .onBreakTrigger(player, sw, pos, state);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                for (var player : world.getPlayers()) {
                    MiningEngine.forPlayer(player.getUuid())
                            .onServerTick(player, world);
                }
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUuid();
            server.execute(() -> {
                playersHoldingV.remove(uuid);
                MiningEngine.removePlayer(uuid);
            });
        });
    }
}