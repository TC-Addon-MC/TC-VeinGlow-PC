package com.tcveinminer.engine.strategy;

import com.tcveinminer.engine.traversal.Traversal;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

/**
 * TreeCapitatorStrategy: chặt toàn bộ cây khi đập gốc.
 * - Chỉ hoạt động trên LOG blocks (dùng BlockTag minecraft:logs)
 * - BFS theo 26 hướng nhưng ưu tiên đi lên (chặt cây từ gốc)
 * - Tự động include LEAVES nếu tìm thấy liền kề (để lá rụng item)
 */
public final class TreeCapitatorStrategy implements MiningStrategy {
    public static final String ID = "TREE_CAP";

    @Override public String getId()    { return ID; }
    @Override public String getLabel() { return "TreeCapitator"; }
    @Override public String getIcon()  { return "🌳"; }

    @Override
    public List<BlockPos> collectBlocks(World world, BlockPos origin, BlockState target, int maxBlocks) {
        // Chỉ kích hoạt khi block là log
        if (!target.isIn(BlockTags.LOGS)) {
            // Fallback: BFS cùng loại block
            return Traversal.bfs(world, origin, maxBlocks, Traversal.D26, Traversal.sameBlock(target));
        }

        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        // Ưu tiên lên trước: sắp xếp để đi lên (Y tăng) trước
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(origin);
        queue.add(origin);

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            BlockPos cur = queue.poll();

            for (int[] d : Traversal.D26) {
                BlockPos nb = cur.add(d[0], d[1], d[2]);
                if (!visited.add(nb)) continue;
                BlockState nbState = world.getBlockState(nb);

                if (nbState.isIn(BlockTags.LOGS)) {
                    result.add(nb);
                    queue.add(nb);
                } else if (nbState.isIn(BlockTags.LEAVES) && result.size() < maxBlocks) {
                    // Include lá liền kề cây để drop đúng
                    result.add(nb);
                }
            }
        }

        // Sort bottom-up để đào từ gốc lên (tránh log trên rơi xuống)
        result.sort(Comparator.comparingInt(BlockPos::getY));
        return result;
    }
}
