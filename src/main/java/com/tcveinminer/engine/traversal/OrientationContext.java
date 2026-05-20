package com.tcveinminer.engine.traversal;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

/**
 * Spatial orientation for direction-aware mining.
 */
public final class OrientationContext {

    public final Direction hitFace;
    public final Direction playerFacing;
    public final Vec3i forward;
    public final Vec3i right;
    public final Vec3i up;
    public final Vec3i planeUp; // Thuộc tính mới cho việc trải lưới Area

    private OrientationContext(Direction hitFace, Direction playerFacing,
                               Vec3i forward, Vec3i right, Vec3i up, Vec3i planeUp) {
        this.hitFace      = hitFace;
        this.playerFacing = playerFacing;
        this.forward      = forward;
        this.right        = right;
        this.up           = up;
        this.planeUp      = planeUp;
    }

    public static OrientationContext of(Direction hitFace, Direction playerFacing) {
        Vec3i worldUp = new Vec3i(0, 1, 0);
        Vec3i fwd;
        Vec3i planeUp;

        switch (hitFace) {
            case UP, DOWN -> {
                // Sàn/Trần: sử dụng hướng nhìn ngang của người chơi làm trục chiều sâu
                fwd = playerFacing.getVector();
                planeUp = playerFacing.getVector(); // Trải phẳng theo hướng người chơi nhìn
            }
            default -> {
                // Tường: hướng đi sâu vào trong lòng tường
                fwd = hitFace.getOpposite().getVector();
                planeUp = worldUp; // Giữ nguyên worldUp cho các mặt tường đứng
            }
        }

        // Sửa Bug 2: Đổi thứ tự để đảm bảo quy tắc bàn tay phải
        Vec3i r = cross(fwd, worldUp);

        return new OrientationContext(hitFace, playerFacing, fwd, r, worldUp, planeUp);
    }

    public static OrientationContext defaultContext() {
        return of(Direction.NORTH, Direction.SOUTH);
    }

    /**
     * Map (sideOffset, upOffset) in the hit-face plane to world BlockPos offset.
     * Sử dụng planeUp thay vì up cố định.
     */
    public BlockPos planeOffset(BlockPos origin, int s, int u) {
        return origin.add(
                right.getX() * s + this.planeUp.getX() * u,
                right.getY() * s + this.planeUp.getY() * u,
                right.getZ() * s + this.planeUp.getZ() * u
        );
    }

    /**
     * Map (forward, side, up) to world BlockPos offset from origin.
     * Giữ nguyên up (worldUp) để đảm bảo chiều cao các mode Tunnel/Stair chính xác.
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
     */
    public static Direction facingFromYaw(float yaw) {
        float y = ((yaw % 360) + 360) % 360;
        if (y < 45 || y >= 315) return Direction.SOUTH;
        if (y < 135) return Direction.WEST;
        if (y < 225) return Direction.NORTH;
        return Direction.EAST;
    }
}