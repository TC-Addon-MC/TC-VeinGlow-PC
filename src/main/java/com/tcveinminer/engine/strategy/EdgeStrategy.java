package com.tcveinminer.engine.strategy;

import com.tcveinminer.engine.traversal.Traversal;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * EdgeStrategy (V2): BFS theo 18 hướng — mặt + cạnh, không góc.
 * Hành vi: phát hiện ore nằm chéo trên cùng mặt phẳng (edge-connected).
 * Khác V1: detect diagonal seams trong vein.
 */
public final class EdgeStrategy implements MiningStrategy {

    public static final String ID = "EDGES";

    @Override public String getId()    { return ID; }
    @Override public String getLabel() { return "Standard V2 (Edges)"; }
    @Override public String getIcon()  { return "🔷"; }

    @Override
    public List<BlockPos> collectBlocks(World world, BlockPos origin, BlockState target, int maxBlocks) {
        return Traversal.bfs(world, origin, maxBlocks, Traversal.D18, Traversal.sameBlock(target));
    }
}
