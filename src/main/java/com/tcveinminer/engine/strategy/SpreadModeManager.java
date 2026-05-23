package com.tcveinminer.engine.strategy;

import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;

public final class SpreadModeManager implements MiningStrategy {

    public enum Mode { FACE, EDGES, CORNERS, TALL, TREE_CAP }

    private static final int[][] D6  = {{1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}};
    private static final int[][] D18 = buildAdjacency(2);
    private static final int[][] D26 = buildAdjacency(3);
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
            case TALL     -> collectTall(req);
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
                        result.size(), getModeType(), req.cache(), req.blacklist()
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
        Set<BlockPos> visitedLogs = new HashSet<>();
        Set<BlockPos> added = new HashSet<>();
        Deque<SearchNode> queue = new ArrayDeque<>();

        visitedLogs.add(req.origin());
        queue.add(new SearchNode(req.origin(), 0, null));

        while (!queue.isEmpty() && result.size() < req.maxBlocks()) {
            SearchNode cur = queue.poll();

            for (int[] d : D26) {
                BlockPos nb = cur.pos.add(d[0], d[1], d[2]);
                if (!visitedLogs.add(nb)) continue;

                BlockState nbState = req.world().getBlockState(nb);
                if (!nbState.isIn(BlockTags.LOGS)) continue;

                int distance = Math.abs(nb.getX() - req.origin().getX())
                        + Math.abs(nb.getY() - req.origin().getY())
                        + Math.abs(nb.getZ() - req.origin().getZ());
                Direction approach = Direction.fromVector(d[0], d[1], d[2]);

                FilterModeManager.FilterContext fCtx = new FilterModeManager.FilterContext(
                        req.world(), req.player(), req.tool(), req.origin(), nb,
                        req.targetState(), nbState, approach, cur.depth + 1, distance,
                        result.size(), getModeType(), req.cache(), req.blacklist()
                );

                if (req.filter().test(fCtx) && added.add(nb)) {
                    result.add(nb);
                    queue.add(new SearchNode(nb, cur.depth + 1, approach));
                }
            }
        }

        List<BlockPos> logs = new ArrayList<>(added);
        logs.add(req.origin());
        for (BlockPos logPos : logs) {
            collectLeavesAroundLog(req, logPos, result, added);
            if (result.size() >= req.maxBlocks()) break;
        }

        result.sort(Comparator.comparingInt(BlockPos::getY));
        return result;
    }

    private void collectLeavesAroundLog(MiningRequest req, BlockPos logPos, List<BlockPos> result, Set<BlockPos> added) {
        for (int dx = -TREE_LEAF_RADIUS; dx <= TREE_LEAF_RADIUS; dx++) {
            for (int dy = -TREE_LEAF_RADIUS; dy <= TREE_LEAF_RADIUS; dy++) {
                for (int dz = -TREE_LEAF_RADIUS; dz <= TREE_LEAF_RADIUS; dz++) {
                    if (result.size() >= req.maxBlocks()) return;
                    int distanceFromLog = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                    if (distanceFromLog > TREE_LEAF_RADIUS) continue;

                    BlockPos leafPos = logPos.add(dx, dy, dz);
                    if (!added.add(leafPos)) continue;

                    BlockState leafState = req.world().getBlockState(leafPos);
                    if (!leafState.isIn(BlockTags.LEAVES)) continue;

                    int distance = Math.abs(leafPos.getX() - req.origin().getX())
                            + Math.abs(leafPos.getY() - req.origin().getY())
                            + Math.abs(leafPos.getZ() - req.origin().getZ());

                    FilterModeManager.FilterContext fCtx = new FilterModeManager.FilterContext(
                            req.world(), req.player(), req.tool(), req.origin(), leafPos,
                            req.targetState(), leafState, Direction.UP, 0, distance,
                            result.size(), getModeType(), req.cache(), req.blacklist()
                    );

                    if (req.filter().test(fCtx)) {
                        result.add(leafPos);
                    }
                }
            }
        }
    }

    private List<BlockPos> collectTall(MiningRequest req) {
        // Logic Tall giữ cấu trúc BFS tương tự executeBfs, nhưng nạp offset HORIZ và tự động check block Y+1.
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Deque<SearchNode> queue = new ArrayDeque<>();

        visited.add(req.origin());
        queue.add(new SearchNode(req.origin(), 0, null));

        // Hàm helper test filter
        var testAndAdd = new java.util.function.BiConsumer<BlockPos, SearchNode>() {
            @Override
            public void accept(BlockPos pos, SearchNode parent) {
                if (!visited.add(pos)) return;
                BlockState state = req.world().getBlockState(pos);
                int dist = (int) Math.sqrt(pos.getSquaredDistance(req.origin()));
                FilterModeManager.FilterContext ctx = new FilterModeManager.FilterContext(
                        req.world(), req.player(), req.tool(), req.origin(), pos,
                        req.targetState(), state, Direction.UP, parent.depth + 1, dist,
                        result.size(), getModeType(), req.cache(), req.blacklist()
                );
                if (req.filter().test(ctx)) {
                    result.add(pos);
                    queue.add(new SearchNode(pos, parent.depth + 1, Direction.UP));
                }
            }
        };

        testAndAdd.accept(req.origin().up(), new SearchNode(req.origin(), 0, null));

        while (!queue.isEmpty() && result.size() < req.maxBlocks()) {
            SearchNode cur = queue.poll();
            for (int[] d : HORIZ) {
                BlockPos nb = cur.pos.add(d[0], 0, d[2]);
                testAndAdd.accept(nb, cur);
                testAndAdd.accept(nb.up(), cur);
            }
        }
        result.sort(Comparator.comparingInt(BlockPos::getY));
        return result;
    }
}
