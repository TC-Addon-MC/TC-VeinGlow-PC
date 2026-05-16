package com.tcveinminer.engine.strategy;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

/**
 * StairStrategy: đào theo hướng cầu thang (lên hoặc xuống).
 * Hành vi thật sự khác biệt:
 *   - Mỗi bước: tiến 1 ngang + dy dọc
 *   - BFS dọc theo chuỗi cầu thang, không spread bừa ra
 *   - Sorted theo Y để đào từ nguồn ra
 * Không phải offset hack — đây là traversal rule thật sự khác nhau.
 */
public final class StairStrategy implements MiningStrategy {

    public static final String ID_UP   = "STAIR_UP";
    public static final String ID_DOWN = "STAIR_DOWN";

    private final int dy;
    private final String id;

    /** dy = +1 (lên) hoặc -1 (xuống) */
    public StairStrategy(int dy) {
        this.dy = dy;
        this.id = dy > 0 ? ID_UP : ID_DOWN;
    }

    @Override public String getId()    { return id; }
    @Override public String getLabel() { return dy > 0 ? "Stair Up" : "Stair Down"; }
    @Override public String getIcon()  { return dy > 0 ? "⬆" : "⬇"; }

    // 4 hướng ngang
    private static final int[][] HORIZ = {{1,0,0},{-1,0,0},{0,0,1},{0,0,-1}};

    @Override
    public List<BlockPos> collectBlocks(World world, BlockPos origin, BlockState target, int maxBlocks) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        visited.add(origin);
        queue.add(origin);

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            BlockPos cur = queue.poll();

            // Face-adjacent spread (để detect ore trong cùng "bậc thang")
            for (int[] d : new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}}) {
                BlockPos nb = cur.add(d[0], d[1], d[2]);
                if (!visited.add(nb)) continue;
                if (world.getBlockState(nb).getBlock() != target.getBlock()) continue;
                result.add(nb);
                queue.add(nb);
            }

            // Stair step: ngang 1 + dy (đây là behavior riêng của StairStrategy)
            for (int[] h : HORIZ) {
                BlockPos step = cur.add(h[0], dy, h[2]);
                if (!visited.add(step)) continue;
                if (world.getBlockState(step).getBlock() != target.getBlock()) continue;
                result.add(step);
                queue.add(step);
            }
        }

        // Sort theo hướng đào: lên thì Y tăng dần, xuống thì Y giảm dần
        result.sort(dy > 0
                ? Comparator.comparingInt(BlockPos::getY)
                : Comparator.comparingInt((BlockPos p) -> p.getY()).reversed());
        return result;
    }
}
