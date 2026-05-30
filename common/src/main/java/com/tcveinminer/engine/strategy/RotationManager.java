package com.tcveinminer.engine.strategy;

import com.tcveinminer.engine.traversal.OrientationContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý các chế độ xoay mặt theo hướng người chơi.
 *
 * Cung cấp:
 *   - fromPlayer()    : tạo OrientationContext từ pitch + yaw của player
 *   - from()          : tạo từ hitFace + playerFacing tường minh
 *   - planeOffsets()  : mảng offset 2D trong mặt phẳng hitFace (dùng cho Shape/Area)
 *   - boxOffsets()    : mảng offset 3D có chiều sâu cố định (dùng cho Shape 3D)
 *
 * Tất cả rotation logic đều đi qua đây — các Strategy không cần biết
 * OrientationContext hoạt động ra sao bên trong.
 */
public final class RotationManager {

    /**
     * Xây OrientationContext từ player hiện tại.
     * Pitch > 60° → đang nhìn sàn (hitFace = UP).
     * Pitch < -60° → đang nhìn trần (hitFace = DOWN).
     * Còn lại → tường theo yaw.
     */
    public static OrientationContext fromPlayer(PlayerEntity player) {
        Direction hitFace = approximateHitFace(player);
        Direction facing  = OrientationContext.facingFromYaw(player.getYaw());
        return OrientationContext.of(hitFace, facing);
    }

    /** Xây OrientationContext từ hitFace + playerFacing tường minh. */
    public static OrientationContext from(Direction hitFace, Direction playerFacing) {
        return OrientationContext.of(hitFace, playerFacing);
    }

    /**
     * Tạo mảng offset 2D trong mặt phẳng hitFace.
     * halfSide=1 → 3×3 (8 block xung quanh origin).
     * halfSide=2 → 5×5 (24 block xung quanh origin).
     * Origin (s=0, u=0) luôn bị loại khỏi kết quả.
     */
    public static int[][] planeOffsets(OrientationContext ctx, int halfSide) {
        int side = halfSide * 2 + 1;
        int[][] offsets = new int[side * side - 1][3];
        int idx = 0;
        for (int s = -halfSide; s <= halfSide; s++) {
            for (int u = -halfSide; u <= halfSide; u++) {
                if (s == 0 && u == 0) continue; // skip origin
                BlockPos off = ctx.planeOffset(BlockPos.ORIGIN, s, u);
                offsets[idx++] = new int[]{off.getX(), off.getY(), off.getZ()};
            }
        }
        return offsets;
    }

    /**
     * Tạo mảng offset 3D: tiết diện (sMin..sMax) × (uMin..uMax), kéo dài depth tầng.
     * Dùng cho tunnel có chiều sâu cố định. Depth bắt đầu từ f=1 (bỏ qua origin).
     */
    public static int[][] boxOffsets(OrientationContext ctx,
                                     int sMin, int sMax, int uMin, int uMax, int depth) {
        List<int[]> list = new ArrayList<>();
        for (int f = 1; f <= depth; f++) {
            for (int s = sMin; s <= sMax; s++) {
                for (int u = uMin; u <= uMax; u++) {
                    BlockPos off = ctx.offset(BlockPos.ORIGIN, f, s, u);
                    list.add(new int[]{off.getX(), off.getY(), off.getZ()});
                }
            }
        }
        return list.toArray(new int[0][]);
    }

    public static Direction approximateHitFace(PlayerEntity player) {
        float pitch = player.getPitch();
        if (pitch > 60f)  return Direction.UP;
        if (pitch < -60f) return Direction.DOWN;
        // Thay đổi ở đây: lấy hướng ngược lại cho mặt tường
        return OrientationContext.facingFromYaw(player.getYaw()).getOpposite();
    }

    private RotationManager() {}
}
