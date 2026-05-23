package com.tcveinminer.engine.strategy;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;

public final class StairModeManager implements MiningStrategy {

    public static final String ID_UP   = "STAIR_UP";
    public static final String ID_DOWN = "STAIR_DOWN";
    private static final int[][] D6 = {{1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}};

    private final int    dy;
    private final String id;

    public StairModeManager(int dy) {
        this.dy = dy;
        this.id = dy > 0 ? ID_UP : ID_DOWN;
    }

    @Override public String getId()    { return id; }
    @Override public String getLabel() { return dy > 0 ? "Stair Up" : "Stair Down"; }
    @Override public String getIcon()  { return dy > 0 ? "⬆" : "⬇"; }
    @Override public FilterModeManager.MiningMode getModeType() { return FilterModeManager.MiningMode.TUNNEL; }

    private record Node(BlockPos pos, int depth) {}

    @Override
    public List<BlockPos> collectBlocks(MiningRequest req) {
        List<BlockPos>  result  = new ArrayList<>();
        Set<BlockPos>   visited = new HashSet<>();
        Deque<Node> queue = new ArrayDeque<>();

        visited.add(req.origin());
        queue.add(new Node(req.origin(), 0));

        // CHỮA LỖI: Lấy Direction chính xác thay vì dùng Vec3i
        Direction forwardDir = (req.orientCtx().hitFace == Direction.UP || req.orientCtx().hitFace == Direction.DOWN)
                ? req.orientCtx().playerFacing
                : req.orientCtx().hitFace.getOpposite();

        while (!queue.isEmpty() && result.size() < req.maxBlocks()) {
            Node cur = queue.poll();

            // 1. Lan 6 mặt
            for (int[] d : D6) {
                if (result.size() >= req.maxBlocks()) break;
                BlockPos nb = cur.pos.add(d[0], d[1], d[2]);
                if (!visited.add(nb)) continue;

                BlockState state = req.world().getBlockState(nb);
                int dist = (int) Math.sqrt(nb.getSquaredDistance(req.origin()));
                FilterModeManager.FilterContext ctx = new FilterModeManager.FilterContext(
                        req.world(), req.player(), req.tool(), req.origin(), nb,
                        req.targetState(), state, Direction.fromVector(d[0], d[1], d[2]),
                        cur.depth + 1, dist, result.size(), getModeType(), req.cache()
                );

                if (req.filter().test(ctx)) {
                    result.add(nb);
                    queue.add(new Node(nb, cur.depth + 1));
                }
            }

            // 2. Bước cầu thang chính
            BlockPos step = cur.pos.add(
                    req.orientCtx().forward.getX() + req.orientCtx().up.getX() * dy,
                    req.orientCtx().forward.getY() + req.orientCtx().up.getY() * dy,
                    req.orientCtx().forward.getZ() + req.orientCtx().up.getZ() * dy
            );

            if (visited.add(step)) {
                BlockState state = req.world().getBlockState(step);
                int dist = (int) Math.sqrt(step.getSquaredDistance(req.origin()));

                // CHỮA LỖI TẠI ĐÂY: Truyền forwardDir thay vì req.orientCtx().forward
                FilterModeManager.FilterContext ctx = new FilterModeManager.FilterContext(
                        req.world(), req.player(), req.tool(), req.origin(), step,
                        req.targetState(), state, forwardDir,
                        cur.depth + 1, dist, result.size(), getModeType(), req.cache()
                );

                if (req.filter().test(ctx)) {
                    result.add(step);
                    queue.add(new Node(step, cur.depth + 1));
                }
            }
        }

        result.sort(dy > 0 ? Comparator.comparingInt(BlockPos::getY) : Comparator.comparingInt((BlockPos p) -> p.getY()).reversed());
        return result;
    }
}