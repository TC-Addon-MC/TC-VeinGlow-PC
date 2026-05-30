package com.tcveinminer.neoforge.event;

import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.engine.MiningEngine;
import com.tcveinminer.engine.session.ActionSessionManager;
import com.tcveinminer.engine.state.PlayerStateRegistry;
import com.tcveinminer.network.NetworkManager;
import com.tcveinminer.network.payload.ConfigSyncData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Bridges NeoForge game events → common engine logic.
 * All NeoForge event imports confined here.
 */
public final class NeoForgeEventBridge {

    public static void registerGameBusListeners() {
        NeoForge.EVENT_BUS.register(new GameEventListener());
    }

    public static class GameEventListener {

        @SubscribeEvent
        public void onBlockBreak(BlockEvent.BreakEvent event) {
            if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel world) {
                com.tcveinminer.engine.EngineApi.onBreakTriggerRaw(
                        event.getPlayer().getUUID(),
                        event.getPlayer(),
                        world,
                        event.getPos(),
                        world.getBlockState(event.getPos())
                );
            }
        }

        @SubscribeEvent
        public void onServerTick(ServerTickEvent.Post event) {
            var server = event.getServer();
            ActionSessionManager.checkTimeouts(System.currentTimeMillis(), 5000);
            for (var level : server.getAllLevels()) {
                if (!(level instanceof net.minecraft.server.level.ServerLevel world)) continue;
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
                        new ConfigSyncData(cfg.maxBlocks, new ArrayList<>(cfg.blacklistedBlocks))
                );
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

    private NeoForgeEventBridge() {}
}
