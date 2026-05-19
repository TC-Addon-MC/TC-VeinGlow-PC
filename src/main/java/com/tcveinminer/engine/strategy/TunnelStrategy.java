package com.tcveinminer.engine.strategy;

import com.tcveinminer.engine.traversal.OrientationContext;
import com.tcveinminer.engine.traversal.Traversal;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * TunnelStrategy: 1×2 (wide × tall) tunnel extending in the player's forward direction.
 * Uses OrientationContext so the tunnel aligns with where the player is facing —
 * not hardcoded to Z axis.
 */
public final class TunnelStrategy implements MiningStrategy {
    public static final String ID = "TUNNEL_1x2";

    @Override public String getId()    { return ID; }
    @Override public String getLabel() { return "Tunnel 1×2"; }
    @Override public String getIcon()  { return "🚇"; }

    @Override
    public List<BlockPos> collectBlocks(World world, BlockPos origin, BlockState target,
                                         int maxBlocks, OrientationContext ctx) {
        // depth = how many blocks we can fit in the 1×2 cross-section
        int depth = Math.max(1, maxBlocks / 2);
        // halfW=0 halfH=0: 1 wide, 2 tall (0 to +1 in up axis)
        // Build manually so we can have asymmetric height (feet+head)
        int[][] offsets = buildTunnel1x2(ctx, depth, maxBlocks);
        return Traversal.collectBox(world, origin, offsets, maxBlocks, Traversal.notAir());
    }

    private static int[][] buildTunnel1x2(OrientationContext ctx, int depth, int maxBlocks) {
        // 2 blocks per depth layer (feet + head height)
        int capacity = Math.min(depth * 2, maxBlocks);
        int[][] offsets = new int[capacity][3];
        int idx = 0;
        for (int f = 1; f <= depth && idx < capacity; f++) {
            // Foot level
            BlockPos foot = ctx.offset(BlockPos.ORIGIN, f, 0, 0);
            offsets[idx++] = new int[]{foot.getX(), foot.getY(), foot.getZ()};
            if (idx >= capacity) break;
            // Head level (+1 up)
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
