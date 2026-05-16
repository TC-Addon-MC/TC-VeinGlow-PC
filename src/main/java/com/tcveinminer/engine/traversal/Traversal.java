package com.tcveinminer.engine.traversal;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;
import java.util.function.BiPredicate;

/**
 * Shared traversal engines: BFS và DFS với adjacency tùy chọn.
 * Tất cả strategy đều gọi vào đây thay vì tự implement loop.
 *
 * Adjacency tiers:
 *   FACE    (D6)  — 6 mặt tiếp xúc
 *   EDGES   (D18) — 6 mặt + 12 cạnh ngang/dọc
 *   CORNERS (D26) — 26 hướng gồm cả góc
 */
public final class Traversal {

    // 6-face adjacency
    public static final int[][] D6 = {
            {1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}
    };

    // 18-adjacency: face + edges (không góc chéo 3D)
    public static final int[][] D18;
    static {
        Set<String> seen = new HashSet<>();
        List<int[]> d = new ArrayList<>();
        for (int[] f : D6) { d.add(f); seen.add(key(f)); }
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) == 2) {
                        int[] v = {dx, dy, dz};
                        if (seen.add(key(v))) d.add(v);
                    }
                }
        D18 = d.toArray(new int[0][]);
    }

    // 26-corner adjacency
    public static final int[][] D26;
    static {
        List<int[]> d = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++)
                    if (dx != 0 || dy != 0 || dz != 0)
                        d.add(new int[]{dx, dy, dz});
        D26 = d.toArray(new int[0][]);
    }

    private static String key(int[] v) { return v[0] + "," + v[1] + "," + v[2]; }

    /**
     * BFS từ origin với adjacency tùy chọn.
     * Dùng visited Set để tránh loop vô hạn.
     * origin không được include trong kết quả.
     */
    public static List<BlockPos> bfs(World world, BlockPos origin, int maxBlocks,
                                     int[][] adjacency, BiPredicate<BlockPos, BlockState> matcher) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        visited.add(origin);
        queue.add(origin);

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            BlockPos cur = queue.poll();
            for (int[] d : adjacency) {
                BlockPos nb = cur.add(d[0], d[1], d[2]);
                if (!visited.add(nb)) continue;
                BlockState nbState = world.getBlockState(nb);
                if (matcher.test(nb, nbState)) {
                    result.add(nb);
                    queue.add(nb);
                }
            }
        }
        return result;
    }

    /**
     * DFS từ origin với recursion depth limit.
     * Cho kết quả khác BFS về thứ tự — dùng cho strategies cần depth-first path.
     */
    public static List<BlockPos> dfs(World world, BlockPos origin, int maxBlocks,
                                     int[][] adjacency, BiPredicate<BlockPos, BlockState> matcher) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        visited.add(origin);
        dfsRecurse(world, origin, maxBlocks, adjacency, matcher, visited, result, 0);
        return result;
    }

    private static void dfsRecurse(World world, BlockPos cur, int maxBlocks,
                                   int[][] adjacency, BiPredicate<BlockPos, BlockState> matcher,
                                   Set<BlockPos> visited, List<BlockPos> result, int depth) {
        if (result.size() >= maxBlocks || depth > 512) return; // hard recursion cap
        for (int[] d : adjacency) {
            BlockPos nb = cur.add(d[0], d[1], d[2]);
            if (!visited.add(nb)) continue;
            BlockState nbState = world.getBlockState(nb);
            if (matcher.test(nb, nbState)) {
                result.add(nb);
                dfsRecurse(world, nb, maxBlocks, adjacency, matcher, visited, result, depth + 1);
                if (result.size() >= maxBlocks) return;
            }
        }
    }

    /** Match theo block type (same block, bất kể state) */
    public static BiPredicate<BlockPos, BlockState> sameBlock(BlockState target) {
        return (pos, state) -> state.getBlock() == target.getBlock();
    }

    private Traversal() {}
}
