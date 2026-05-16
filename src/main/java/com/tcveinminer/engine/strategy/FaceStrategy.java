package com.tcveinminer.engine.strategy;

import com.tcveinminer.engine.traversal.Traversal;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * FaceStrategy (V1): BFS chỉ theo 6 mặt tiếp xúc trực tiếp.
 * Hành vi: chỉ đào các block kết nối mặt-đối-mặt — hẹp nhất trong 3 vein modes.
 */
public final class FaceStrategy implements MiningStrategy {

    public static final String ID = "FACE";

    @Override public String getId()    { return ID; }
    @Override public String getLabel() { return "Standard (Face)"; }
    @Override public String getIcon()  { return "⬛"; }

    @Override
    public List<BlockPos> collectBlocks(World world, BlockPos origin, BlockState target, int maxBlocks) {
        return Traversal.bfs(world, origin, maxBlocks, Traversal.D6, Traversal.sameBlock(target));
    }
}
