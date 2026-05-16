package com.tcveinminer.engine.strategy;

import com.tcveinminer.engine.traversal.Traversal;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * CornerStrategy (V3): DFS theo 26 hướng — mặt + cạnh + góc.
 * Hành vi: dùng DFS để ưu tiên đào sâu dọc theo vein trước khi mở rộng.
 * Khác V2: (a) detect góc 3D, (b) DFS thay BFS → mining order khác hẳn.
 * Dùng DFS vì vein thường là chuỗi dài — DFS sẽ follow dọc chuỗi trước.
 */
public final class CornerStrategy implements MiningStrategy {

    public static final String ID = "CORNERS";

    @Override public String getId()    { return ID; }
    @Override public String getLabel() { return "Standard V3 (Corners)"; }
    @Override public String getIcon()  { return "💎"; }

    @Override
    public List<BlockPos> collectBlocks(World world, BlockPos origin, BlockState target, int maxBlocks) {
        // DFS: follow vein depth-first trước khi branch ra
        return Traversal.dfs(world, origin, maxBlocks, Traversal.D26, Traversal.sameBlock(target));
    }
}
