package com.tcveinminer;

import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.engine.session.ActionSessionManager;
import com.tcveinminer.engine.MiningEngine;
import com.tcveinminer.engine.strategy.StrategyRegistry;
import com.tcveinminer.network.HoldKeyPayload;
import com.tcveinminer.network.ConfigSyncPayload;
import com.tcveinminer.network.MiningStatePayload;
import com.tcveinminer.network.ActivationRequestPayload;
import com.tcveinminer.network.ActivationConfirmPayload;
import com.tcveinminer.network.LookedAtBlockPayload;
import com.tcveinminer.network.FilterResultPayload;
import com.tcveinminer.network.HighlightBlockListPayload;
import com.tcveinminer.network.HighlightDeltaPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.util.ActionResult;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TCVeinMinerMod implements ModInitializer {

    public static final Set<UUID> playersHoldingV =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final java.util.Map<UUID, Long> activationRateLimitMap = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        ConfigManager.load();

        PayloadTypeRegistry.playC2S().register(HoldKeyPayload.ID, HoldKeyPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ConfigSyncPayload.ID, ConfigSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MiningStatePayload.ID, MiningStatePayload.CODEC);

        PayloadTypeRegistry.playC2S().register(ActivationRequestPayload.ID, ActivationRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ActivationConfirmPayload.ID, ActivationConfirmPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LookedAtBlockPayload.ID, LookedAtBlockPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FilterResultPayload.ID, FilterResultPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HighlightBlockListPayload.ID, HighlightBlockListPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HighlightDeltaPayload.ID, HighlightDeltaPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(HoldKeyPayload.ID, (payload, context) -> {
            UUID uuid = context.player().getUuid();
            if (payload.shapeId() == null || payload.shapeId().length() > 128) return;

            String safeEq = payload.equation() == null ? "" : payload.equation();
            if (safeEq.length() > 512) safeEq = "";
            final String safeEquation = safeEq;

            boolean isCustomShape = payload.shapeId().startsWith("custom:") && !safeEquation.isBlank();
            String safeShapeId = (StrategyRegistry.contains(payload.shapeId()) || isCustomShape)
                    ? payload.shapeId()
                    : "FACE";
            int safeMax = Math.max(1, Math.min(payload.maxBlocks(), ConfigManager.get().maxBlocks));

            context.server().execute(() -> {
                // [ĐÃ SỬA LỖI]: LUÔN LUÔN cập nhật chế độ đào (shape) kể cả khi thả phím.
                // Tránh việc Client báo đổi chế độ nhưng Server phớt lờ vì đang không nhấn V.
                MiningEngine.forPlayer(uuid).updatePlayerConfig(safeShapeId, safeMax, safeEquation, payload.blacklist());

                // Sau đó mới cập nhật trạng thái có đang giữ phím hay không
                if (payload.isHolding()) {
                    playersHoldingV.add(uuid);
                } else {
                    playersHoldingV.remove(uuid);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(ActivationRequestPayload.ID, (payload, context) -> {
            UUID uuid = context.player().getUuid();
            long now = System.currentTimeMillis();
            if (now - activationRateLimitMap.getOrDefault(uuid, 0L) < 50) return; // Rate limit: max 20 requests per sec
            activationRateLimitMap.put(uuid, now);

            context.server().execute(() -> {
                MiningEngine.forPlayer(uuid)
                        .handleActivationRequest((net.minecraft.server.network.ServerPlayerEntity) context.player(), payload.active(), payload.targetPos().orElse(null));
            });
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, be) -> {
            if (world instanceof ServerWorld sw) {
                MiningEngine.forPlayer(player.getUuid())
                        .onBreakTrigger(player, sw, pos, state);
            }
        });

        net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register((player, world, hand) -> {
            return com.tcveinminer.engine.skill.BucketSkill.onUseItem(player, world, hand);
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (com.tcveinminer.engine.right.RightClickEngine.isProcessingInternal()) {
                return ActionResult.PASS;
            }
            if (player instanceof net.minecraft.server.network.ServerPlayerEntity spe) {
                if (playersHoldingV.contains(spe.getUuid())) {
                    MiningEngine engine = MiningEngine.forPlayer(spe.getUuid());
                    if (!engine.isWorking() && !engine.right().isProcessing()) {
                        boolean started = engine.onInteractTrigger(spe, (ServerWorld) world, hand, hitResult);
                        if (started) return ActionResult.SUCCESS;
                    }
                }
            }
            return ActionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ActionSessionManager.checkTimeouts(System.currentTimeMillis(), 5000);
            for (ServerWorld world : server.getWorlds()) {
                for (var player : world.getPlayers()) {
                    if (MiningEngine.hasEngine(player.getUuid())) {
                        MiningEngine.forPlayer(player.getUuid())
                                .onServerTick(player, world);
                    }
                }
            }
        });

                ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var cfg = ConfigManager.get();
            sender.sendPacket(new ConfigSyncPayload(cfg.maxBlocks, new ArrayList<>(cfg.blacklistedBlocks)));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUuid();
            server.execute(() -> {
                playersHoldingV.remove(uuid);
                MiningEngine.removePlayer(uuid);
                ActionSessionManager.remove(uuid);
            });
        });
    }
}
