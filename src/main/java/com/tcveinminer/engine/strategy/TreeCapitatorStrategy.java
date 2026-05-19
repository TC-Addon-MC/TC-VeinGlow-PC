package com.tcveinminer.engine.strategy;

import com.tcveinminer.engine.traversal.OrientationContext;
import com.tcveinminer.engine.traversal.Traversal;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public final class TreeCapitatorStrategy implements MiningStrategy {
    public static final String ID = "TREE_CAP";
    @Override public String getId()    { return ID; }
    @Override public String getLabel() { return "TreeCapitator"; }
    @Override public String getIcon()  { return "🌳"; }

    @Override
    public List<BlockPos> collectBlocks(World world, BlockPos origin, BlockState target,
                                         int maxBlocks, OrientationContext ctx) {
        if (!target.isIn(BlockTags.LOGS)) {
            return Traversal.bfs(world, origin, maxBlocks, Traversal.D26, Traversal.sameBlock(target));
        }

        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

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
                    queue.add(nb);
                } else if (nbState.isIn(BlockTags.LEAVES) && result.size() < maxBlocks) {
                    result.add(nb);
                }
            }
        }

        result.sort(Comparator.comparingInt(BlockPos::getY));
        return result;
    }
}
