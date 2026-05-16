package com.tcveinminer.engine.strategy;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

/**
 * TallStrategy (1x2 Tall): BFS ngang + kết hợp block trên/dưới thành cặp đứng.
 * Hành vi: đào theo cột 2 block cao — phù hợp tunnel khi player đứng thẳng.
 * Khác vein modes: spread ngang rồi pair theo Y, sort bottom-up để tránh block bị chặn.
 */
public final class TallStrategy implements MiningStrategy {

    public static final String ID = "TALL_1x2";

    @Override public String getId()    { return ID; }
    @Override public String getLabel() { return "1×2 (Tall)"; }
    @Override public String getIcon()  { return "🧱"; }

    @Override
    public List<BlockPos> collectBlocks(World world, BlockPos origin, BlockState target, int maxBlocks) {
        BlockState targetBlock = target;
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        visited.add(origin);
        queue.add(origin);

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            BlockPos cur = queue.poll();

            // Spread theo 4 hướng ngang (không Y)
            for (int[] d : new int[][]{{1,0,0},{-1,0,0},{0,0,1},{0,0,-1}}) {
                BlockPos nb = cur.add(d[0], 0, d[2]);
                if (!visited.add(nb)) continue;
                if (world.getBlockState(nb).getBlock() != target.getBlock()) continue;
                result.add(nb);
                queue.add(nb);
                if (result.size() >= maxBlocks) break;

                // Pair: thêm block bên trên/dưới nếu cùng loại
                BlockPos above = nb.up();
                if (visited.add(above) && world.getBlockState(above).getBlock() == target.getBlock()) {
                    result.add(above);
                    queue.add(above);
                }
            }
        }

        // Sort bottom-up: đào từ dưới lên để không bị tự chặn
        result.sort(Comparator.comparingInt(BlockPos::getY));
        return result;
    }
}
