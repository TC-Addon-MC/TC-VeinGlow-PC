package com.tcveinminer.forge.event;

import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.engine.MiningEngine;
import com.tcveinminer.engine.session.ActionSessionManager;
import com.tcveinminer.engine.state.PlayerStateRegistry;
import com.tcveinminer.network.NetworkManager;
import com.tcveinminer.network.payload.ConfigSyncData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Bridges Forge game events → common engine logic.
 * All Forge event imports confined here.
 */
public final class ForgeEventBridge {

    public static class GameEventListener {

        @SubscribeEvent
        public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            if (event.getLevel().isClientSide())
                return;
            if (com.tcveinminer.engine.right.RightClickEngine.isProcessingInternal())
                return;
            if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer spe))
                return;
            if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel world))
                return;

            com.tcveinminer.engine.MiningEngine engine = com.tcveinminer.engine.MiningEngine.forPlayer(spe.getUUID());
            if (!engine.isWorking() && !engine.right().isProcessing()) {
                boolean started = engine.onInteractTrigger(spe, world, event.getHand(),
                        (net.minecraft.world.phys.BlockHitResult) event.getHitVec());
                if (started)
                    event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
            if (event.getLevel().isClientSide())
                return;
            var result = com.tcveinminer.engine.skill.BucketSkill.onUseItem(
                    event.getEntity(), event.getLevel(), event.getHand());
            if (result.getResult().consumesAction()) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public void onBlockBreak(BlockEvent.BreakEvent event) {
            if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel world) {
                com.tcveinminer.engine.EngineApi.onBreakTriggerRaw(
                        event.getPlayer().getUUID(),
                        event.getPlayer(),
                        world,
                        event.getPos(),
                        world.getBlockState(event.getPos()));
            }
        }

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END)
                return;
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null)
                return;

            ActionSessionManager.checkTimeouts(System.currentTimeMillis(), 5000);
            for (var level : server.getAllLevels()) {
                if (!(level instanceof net.minecraft.server.level.ServerLevel world))
                    continue;
                for (var player : world.players()) {
                    UUID uuid = player.getUUID();
                    com.tcveinminer.engine.EngineApi.onServerTickRaw(uuid, player, world);
                }
            }
        }

        @SubscribeEvent
        public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer spe) {
                var cfg = ConfigManager.get();
                NetworkManager.sendToPlayer(
                        spe,
                        new ConfigSyncData(cfg.maxBlocks, new ArrayList<>(cfg.blacklistedBlocks)));
            }
        }

        @SubscribeEvent
        public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
            UUID uuid = event.getEntity().getUUID();
            PlayerStateRegistry.cleanup(uuid);
            MiningEngine.removePlayer(uuid);
            ActionSessionManager.remove(uuid);
        }
    }

    private ForgeEventBridge() {
    }
}