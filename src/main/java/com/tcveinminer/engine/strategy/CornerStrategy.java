package com.tcveinminer.engine.strategy;

import com.tcveinminer.engine.traversal.OrientationContext;
import com.tcveinminer.engine.traversal.Traversal;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.List;

public final class CornerStrategy implements MiningStrategy {
    public static final String ID = "CORNERS";
    @Override public String getId()    { return ID; }
    @Override public String getLabel() { return "Standard V3 (Corners)"; }
    @Override public String getIcon()  { return "💎"; }
    @Override
    public List<BlockPos> collectBlocks(World world, BlockPos origin, BlockState target,
                                         int maxBlocks, OrientationContext ctx) {
        // Iterative DFS — no recursion, no StackOverflow
        return Traversal.dfs(world, origin, maxBlocks, Traversal.D26, Traversal.sameBlock(target));
    }
}
