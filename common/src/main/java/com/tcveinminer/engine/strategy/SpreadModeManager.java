package com.tcveinminer.engine.strategy;

import com.tcveinminer.engine.traversal.OrientationContext;
import com.tcveinminer.engine.traversal.TraversalUtils;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.Item;
import com.tcveinminer.engine.state.EngineState;

import java.util.*;

public final class SpreadModeManager extends BaseBfsStrategy {

    public enum Mode { FACE, EDGES, CORNERS, TREE_CAP }

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

    @Override
    protected int[][] getSpreadDirections() {
        return switch (mode) {
            case FACE     -> TraversalUtils.D6;
            case EDGES    -> TraversalUtils.D18;
            case CORNERS, TREE_CAP -> TraversalUtils.D26;
        };
    }

    @Override
    protected boolean isWithinShape(BlockPos pos, BlockPos origin, OrientationContext ctx, int f, int s, int u) {
        return true; // Không giới hạn hình dáng, lan tuỳ ý
    }

    @Override
    protected boolean isTreeMode() {
        return mode == Mode.TREE_CAP;
    }

    @Override
    public List<BlockPos> collectBlocks(MiningRequest req) {
        if (mode == Mode.TREE_CAP) {
            return collectTree(req);
        }
        return super.collectBlocks(req);
    }

    private List<BlockPos> collectTree(MiningRequest req) {
        List<BlockPos> result = new ArrayList<>();
        Deque<SearchNode> queue = new ArrayDeque<>();

        // Giai đoạn 1: Tìm gỗ (D26)
        Set<BlockPos> visitedLogs = new HashSet<>();
        visitedLogs.add(req.origin());
        queue.add(new SearchNode(req.origin(), 0, 0, 0, 0, null));
        List<BlockPos> foundLogs = new ArrayList<>();

        while (!queue.isEmpty() && result.size() < req.maxBlocks()) {
            SearchNode cur = queue.poll();
            for (int[] d : TraversalUtils.D26) {
                BlockPos nb = cur.pos().add(d[0], d[1], d[2]);
                if (!visitedLogs.add(nb)) continue;

                if (!req.world().isChunkLoaded(nb.getX() >> 4, nb.getZ() >> 4)) continue;
                BlockState state = req.world().getBlockState(nb);
                if (!state.isIn(BlockTags.LOGS)) continue;

                int dist = Math.abs(nb.getX() - req.origin().getX()) + Math.abs(nb.getY() - req.origin().getY()) + Math.abs(nb.getZ() - req.origin().getZ());
                FilterModeManager.FilterContext fCtx = new FilterModeManager.FilterContext(
                        req.world(), req.player(), req.tool(), req.origin(), nb,
                        req.targetState(), state, TraversalUtils.getApproachDirection(d[0], d[1], d[2]), cur.depth() + 1, dist,
                        result.size(), getModeType(), req.cache(), req.blacklist(), req.requireHarvestCapability()
                );

                if (req.filter().test(fCtx)) {
                    result.add(nb);
                    foundLogs.add(nb);
                    queue.add(new SearchNode(nb, cur.depth() + 1, 0, 0, 0, null));
                }
            }
        }

        // Giai đoạn 2: Xác định loại lá đúng — lá tiếp xúc trực tiếp với gỗ
        net.minecraft.block.Block canonicalLeafBlock = null;
        outer:
        for (BlockPos log : foundLogs) {
            for (int[] d : TraversalUtils.D6) {
                BlockPos nb = log.add(d[0], d[1], d[2]);
                if (!req.world().isChunkLoaded(nb.getX() >> 4, nb.getZ() >> 4)) continue;
                BlockState state = req.world().getBlockState(nb);
                if (state.isIn(BlockTags.LEAVES)) {
                    canonicalLeafBlock = state.getBlock();
                    break outer;
                }
            }
        }
        // Cũng kiểm tra origin (gỗ ban đầu)
        if (canonicalLeafBlock == null) {
            for (int[] d : TraversalUtils.D6) {
                BlockPos nb = req.origin().add(d[0], d[1], d[2]);
                if (!req.world().isChunkLoaded(nb.getX() >> 4, nb.getZ() >> 4)) continue;
                BlockState state = req.world().getBlockState(nb);
                if (state.isIn(BlockTags.LEAVES)) {
                    canonicalLeafBlock = state.getBlock();
                    break;
                }
            }
        }

        // Giai đoạn 3: Lan lá chỉ cùng loại với lá tiếp xúc gỗ
        if (canonicalLeafBlock != null) {
            final net.minecraft.block.Block leafBlock = canonicalLeafBlock;
            Set<BlockPos> visitedLeaves = new HashSet<>();
            visitedLeaves.addAll(foundLogs);
            visitedLeaves.add(req.origin());

            queue.clear();
            for (BlockPos log : foundLogs) queue.add(new SearchNode(log, 0, 0, 0, 0, null));
            if (!foundLogs.contains(req.origin())) queue.add(new SearchNode(req.origin(), 0, 0, 0, 0, null));

            while (!queue.isEmpty() && result.size() < req.maxBlocks()) {
                SearchNode cur = queue.poll();
                for (int[] d : TraversalUtils.D6) {
                    BlockPos nb = cur.pos().add(d[0], d[1], d[2]);
                    if (!visitedLeaves.add(nb)) continue;

                    if (!req.world().isChunkLoaded(nb.getX() >> 4, nb.getZ() >> 4)) continue;
                    BlockState state = req.world().getBlockState(nb);
                    // Chỉ lấy đúng loại lá này, tránh lan sang cây khác
                    if (state.getBlock() != leafBlock) continue;

                    int dist = Math.abs(nb.getX() - req.origin().getX()) + Math.abs(nb.getY() - req.origin().getY()) + Math.abs(nb.getZ() - req.origin().getZ());
                    // Giới hạn lá không lan quá xa gỗ (depth từ gỗ gần nhất)
                    if (cur.depth() >= 6) continue;

                    FilterModeManager.FilterContext fCtx = new FilterModeManager.FilterContext(
                            req.world(), req.player(), req.tool(), req.origin(), nb,
                            req.targetState(), state, TraversalUtils.getApproachDirection(d[0], d[1], d[2]), cur.depth() + 1, dist,
                            result.size(), getModeType(), req.cache(), req.blacklist(), req.requireHarvestCapability()
                    );

                    if (req.filter().test(fCtx)) {
                        result.add(nb);
                        queue.add(new SearchNode(nb, cur.depth() + 1, 0, 0, 0, null));
                    }
                }
            }
        }

        result.sort(Comparator.comparingInt(BlockPos::getY));
        return result;
    }
}
