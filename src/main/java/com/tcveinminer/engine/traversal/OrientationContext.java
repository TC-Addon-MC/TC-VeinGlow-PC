package com.tcveinminer.engine.traversal;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

/**
 * Spatial orientation for direction-aware mining.
 *
 * Coordinate frame (right-hand rule):
 *   forward = depth axis (into the wall, away from player)
 *   up      = +Y world axis
 *   right   = cross(up, forward)  — gives correct handedness
 *
 * Wall face (NORTH/SOUTH/EAST/WEST):
 *   forward = hitFace.getOpposite()
 *   right   = cross(up, forward)
 *
 * Floor/ceiling (UP/DOWN):
 *   forward = playerFacing (horizontal)
 *   right   = cross(up, forward)
 *
 * "plane" for AreaStrategy = (right × up) plane at origin.
 */
public final class OrientationContext {

    public final Direction hitFace;
    public final Direction playerFacing;
    public final Vec3i forward;
    public final Vec3i right;
    public final Vec3i up;

    private OrientationContext(Direction hitFace, Direction playerFacing,
                               Vec3i forward, Vec3i right, Vec3i up) {
        this.hitFace      = hitFace;
        this.playerFacing = playerFacing;
        this.forward      = forward;
        this.right        = right;
        this.up           = up;
    }

    public static OrientationContext of(Direction hitFace, Direction playerFacing) {
        Vec3i worldUp = new Vec3i(0, 1, 0);
        Vec3i fwd;

        switch (hitFace) {
            case UP, DOWN ->
                // Floor/ceiling: use player's horizontal facing as depth axis
                    fwd = playerFacing.getVector();
            default ->
                // Wall: forward = direction INTO the wall
                    fwd = hitFace.getOpposite().getVector();
        }

        // right = cross(up, forward) — standard right-hand rule
        Vec3i r = cross(worldUp, fwd);

        return new OrientationContext(hitFace, playerFacing, fwd, r, worldUp);
    }

    public static OrientationContext defaultContext() {
        return of(Direction.NORTH, Direction.SOUTH);
    }

    /**
     * Map (sideOffset, upOffset) in the hit-face plane to world BlockPos offset.
     * AreaStrategy uses this: s = horizontal spread, u = vertical spread.
     */
    public BlockPos planeOffset(BlockPos origin, int s, int u) {
        return origin.add(
                right.getX() * s + this.up.getX() * u,
                right.getY() * s + this.up.getY() * u,
                right.getZ() * s + this.up.getZ() * u
        );
    }

    /**
     * Map (forward, side, up) to world BlockPos offset from origin.
     * TunnelStrategy uses this.
     */
    public BlockPos offset(BlockPos origin, int f, int s, int u) {
        return origin.add(
                forward.getX() * f + right.getX() * s + this.up.getX() * u,
                forward.getY() * f + right.getY() * s + this.up.getY() * u,
                forward.getZ() * f + right.getZ() * s + this.up.getZ() * u
        );
    }

    /** a × b */
    private static Vec3i cross(Vec3i a, Vec3i b) {
        return new Vec3i(
                a.getY() * b.getZ() - a.getZ() * b.getY(),
                a.getZ() * b.getX() - a.getX() * b.getZ(),
                a.getX() * b.getY() - a.getY() * b.getX()
        );
    }

    /**
     * Map Minecraft yaw → cardinal direction.
     * Minecraft yaw: 0 = south, 90 = west, 180 = north, 270 = east.
     */
    public static Direction facingFromYaw(float yaw) {
        float y = ((yaw % 360) + 360) % 360;
        if (y < 45 || y >= 315) return Direction.SOUTH;
        if (y < 135) return Direction.WEST;
        if (y < 225) return Direction.NORTH;
        return Direction.EAST;
    }
}