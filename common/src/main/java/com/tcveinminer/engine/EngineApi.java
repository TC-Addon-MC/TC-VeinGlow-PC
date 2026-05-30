package com.tcveinminer.engine;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import java.util.UUID;

public final class EngineApi {
    public static void onBreakTriggerRaw(UUID uuid, Object player, Object world, Object pos, Object state) {
        MiningEngine.forPlayer(uuid).onBreakTrigger(
                (Player) player,
                (ServerLevel) world,
                (BlockPos) pos,
                (BlockState) state
        );
    }
    
    public static void onServerTickRaw(UUID uuid, Object player, Object world) {
        MiningEngine.forPlayer(uuid).onServerTick(
                (Player) player,
                (ServerLevel) world
        );
    }
    
    private EngineApi() {}
}
