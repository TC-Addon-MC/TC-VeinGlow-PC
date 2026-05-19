package com.tcveinminer.engine.strategy;

import com.tcveinminer.engine.traversal.OrientationContext;
import com.tcveinminer.engine.traversal.Traversal;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.List;

public final class TunnelStrategy implements MiningStrategy {
    public static final String ID = "TUNNEL_1x2";

    @Override public String getId()    { return ID; }
    @Override public String getLabel() { return "Tunnel 1×2"; }
    @Override public String getIcon()  { return "🚇"; }

    @Override
    public List<BlockPos> collectBlocks(World world, BlockPos origin, BlockState target,
                                        int maxBlocks, OrientationContext ctx) {
        int depth = Math.max(1, maxBlocks / 2);
        int[][] offsets = buildTunnel1x2(ctx, depth, maxBlocks);
        // [ĐÃ SỬA]: Chỉ đào block cùng loại
        return Traversal.collectBox(world, origin, offsets, maxBlocks, Traversal.sameBlock(target));
    }

    private static int[][] buildTunnel1x2(OrientationContext ctx, int depth, int maxBlocks) {
        int capacity = Math.min((depth + 1) * 2, maxBlocks);
        int[][] offsets = new int[capacity][3];
        int idx = 0;

        // [ĐÃ SỬA]: Bắt đầu từ f = 0 để quét block ngay tại bề mặt
        for (int f = 0; f <= depth && idx < capacity; f++) {
            if (f != 0) { // Bỏ qua chân bề mặt vì đó là block gốc bạn vừa đập
                BlockPos foot = ctx.offset(BlockPos.ORIGIN, f, 0, 0);
                offsets[idx++] = new int[]{foot.getX(), foot.getY(), foot.getZ()};
            }
            if (idx >= capacity) break;

            // Block ngang tầm mắt (+1 up)
            BlockPos head = ctx.offset(BlockPos.ORIGIN, f, 0, 1);
            offsets[idx++] = new int[]{head.getX(), head.getY(), head.getZ()};
        }

        if (idx < capacity) {
            int[][] trimmed = new int[idx][3];
            System.arraycopy(offsets, 0, trimmed, 0, idx);
            return trimmed;
        }
        return offsets;
    }
}