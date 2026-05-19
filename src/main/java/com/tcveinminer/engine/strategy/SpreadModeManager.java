package com.tcveinminer.engine.strategy;

import com.tcveinminer.engine.traversal.OrientationContext;
import com.tcveinminer.engine.traversal.Traversal;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

/**
 * Quản lý các chế độ lan (vein/spread):
 *
 *   FACE     — BFS 6 mặt,  chỉ block cùng loại
 *   EDGES    — BFS 18 adj, chỉ block cùng loại
 *   CORNERS  — DFS 26 adj, chỉ block cùng loại
 *   TALL     — BFS ngang, mỗi vị trí kéo thêm block phía trên (+1 up)
 *   TREE_CAP — BFS theo log, gom thêm lá xung quanh
 */
public final class SpreadModeManager implements MiningStrategy {

    public enum Mode { FACE, EDGES, CORNERS, TALL, TREE_CAP }

    // Bốn hướng ngang, dùng riêng cho TALL
    private static final int[][] HORIZ = {{1,0,0},{-1,0,0},{0,0,1},{0,0,-1}};

    private final Mode   mode;
    private final String id, label, icon;

    public SpreadModeManager(Mode mode, String id, String label, String icon) {
        this.mode  = mode;
        this.id    = id;
        this.label = label;
        this.icon  = icon;
    }

    @Override public String getId()    { return id; }
    @Override public String getLabel() { return label; }
    @Override public String getIcon()  { return icon; }

    @Override
    public List<BlockPos> collectBlocks(World world, BlockPos origin, BlockState target,
                                        int maxBlocks, OrientationContext ctx) {
        return switch (mode) {
            case FACE     -> Traversal.bfs(world, origin, maxBlocks,
                                           Traversal.D6,  FilterModeManager.sameBlock(target));
            case EDGES    -> Traversal.bfs(world, origin, maxBlocks,
                                           Traversal.D18, FilterModeManager.sameBlock(target));
            case CORNERS  -> Traversal.dfs(world, origin, maxBlocks,
                                           Traversal.D26, FilterModeManager.sameBlock(target));
            case TALL     -> collectTall(world, origin, target, maxBlocks);
            case TREE_CAP -> collectTree(world, origin, target, maxBlocks);
        };
    }

    // ── TALL ─────────────────────────────────────────────────────────────────

    /**
     * Lan ngang theo D4, mỗi block đào thêm block ngay phía trên (1×2 tall).
     * Đảm bảo thêm ngay block phía trên origin trước khi BFS.
     */
    private List<BlockPos> collectTall(World world, BlockPos origin,
                                       BlockState target, int maxBlocks) {
        List<BlockPos> result  = new ArrayList<>();
        Set<BlockPos>  visited = new HashSet<>();
        Deque<BlockPos> queue  = new ArrayDeque<>();

        visited.add(origin);
        queue.add(origin);

        // Ngay block phía trên origin
        BlockPos originAbove = origin.up();
        if (world.getBlockState(originAbove).getBlock() == target.getBlock()) {
            visited.add(originAbove);
            result.add(originAbove);
            queue.add(originAbove);
        }

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            BlockPos cur = queue.poll();
            for (int[] d : HORIZ) {
                BlockPos nb = cur.add(d[0], 0, d[2]);
                if (!visited.add(nb)) continue;
                if (world.getBlockState(nb).getBlock() != target.getBlock()) continue;

                result.add(nb);
                queue.add(nb);
                if (result.size() >= maxBlocks) break;

                // Kéo thêm block phía trên
                BlockPos above = nb.up();
                if (visited.add(above)
                        && world.getBlockState(above).getBlock() == target.getBlock()) {
                    result.add(above);
                    queue.add(above);
                }
            }
        }

        result.sort(Comparator.comparingInt(BlockPos::getY));
        return result;
    }

    // ── TREE_CAP ─────────────────────────────────────────────────────────────

    /**
     * BFS theo log (D26). Nếu block không phải log thì dùng sameBlock thường.
     * Lá cây được gom thêm vào kết quả nhưng không mở rộng BFS thêm.
     */
    private List<BlockPos> collectTree(World world, BlockPos origin,
                                       BlockState target, int maxBlocks) {
        if (!target.isIn(BlockTags.LOGS)) {
            return Traversal.bfs(world, origin, maxBlocks,
                                 Traversal.D26, FilterModeManager.sameBlock(target));
        }

        List<BlockPos>  result  = new ArrayList<>();
        Set<BlockPos>   visited = new HashSet<>();
        Deque<BlockPos> queue   = new ArrayDeque<>();

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
                    queue.add(nb); // log tiếp tục mở rộng BFS
                } else if (nbState.isIn(BlockTags.LEAVES) && result.size() < maxBlocks) {
                    result.add(nb); // lá: gom vào nhưng không BFS tiếp
                }
            }
        }

        result.sort(Comparator.comparingInt(BlockPos::getY));
        return result;
    }
}
