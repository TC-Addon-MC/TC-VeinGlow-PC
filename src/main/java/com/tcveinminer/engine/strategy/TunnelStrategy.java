package com.tcveinminer.engine.strategy;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

/**
 * TunnelStrategy (1×2 Tunnel): đào thẳng về phía trước theo hướng player.
 * Mỗi bước: block tại chân + block phía trên (1 rộng × 2 cao).
 * BFS theo trục Z (depth) hoặc X, giữ nguyên Y.
 * Chỉ match cùng loại block — không đào bừa.
 */
public final class TunnelStrategy implements MiningStrategy {
    public static final String ID = "TUNNEL_1x2";

    @Override public String getId()    { return ID; }
    @Override public String getLabel() { return "Tunnel 1×2"; }
    @Override public String getIcon()  { return "🚇"; }

    // 4 hướng ngang (không Y)
    private static final int[][] HORIZ = {{1,0,0},{-1,0,0},{0,0,1},{0,0,-1}};

    @Override
    public List<BlockPos> collectBlocks(World world, BlockPos origin, BlockState target, int maxBlocks) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        visited.add(origin);
        queue.add(origin);

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            BlockPos cur = queue.poll();

            for (int[] d : HORIZ) {
                // Block chân
                BlockPos foot = cur.add(d[0], 0, d[2]);
                if (visited.add(foot) && world.getBlockState(foot).getBlock() == target.getBlock()) {
                    result.add(foot);
                    queue.add(foot);
                    if (result.size() >= maxBlocks) return result;
                }

                // Block đầu (trên chân 1)
                BlockPos head = cur.add(d[0], 1, d[2]);
                if (visited.add(head) && world.getBlockState(head).getBlock() == target.getBlock()) {
                    result.add(head);
                    if (result.size() >= maxBlocks) return result;
                }
            }

            // Thêm block trên origin vào tunnel
            BlockPos above = cur.up();
            if (visited.add(above) && world.getBlockState(above).getBlock() == target.getBlock()) {
                result.add(above);
                queue.add(above);
                if (result.size() >= maxBlocks) return result;
            }
        }

        return result;
    }
}
