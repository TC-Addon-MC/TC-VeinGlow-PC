package com.tcveinminer.network.handlers;

import com.tcveinminer.engine.MiningEngine;
import com.tcveinminer.engine.state.PlayerStateRegistry;
import com.tcveinminer.network.payload.ActivationRequestData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;

import java.util.UUID;

/**
 * Server-side handler for {@link ActivationRequestData} packets.
 * All loaders call {@link #handle(ServerPlayer, ActivationRequestData)} after decoding.
 */
public final class ActivationRequestHandler {

    public static void handleRaw(Object player, ActivationRequestData data) {
        handle((ServerPlayer) player, data);
    }

    public static void handle(ServerPlayer player, ActivationRequestData data) {
        UUID uuid = player.getUUID();

        // Rate limit: max 20 requests/sec
        if (!PlayerStateRegistry.checkActivationRateLimit(uuid)) return;

        BlockPos targetPos = data.targetPos().map(BlockPos::of).orElse(null);
        MiningEngine.forPlayer(uuid).handleActivationRequest(player, data.active(), targetPos);
    }

    private ActivationRequestHandler() {}
}
