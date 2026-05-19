package com.tcveinminer.engine.strategy;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

/**
 * Area5x5Strategy: đào 5×5 block xung quanh origin.
 * Không match block type — đào tất cả block không phải air.
 */
public final class Area5x5Strategy implements MiningStrategy {
    public static final String ID = "AREA_5x5";

    @Override public String getId()    { return ID; }
    @Override public String getLabel() { return "5×5 Area"; }
    @Override public String getIcon()  { return "🔵"; }

    @Override
    public List<BlockPos> collectBlocks(World world, BlockPos origin, BlockState target, int maxBlocks) {
        List<BlockPos> result = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos nb = origin.add(dx, dy, dz);
                    if (!world.getBlockState(nb).isAir()) {
                        result.add(nb);
                        if (result.size() >= maxBlocks) return result;
                    }
                }
            }
        }
        return result;
    }
}
