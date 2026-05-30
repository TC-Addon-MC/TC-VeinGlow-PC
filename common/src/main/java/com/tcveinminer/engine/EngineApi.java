package com.tcveinminer.engine;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import java.util.UUID;

public final class EngineApi {
    public static void onBreakTriggerRaw(UUID uuid, Object player, Object world, Object pos, Object state) {
        MiningEngine.forPlayer(uuid).onBreakTrigger(
                (PlayerEntity) player,
                (ServerWorld) world,
                (BlockPos) pos,
                (BlockState) state
        );
    }
    
    public static void onServerTickRaw(UUID uuid, Object player, Object world) {
        MiningEngine.forPlayer(uuid).onServerTick(
                (PlayerEntity) player,
                (ServerWorld) world
        );
    }
    
    private EngineApi() {}
}
