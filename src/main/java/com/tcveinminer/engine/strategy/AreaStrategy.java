package com.tcveinminer.engine.strategy;

import com.tcveinminer.engine.traversal.OrientationContext;
import com.tcveinminer.engine.traversal.Traversal;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.List;

public final class AreaStrategy implements MiningStrategy {
    public static final String ID = "AREA_3x3";

    @Override public String getId()    { return ID; }
    @Override public String getLabel() { return "3×3 Area"; }
    @Override public String getIcon()  { return "🟦"; }

    @Override
    public List<BlockPos> collectBlocks(World world, BlockPos origin, BlockState target,
                                        int maxBlocks, OrientationContext ctx) {
        int[][] offsets = Traversal.buildPlaneOffsets(ctx, 1);
        // [ĐÃ SỬA]: Chỉ đào block cùng loại với block mục tiêu
        return Traversal.collectBox(world, origin, offsets, maxBlocks, Traversal.sameBlock(target));
    }
}