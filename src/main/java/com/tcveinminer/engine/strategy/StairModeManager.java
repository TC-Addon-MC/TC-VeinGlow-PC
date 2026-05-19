package com.tcveinminer.engine.strategy;

import com.tcveinminer.engine.traversal.OrientationContext;
import com.tcveinminer.engine.traversal.Traversal;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

/**
 * Quản lý chế độ cầu thang (Stair).
 *
 * Mỗi bước: BFS lan 6 mặt + thêm bước đi forward+dy theo hướng người chơi.
 *   dy = +1 → cầu thang đi lên
 *   dy = -1 → cầu thang đi xuống
 *
 * Tạo instance:
 *   new StairModeManager(+1)  → STAIR_UP
 *   new StairModeManager(-1)  → STAIR_DOWN
 */
public final class StairModeManager implements MiningStrategy {

    public static final String ID_UP   = "STAIR_UP";
    public static final String ID_DOWN = "STAIR_DOWN";

    private final int    dy; // +1 = lên, -1 = xuống
    private final String id;

    public StairModeManager(int dy) {
        this.dy = dy;
        this.id = dy > 0 ? ID_UP : ID_DOWN;
    }

    @Override public String getId()    { return id; }
    @Override public String getLabel() { return dy > 0 ? "Stair Up" : "Stair Down"; }
    @Override public String getIcon()  { return dy > 0 ? "⬆" : "⬇"; }

    @Override
    public List<BlockPos> collectBlocks(World world, BlockPos origin, BlockState target,
                                        int maxBlocks, OrientationContext ctx) {
        List<BlockPos>  result  = new ArrayList<>();
        Set<BlockPos>   visited = new HashSet<>();
        Deque<BlockPos> queue   = new ArrayDeque<>();

        visited.add(origin);
        queue.add(origin);

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            BlockPos cur = queue.poll();

            // Lan 6 mặt: block cùng loại
            for (int[] d : Traversal.D6) {
                if (result.size() >= maxBlocks) break;
                BlockPos nb = cur.add(d[0], d[1], d[2]);
                if (!visited.add(nb)) continue;
                if (world.getBlockState(nb).getBlock() != target.getBlock()) continue;
                result.add(nb);
                queue.add(nb);
            }

            // Bước cầu thang: tiến 1 bước forward + dy bước dọc
            BlockPos step = cur.add(
                ctx.forward.getX() + ctx.up.getX() * dy,
                ctx.forward.getY() + ctx.up.getY() * dy,
                ctx.forward.getZ() + ctx.up.getZ() * dy
            );
            if (visited.add(step)
                    && world.getBlockState(step).getBlock() == target.getBlock()) {
                result.add(step);
                queue.add(step);
            }
        }

        // Sắp xếp theo Y để đào từ thấp lên (hoặc từ cao xuống)
        result.sort(dy > 0
                ? Comparator.comparingInt(BlockPos::getY)
                : Comparator.comparingInt((BlockPos p) -> p.getY()).reversed());
        return result;
    }
}
