package com.tcveinminer.engine.strategy;

import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;

public final class SpreadModeManager implements MiningStrategy {

    public enum Mode { FACE, EDGES, CORNERS, TREE_CAP }

    private static final int[][] D6  = {{1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}};
    private static final int[][] D18 = buildAdjacency(2);
    private static final int[][] D26 = buildAdjacency(1, 1); // Chỉ lấy neighbors ngay gần (1 block away trên mỗi trục)
    private static final int[][] HORIZ = {{1,0,0}, {-1,0,0}, {0,0,1}, {0,0,-1}};
    private static final int TREE_LEAF_RADIUS = 6;

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
    public FilterModeManager.MiningMode getModeType() {
        return mode == Mode.TREE_CAP ? FilterModeManager.MiningMode.TREE_CAPITATOR : FilterModeManager.MiningMode.VEIN;
    }

    private record SearchNode(BlockPos pos, int depth, Direction approachDir) {}

    @Override
    public List<BlockPos> collectBlocks(MiningRequest req) {
        return switch (mode) {
            case FACE     -> executeBfs(req, D6, false);
            case EDGES    -> executeBfs(req, D18, false); // Hoặc nạp D18 thật
            case CORNERS  -> executeBfs(req, D26, false); // Hoặc nạp D26 thật
            case TREE_CAP -> collectTree(req);
        };
    }

    private static int[][] buildAdjacency(int maxManhattan) {
        List<int[]> dirs = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int manhattan = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                    if (manhattan > 0 && manhattan <= maxManhattan) {
                        dirs.add(new int[]{dx, dy, dz});
                    }
                }
            }
        }
        return dirs.toArray(new int[0][]);
    }

    private static int[][] buildAdjacency(int maxManhattan, int maxChebyshev) {
        List<int[]> dirs = new ArrayList<>();
        for (int dx = -maxChebyshev; dx <= maxChebyshev; dx++) {
            for (int dy = -maxChebyshev; dy <= maxChebyshev; dy++) {
                for (int dz = -maxChebyshev; dz <= maxChebyshev; dz++) {
                    int manhattan = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                    if (manhattan > 0 && manhattan <= maxManhattan) {
                        dirs.add(new int[]{dx, dy, dz});
                    }
                }
            }
        }
        return dirs.toArray(new int[0][]);
    }

    private List<BlockPos> executeBfs(MiningRequest req, int[][] directions, boolean isTree) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Deque<SearchNode> queue = new ArrayDeque<>();

        visited.add(req.origin());
        queue.add(new SearchNode(req.origin(), 0, null));

        while (!queue.isEmpty() && result.size() < req.maxBlocks()) {
            SearchNode cur = queue.poll();

            for (int[] d : directions) {
                BlockPos nb = cur.pos.add(d[0], d[1], d[2]);
                if (!visited.add(nb)) continue;

                BlockState nbState = req.world().getBlockState(nb);
                int distance = Math.abs(nb.getX() - req.origin().getX()) + Math.abs(nb.getY() - req.origin().getY()) + Math.abs(nb.getZ() - req.origin().getZ()); // Manhattan
                Direction approach = Direction.fromVector(d[0], d[1], d[2]);

                FilterModeManager.FilterContext fCtx = new FilterModeManager.FilterContext(
                        req.world(), req.player(), req.tool(), req.origin(), nb,
                        req.targetState(), nbState, approach, cur.depth + 1, distance,
                        result.size(), getModeType(), req.cache(), req.blacklist(), req.requireCorrectTool()
                );

                if (req.filter().test(fCtx)) {
                    result.add(nb);
                    // Nếu là chế độ Tree và block này là Lá, không đưa vào hàng đợi BFS để tránh lan vô tận qua tán rừng.
                    if (isTree && nbState.isIn(BlockTags.LEAVES)) {
                        continue;
                    }
                    queue.add(new SearchNode(nb, cur.depth + 1, approach));
                }
            }
        }
        if (isTree) result.sort(Comparator.comparingInt(BlockPos::getY));
        return result;
    }

    private List<BlockPos> collectTree(MiningRequest req) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Deque<SearchNode> queue = new ArrayDeque<>();

        // Giai đoạn 1: Tìm gỗ (D26 để quấn quanh thân gỗ chéo)
        visited.add(req.origin());
        queue.add(new SearchNode(req.origin(), 0, null));
        List<BlockPos> foundLogs = new ArrayList<>();

        while (!queue.isEmpty() && result.size() < req.maxBlocks()) {
            SearchNode cur = queue.poll();
            for (int[] d : D26) {
                BlockPos nb = cur.pos.add(d[0], d[1], d[2]);
                if (!visited.add(nb)) continue;

                BlockState state = req.world().getBlockState(nb);
                if (!state.isIn(BlockTags.LOGS)) {
                    visited.remove(nb); // Để giai đoạn 2 có thể quét lá tại đây
                    continue;
                }

                int dist = Math.abs(nb.getX() - req.origin().getX()) + Math.abs(nb.getY() - req.origin().getY()) + Math.abs(nb.getZ() - req.origin().getZ());
                FilterModeManager.FilterContext fCtx = new FilterModeManager.FilterContext(
                        req.world(), req.player(), req.tool(), req.origin(), nb,
                        req.targetState(), state, Direction.fromVector(d[0], d[1], d[2]), cur.depth + 1, dist,
                        result.size(), getModeType(), req.cache(), req.blacklist(), req.requireCorrectTool()
                );

                if (req.filter().test(fCtx)) {
                    result.add(nb);
                    foundLogs.add(nb);
                    queue.add(new SearchNode(nb, cur.depth + 1, null));
                }
            }
        }

        // Giai đoạn 2: Tìm lá (D6 - Standard V1 từ gỗ)
        queue.clear();
        for (BlockPos log : foundLogs) queue.add(new SearchNode(log, 0, null));
        if (!foundLogs.contains(req.origin())) queue.add(new SearchNode(req.origin(), 0, null));

        while (!queue.isEmpty() && result.size() < req.maxBlocks()) {
            SearchNode cur = queue.poll();
            for (int[] d : D6) {
                BlockPos nb = cur.pos.add(d[0], d[1], d[2]);
                if (!visited.add(nb)) continue;

                BlockState state = req.world().getBlockState(nb);
                if (!state.isIn(BlockTags.LEAVES)) continue;

                int dist = Math.abs(nb.getX() - req.origin().getX()) + Math.abs(nb.getY() - req.origin().getY()) + Math.abs(nb.getZ() - req.origin().getZ());
                // Giới hạn lá không lan quá xa gỗ (depth từ gỗ gần nhất)
                if (cur.depth >= 4) continue; 

                FilterModeManager.FilterContext fCtx = new FilterModeManager.FilterContext(
                        req.world(), req.player(), req.tool(), req.origin(), nb,
                        req.targetState(), state, Direction.fromVector(d[0], d[1], d[2]), cur.depth + 1, dist,
                        result.size(), getModeType(), req.cache(), req.blacklist(), req.requireCorrectTool()
                );

                if (req.filter().test(fCtx)) {
                    result.add(nb);
                    queue.add(new SearchNode(nb, cur.depth + 1, null));
                }
            }
        }

        result.sort(Comparator.comparingInt(BlockPos::getY));
        return result;
    }

}
