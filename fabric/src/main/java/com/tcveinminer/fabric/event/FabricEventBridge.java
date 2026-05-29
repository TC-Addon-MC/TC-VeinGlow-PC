package com.tcveinminer.fabric.event;

import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.engine.MiningEngine;
import com.tcveinminer.engine.session.ActionSessionManager;
import com.tcveinminer.engine.state.PlayerStateRegistry;
import com.tcveinminer.fabric.network.FabricPacketChannel;
import com.tcveinminer.network.NetworkManager;
import com.tcveinminer.network.handlers.ActivationRequestHandler;
import com.tcveinminer.network.handlers.HoldKeyHandler;
import com.tcveinminer.network.payload.ConfigSyncData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Bridges Fabric game events → common engine logic.
 * <p>
 * All Fabric API imports are isolated to this file.
 * The common module knows nothing about Fabric events.
 */
public final class FabricEventBridge {

    public static void register() {
        registerPlayerEvents();
        registerWorldEvents();
        registerConnectionEvents();
        registerIncomingPackets();
    }

    // ── Block / Player Events ─────────────────────────────────────────────

    private static void registerPlayerEvents() {
        // Left click break → vein mine trigger
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, be) -> {
            if (world instanceof ServerWorld sw) {
                MiningEngine.forPlayer(player.getUuid()).onBreakTrigger(player, sw, pos, state);
            }
        });

        // Use item (bucket skill)
        UseItemCallback.EVENT.register((player, world, hand) ->
                com.tcveinminer.engine.skill.BucketSkill.onUseItem(player, world, hand)
        );

        // Right click block → interact/harvest/plant trigger
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (com.tcveinminer.engine.right.RightClickEngine.isProcessingInternal()) {
                return ActionResult.PASS;
            }
            if (player instanceof net.minecraft.server.network.ServerPlayerEntity spe) {
                MiningEngine engine = MiningEngine.forPlayer(spe.getUuid());
                if (!engine.isWorking() && !engine.right().isProcessing()) {
                    boolean started = engine.onInteractTrigger(spe, (ServerWorld) world, hand, hitResult);
                    if (started) return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        });
    }

    // ── World / Server Tick ───────────────────────────────────────────────

    private static void registerWorldEvents() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ActionSessionManager.checkTimeouts(System.currentTimeMillis(), 5000);
            for (ServerWorld world : server.getWorlds()) {
                for (var player : world.getPlayers()) {
                    UUID uuid = player.getUuid();
                    if (MiningEngine.hasEngine(uuid)) {
                        MiningEngine.forPlayer(uuid).onServerTick(player, world);
                    }
                }
            }
        });
    }

    // ── Connection Events ─────────────────────────────────────────────────

    private static void registerConnectionEvents() {
        // Sync config on join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var cfg = ConfigManager.get();
            NetworkManager.sendToPlayer(
                    handler.player,
                    new ConfigSyncData(cfg.maxBlocks, new ArrayList<>(cfg.blacklistedBlocks))
            );
        });

        // Cleanup on disconnect
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUuid();
            server.execute(() -> {
                PlayerStateRegistry.cleanup(uuid);
                MiningEngine.removePlayer(uuid);
                ActionSessionManager.remove(uuid);
            });
        });
    }

    // ── Incoming C→S Packets ──────────────────────────────────────────────

    private static void registerIncomingPackets() {
        // HoldKey C→S
        ServerPlayNetworking.registerGlobalReceiver(
                FabricPacketChannel.FabricHoldKeyPayload.ID,
                (payload, context) -> {
                    var data = new com.tcveinminer.network.payload.HoldKeyData(
                            payload.isHolding(), payload.shapeId(), payload.maxBlocks(),
                            payload.equation(), payload.blacklist()
                    );
                    context.server().execute(() -> HoldKeyHandler.handle(context.player(), data));
                }
        );

        // ActivationRequest C→S
        ServerPlayNetworking.registerGlobalReceiver(
                FabricPacketChannel.FabricActivationRequestPayload.ID,
                (payload, context) -> {
                    var data = new com.tcveinminer.network.payload.ActivationRequestData(
                            payload.active(), payload.targetPos()
                    );
                    context.server().execute(() -> ActivationRequestHandler.handle(context.player(), data));
                }
        );
    }

    private FabricEventBridge() {}
}
