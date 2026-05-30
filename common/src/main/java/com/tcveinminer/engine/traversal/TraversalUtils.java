package com.tcveinminer.engine.traversal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

public final class TraversalUtils {

    public static final int[][] D6  = {{1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}};
    public static final int[][] D18 = buildAdjacency(2, 1);
    public static final int[][] D26 = buildAdjacency(3, 1);

    private TraversalUtils() {}

    private static int[][] buildAdjacency(int maxManhattan, int maxChebyshev) {
        List<int[]> dirs = new ArrayList<>();
        for (int dx = -maxChebyshev; dx <= maxChebyshev; dx++) {
            for (int dy = -maxChebyshev; dy <= maxChebyshev; dy++) {
                for (int dz = -maxChebyshev; dz <= maxChebyshev; dz++) {
                    int manhattan = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                    if (manhattan > 0 && manhattan <= maxManhattan) {
                        dirs.add(new int[]{dx, dy, dz});
                    }
                }
            }
        }
        return dirs.toArray(new int[0][]);
    }

    public static Direction getApproachDirection(int dx, int dy, int dz) {
        Direction dir = Direction.fromVector(dx, dy, dz);
        if (dir != null) return dir;
        if (dy != 0) return dy > 0 ? Direction.UP : Direction.DOWN;
        if (dz != 0) return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        if (dx != 0) return dx > 0 ? Direction.EAST : Direction.WEST;
        return Direction.UP;
    }

    public static int[] project(BlockPos pos, BlockPos origin, OrientationContext ctx) {
        int dx = pos.getX() - origin.getX();
        int dy = pos.getY() - origin.getY();
        int dz = pos.getZ() - origin.getZ();
        
        int f = dx * ctx.forward.getX() + dy * ctx.forward.getY() + dz * ctx.forward.getZ();
        int s = dx * ctx.right.getX() + dy * ctx.right.getY() + dz * ctx.right.getZ();
        int u = dx * ctx.up.getX() + dy * ctx.up.getY() + dz * ctx.up.getZ();
        return new int[]{f, s, u};
    }
}
