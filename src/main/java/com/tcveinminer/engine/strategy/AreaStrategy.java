package com.tcveinminer.engine.strategy;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

/**
 * AreaStrategy (3×3): đào tất cả block trong mặt phẳng 3×3 xung quanh block gốc.
 * Hành vi khác biệt hoàn toàn:
 *   - KHÔNG match block type — đào bất kể loại gì
 *   - BFS mở rộng theo mặt phẳng vuông góc với hướng đào
 *   - Dùng cho tunnel/strip mining, không phải vein mining
 *
 * Lưu ý: strategy này intentionally bỏ qua block type matching.
 */
public final class AreaStrategy implements MiningStrategy {

    public static final String ID = "AREA_3x3";

    @Override public String getId()    { return ID; }
    @Override public String getLabel() { return "3×3 Area"; }
    @Override public String getIcon()  { return "🟦"; }

    @Override
    public List<BlockPos> collectBlocks(World world, BlockPos origin, BlockState target, int maxBlocks) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        visited.add(origin);

        // Layer 1: 3×3 trên mặt phẳng XZ (trung tâm là origin)
        // Đây là behavior thật sự khác: đào theo shape, không theo vein
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos nb = origin.add(dx, dy, dz);
                    if (visited.add(nb) && !world.getBlockState(nb).isAir()) {
                        result.add(nb);
                    }
                }
            }
        }

        // Nếu còn quota, BFS tiếp từ các block match (để chain khi đào tunnel)
        if (result.size() < maxBlocks) {
            Deque<BlockPos> queue = new ArrayDeque<>(result.subList(0, Math.min(result.size(), 9)));
            while (!queue.isEmpty() && result.size() < maxBlocks) {
                BlockPos cur = queue.poll();
                // Chỉ spread ngang — tránh đào lung tung theo Y
                for (int[] d : new int[][]{{1,0,0},{-1,0,0},{0,0,1},{0,0,-1}}) {
                    BlockPos nb = cur.add(d[0], d[1], d[2]);
                    if (!visited.add(nb)) continue;
                    BlockState nbState = world.getBlockState(nb);
                    if (!nbState.isAir() && nbState.getBlock() == target.getBlock()) {
                        result.add(nb);
                        queue.add(nb);
                    }
                }
            }
        }

        return result;
    }
}
