package com.tcveinminer.engine.strategy;

import com.tcveinminer.engine.traversal.OrientationContext;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public final class TallStrategy implements MiningStrategy {
    public static final String ID = "TALL_1x2";
    @Override public String getId()    { return ID; }
    @Override public String getLabel() { return "1×2 (Tall)"; }
    @Override public String getIcon()  { return "🧱"; }

    private static final int[][] HORIZ = {{1,0,0},{-1,0,0},{0,0,1},{0,0,-1}};

    @Override
    public List<BlockPos> collectBlocks(World world, BlockPos origin, BlockState target,
                                         int maxBlocks, OrientationContext ctx) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        visited.add(origin);
        queue.add(origin);

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            BlockPos cur = queue.poll();
            for (int[] d : HORIZ) {
                BlockPos nb = cur.add(d[0], 0, d[2]);
                if (!visited.add(nb)) continue;
                if (world.getBlockState(nb).getBlock() != target.getBlock()) continue;
                result.add(nb);
                queue.add(nb);
                if (result.size() >= maxBlocks) break;

                BlockPos above = nb.up();
                if (visited.add(above) &&
                    world.getBlockState(above).getBlock() == target.getBlock()) {
                    result.add(above);
                    queue.add(above);
                }
            }
        }
        result.sort(Comparator.comparingInt(BlockPos::getY));
        return result;
    }
}
