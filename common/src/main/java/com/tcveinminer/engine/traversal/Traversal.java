package com.tcveinminer.engine.traversal;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.function.BiPredicate;

/**
 * Unified traversal engine. All strategies call here.
 *
 * DFS is iterative (stack-based). No recursion. No StackOverflowError possible.
 *
 * Hard limits on node count and iterations prevent server freeze.
 */
public final class Traversal {

    /** Maximum nodes visited across any traversal (anti-freeze guard). */
    private static final int MAX_VISITED = 32_768;

    // 6-face adjacency
    public static final int[][] D6 = {
            { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 }
    };

    // 18-adjacency: face + edge (no 3D corner)
    public static final int[][] D18;
    static {
        List<int[]> d = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (int[] f : D6) {
            d.add(f);
            seen.add(dirKey(f));
        }
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) == 2) {
                        int[] v = { dx, dy, dz };
                        if (seen.add(dirKey(v)))
                            d.add(v);
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
                        d.add(new int[] { dx, dy, dz });
        D26 = d.toArray(new int[0][]);
    }

    private static long dirKey(int[] v) {
        return ((long) (v[0] + 2) * 25L + (v[1] + 2) * 5L + (v[2] + 2));
    }

    /**
     * Iterative BFS from origin.
     * origin is NOT included in results.
     */
    public static List<BlockPos> bfs(World world, BlockPos origin, int maxBlocks,
            int[][] adjacency, BiPredicate<BlockPos, BlockState> matcher) {
        List<BlockPos> result = new ArrayList<>(Math.min(maxBlocks, 256));
        LongOpenHashSet visited = new LongOpenHashSet(Math.min(maxBlocks * 4, MAX_VISITED));
        Deque<BlockPos> queue = new ArrayDeque<>();

        visited.add(origin.asLong());
        queue.add(origin);
        int iterations = 0;

        while (!queue.isEmpty() && result.size() < maxBlocks && iterations < MAX_VISITED) {
            BlockPos cur = queue.poll();
            iterations++;
            for (int[] d : adjacency) {
                if (result.size() >= maxBlocks)
                    break;
                BlockPos nb = cur.add(d[0], d[1], d[2]);
                if (!visited.add(nb.asLong()))
                    continue;
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
     * Iterative DFS from origin. Uses explicit stack — no recursion.
     * origin is NOT included in results.
     */
    public static List<BlockPos> dfs(World world, BlockPos origin, int maxBlocks,
            int[][] adjacency, BiPredicate<BlockPos, BlockState> matcher) {
        List<BlockPos> result = new ArrayList<>(Math.min(maxBlocks, 256));
        LongOpenHashSet visited = new LongOpenHashSet(Math.min(maxBlocks * 4, MAX_VISITED));
        Deque<BlockPos> stack = new ArrayDeque<>();

        visited.add(origin.asLong());
        stack.push(origin);
        int iterations = 0;

        while (!stack.isEmpty() && result.size() < maxBlocks && iterations < MAX_VISITED) {
            BlockPos cur = stack.pop();
            iterations++;
            for (int[] d : adjacency) {
                if (result.size() >= maxBlocks)
                    break;
                BlockPos nb = cur.add(d[0], d[1], d[2]);
                if (!visited.add(nb.asLong()))
                    continue;
                BlockState nbState = world.getBlockState(nb);
                if (matcher.test(nb, nbState)) {
                    result.add(nb);
                    stack.push(nb);
                }
            }
        }
        return result;
    }

    /**
     * Collect blocks within a bounded box (oriented plane or 3D region).
     * All offsets are in world space. No traversal — flat iteration.
     * Used by AreaStrategy and shape-mining modes.
     */
    public static List<BlockPos> collectBox(World world, BlockPos origin,
            int[][] worldOffsets, int maxBlocks,
            BiPredicate<BlockPos, BlockState> matcher) {
        List<BlockPos> result = new ArrayList<>();
        for (int[] off : worldOffsets) {
            if (result.size() >= maxBlocks)
                break;
            BlockPos nb = origin.add(off[0], off[1], off[2]);
            BlockState state = world.getBlockState(nb);
            if (matcher.test(nb, state))
                result.add(nb);
        }
        return result;
    }

    // ── Matchers ─────────────────────────────────────────────────────────────

    /** Match same block type regardless of state. */
    public static BiPredicate<BlockPos, BlockState> sameBlock(BlockState target) {
        return (pos, state) -> state.getBlock() == target.getBlock();
    }

    /** Match any non-air block. */
    public static BiPredicate<BlockPos, BlockState> notAir() {
        return (pos, state) -> !state.isAir();
    }

    // ── Oriented box builders ─────────────────────────────────────────────────

    /**
     * Build a flat 2D grid of offsets in the hit-face plane.
     * halfSide=1 → 3×3, halfSide=2 → 5×5
     */
    public static int[][] buildPlaneOffsets(OrientationContext ctx, int halfSide) {
        int side = halfSide * 2 + 1;
        int[][] offsets = new int[side * side - 1][3];
        int idx = 0;
        for (int s = -halfSide; s <= halfSide; s++) {
            for (int u = -halfSide; u <= halfSide; u++) {
                if (s == 0 && u == 0)
                    continue;
                BlockPos off = ctx.planeOffset(BlockPos.ORIGIN, s, u);
                offsets[idx++] = new int[] { off.getX(), off.getY(), off.getZ() };
            }
        }
        return offsets;
    }

    /**
     * Build a tunnel of given width×height × depth.
     * Center-aligned in the plane, extending along forward axis.
     */
    public static int[][] buildTunnelOffsets(OrientationContext ctx,
            int halfW, int halfH, int depth) {
        List<int[]> offsets = new ArrayList<>();
        for (int f = 1; f <= depth; f++) {
            for (int s = -halfW; s <= halfW; s++) {
                for (int u = -halfH; u <= halfH; u++) {
                    BlockPos off = ctx.offset(BlockPos.ORIGIN, f, s, u);
                    offsets.add(new int[] { off.getX(), off.getY(), off.getZ() });
                }
            }
        }
        return offsets.toArray(new int[0][]);
    }

    private Traversal() {
    }
}
