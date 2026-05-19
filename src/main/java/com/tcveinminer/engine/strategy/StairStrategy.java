package com.tcveinminer.engine.strategy;

import com.tcveinminer.engine.traversal.OrientationContext;
import com.tcveinminer.engine.traversal.Traversal;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

/**
 * StairStrategy: stair diagonal +1 up or -1 down per step, oriented to player facing.
 */
public final class StairStrategy implements MiningStrategy {

    public static final String ID_UP   = "STAIR_UP";
    public static final String ID_DOWN = "STAIR_DOWN";

    private final int dy;
    private final String id;

    public StairStrategy(int dy) {
        this.dy = dy;
        this.id = dy > 0 ? ID_UP : ID_DOWN;
    }

    @Override public String getId()    { return id; }
    @Override public String getLabel() { return dy > 0 ? "Stair Up" : "Stair Down"; }
    @Override public String getIcon()  { return dy > 0 ? "⬆" : "⬇"; }

    @Override
    public List<BlockPos> collectBlocks(World world, BlockPos origin, BlockState target,
                                         int maxBlocks, OrientationContext ctx) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        visited.add(origin);
        queue.add(origin);

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            BlockPos cur = queue.poll();

            // Face-adjacent same-block spread
            for (int[] d : Traversal.D6) {
                BlockPos nb = cur.add(d[0], d[1], d[2]);
                if (!visited.add(nb)) continue;
                if (world.getBlockState(nb).getBlock() != target.getBlock()) continue;
                result.add(nb);
                queue.add(nb);
                if (result.size() >= maxBlocks) break;
            }

            // Stair step: forward 1 + dy vertical (uses orientation forward axis)
            BlockPos step = cur.add(
                ctx.forward.getX() + ctx.up.getX() * dy,
                ctx.forward.getY() + ctx.up.getY() * dy,
                ctx.forward.getZ() + ctx.up.getZ() * dy
            );
            if (visited.add(step) && world.getBlockState(step).getBlock() == target.getBlock()) {
                result.add(step);
                queue.add(step);
            }
        }

        result.sort(dy > 0
            ? Comparator.comparingInt(BlockPos::getY)
            : Comparator.comparingInt((BlockPos p) -> p.getY()).reversed());
        return result;
    }
}
