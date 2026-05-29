package com.tcveinminer.engine.strategy;

import com.tcveinminer.engine.traversal.OrientationContext;
import com.tcveinminer.engine.traversal.TraversalUtils;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.item.Item;
import com.tcveinminer.engine.state.EngineState;

import java.util.*;

public abstract class BaseBfsStrategy implements MiningStrategy {

    protected record SearchNode(BlockPos pos, int depth, int f, int s, int u, Direction approachDir) {}

    /**
     * Mặc định sử dụng D26 để lan truyền (hỗ trợ lan qua góc chéo).
     */
    protected int[][] getSpreadDirections() {
        return TraversalUtils.D26;
    }

    /**
     * Cờ đánh dấu chế độ chặt cây, nếu true sẽ không lây lan tiếp từ khối lá.
     */
    protected boolean isTreeMode() {
        return false;
    }

    /**
     * Kiểm tra xem toạ độ block (hoặc toạ độ tương đối f, s, u) có nằm trong hình dáng lây lan không.
     * @param f Forward (chiều sâu dọc theo hướng nhìn)
     * @param s Side (chiều ngang)
     * @param u Up (chiều dọc)
     */
    protected abstract boolean isWithinShape(BlockPos pos, BlockPos origin, OrientationContext ctx, int f, int s, int u);

    /**
     * Kiểm tra xem có nên tiếp tục thêm block này vào hàng đợi để lan truyền tiếp không.
     * Sử dụng để thiết lập điều kiện dừng (vd: khoảng trống quá lớn).
     * @param passedFilter Block này có vượt qua filter (được đào) hay không.
     * @param maxSolidF Giá trị F (forward) lớn nhất đã tìm thấy khối rắn hợp lệ.
     */
    protected boolean shouldQueue(SearchNode node, BlockState state, MiningRequest req, int maxSolidF, int maxSolidDepth, boolean passedFilter) {
        return passedFilter;
    }

    @Override
    public List<BlockPos> collectBlocks(MiningRequest req) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Deque<SearchNode> queue = new ArrayDeque<>();

        visited.add(req.origin());
        queue.add(new SearchNode(req.origin(), 0, 0, 0, 0, null));

        int maxSolidF = 0;
        int maxSolidDepth = 0;
        Direction forwardDir = req.orientCtx().playerFacing;
        int[][] directions = getSpreadDirections();
        boolean isTree = isTreeMode();

        while (!queue.isEmpty() && result.size() < req.maxBlocks()) {
            SearchNode cur = queue.poll();

            for (int[] d : directions) {
                BlockPos nb = cur.pos().add(d[0], d[1], d[2]);
                if (!visited.add(nb)) continue;

                int[] proj = TraversalUtils.project(nb, req.origin(), req.orientCtx());
                int f = proj[0], s = proj[1], u = proj[2];

                if (!isWithinShape(nb, req.origin(), req.orientCtx(), f, s, u)) continue;

                if (!req.world().isChunkLoaded(nb.getX() >> 4, nb.getZ() >> 4)) continue;
                BlockState nbState = req.world().getBlockState(nb);

                Direction approach = TraversalUtils.getApproachDirection(d[0], d[1], d[2]);
                double distance = Math.sqrt(nb.getSquaredDistance(req.origin()));

                FilterModeManager.FilterContext fCtx = new FilterModeManager.FilterContext(
                        req.world(), req.player(), req.tool(), req.origin(), nb,
                        req.targetState(), nbState, approach, cur.depth() + 1, distance,
                        result.size(), getModeType(), req.cache(), req.blacklist(), req.requireHarvestCapability()
                );

                boolean passedFilter = false;

                if (req.filter().test(fCtx)) {
                    passedFilter = true;
                    result.add(nb);
                    if (f > maxSolidF) maxSolidF = f;
                    if (cur.depth() + 1 > maxSolidDepth) maxSolidDepth = cur.depth() + 1;

                    if (result.size() >= req.maxBlocks()) {
                        return result;
                    }
                }

                SearchNode nextNode = new SearchNode(nb, cur.depth() + 1, f, s, u, approach);

                if (isTree && passedFilter && nbState.isIn(BlockTags.LEAVES)) {
                    // TreeCapitator logic: lá không lan tiếp
                    continue;
                }

                if (shouldQueue(nextNode, nbState, req, maxSolidF, maxSolidDepth, passedFilter)) {
                    queue.add(nextNode);
                }
            }
        }

        return result;
    }
}
