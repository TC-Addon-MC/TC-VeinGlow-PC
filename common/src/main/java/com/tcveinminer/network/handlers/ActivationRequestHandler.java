package com.tcveinminer.network.handlers;

import com.tcveinminer.engine.MiningEngine;
import com.tcveinminer.engine.state.PlayerStateRegistry;
import com.tcveinminer.network.payload.ActivationRequestData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * Server-side handler for {@link ActivationRequestData} packets.
 * All loaders call {@link #handle(ServerPlayerEntity, ActivationRequestData)} after decoding.
 */
public final class ActivationRequestHandler {
    public static void handleRaw(Object player, ActivationRequestData data) {
        handle((ServerPlayerEntity) player, data);
    }

    public static void handle(ServerPlayerEntity player, ActivationRequestData data) {
        UUID uuid = player.getUuid();

        // Rate limit: max 20 requests/sec
        if (!PlayerStateRegistry.checkActivationRateLimit(uuid)) return;

        BlockPos targetPos = data.targetPos().map(BlockPos::fromLong).orElse(null);
        MiningEngine.forPlayer(uuid).handleActivationRequest(player, data.active(), targetPos);
    }

    private ActivationRequestHandler() {}
}
