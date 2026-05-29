package com.tcveinminer.network.handlers;

import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.engine.MiningEngine;
import com.tcveinminer.engine.state.PlayerStateRegistry;
import com.tcveinminer.engine.strategy.StrategyRegistry;
import com.tcveinminer.network.payload.HoldKeyData;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * Server-side handler for {@link HoldKeyData} packets.
 * <p>
 * Loader event bridges call {@link #handle(ServerPlayerEntity, HoldKeyData)}
 * after decoding the packet. All game logic lives here — no loader coupling.
 */
public final class HoldKeyHandler {

    public static void handle(ServerPlayerEntity player, HoldKeyData data) {
        UUID uuid = player.getUuid();

        // Validate shapeId length
        if (data.shapeId() == null || data.shapeId().length() > 128) return;

        // Validate equation length
        String safeEq = (data.equation() == null) ? "" : data.equation();
        if (safeEq.length() > 512) safeEq = "";
        final String safeEquation = safeEq;

        boolean isCustomShape = data.shapeId().startsWith("custom:") && !safeEquation.isBlank();
        String safeShapeId = (StrategyRegistry.contains(data.shapeId()) || isCustomShape)
                ? data.shapeId() : "FACE";
        int safeMax = Math.max(1, Math.min(data.maxBlocks(), ConfigManager.get().maxBlocks));

        // Must run on server thread (called from network thread in some loaders)
        // The caller (EventBridge) is responsible for dispatching to server thread.
        // Here we assume we're already on the server thread.

        // Always update shape/config, even on key release
        MiningEngine.forPlayer(uuid).updatePlayerConfig(safeShapeId, safeMax, safeEquation, data.blacklist());

        // Then update hold state
        PlayerStateRegistry.setHolding(uuid, data.isHolding());
    }

    private HoldKeyHandler() {}
}
