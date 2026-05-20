package com.tcveinminer.engine.strategy;

import com.tcveinminer.engine.traversal.OrientationContext;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý các chế độ đào theo hình thù.
 *
 * Truyền vào mảng 2D hoặc 3D định nghĩa hình dạng:
 *   1 = đào block này, 0 = bỏ qua
 *   Ô trung tâm luôn là origin (block vừa đập) — tự động bị loại.
 *
 * Dùng factory:
 *   ShapeModeManager.from2D(id, label, icon, new int[][]{ {1,1,1}, {1,1,1}, {1,1,1} })
 *     → 3×3 trong mặt phẳng hitFace, xoay theo hướng người chơi
 *
 *   ShapeModeManager.from3D(id, label, icon, new int[][][]{ ... })
 *     → 3×3×3 theo không gian 3D, xoay theo hướng người chơi
 *
 * Hình thù xoay tự động theo OrientationContext — không cần tự tính toán hướng.
 */
public final class ShapeModeManager implements MiningStrategy {

    private enum ShapeType { PLANE_2D, BOX_3D }

    private final String    id, label, icon;
    private final ShapeType type;
    /**
     * Với PLANE_2D: mỗi phần tử là {s, u} (side, up) trong mặt phẳng hitFace.
     * Với BOX_3D:   mỗi phần tử là {f, s, u} (forward, side, up).
     */
    private final int[][]   relOffsets;

    private ShapeModeManager(String id, String label, String icon,
                              ShapeType type, int[][] relOffsets) {
        this.id         = id;
        this.label      = label;
        this.icon       = icon;
        this.type       = type;
        this.relOffsets = relOffsets;
    }

    // ── Factory: 2D ──────────────────────────────────────────────────────────

    /**
     * Tạo chế độ đào từ mảng 2D.
     * shape[row][col] — ô trung tâm = (rows/2, cols/2).
     * Hàng 0 = trên cùng, cột 0 = trái.
     */
    public static ShapeModeManager from2D(String id, String label, String icon, int[][] shape) {
        int centerRow = shape.length / 2;
        int centerCol = shape[0].length / 2;
        List<int[]> offsets = new ArrayList<>();
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] == 0) continue;
                int s = c - centerCol;     // trục ngang (right)
                int u = centerRow - r;     // trục dọc  (up)
                if (s == 0 && u == 0) continue; // bỏ origin
                offsets.add(new int[]{s, u});
            }
        }
        return new ShapeModeManager(id, label, icon, ShapeType.PLANE_2D,
                                    offsets.toArray(new int[0][]));
    }

    /**
     * Tạo chế độ đào từ mảng 3D.
     * shape[depth][row][col] — khối trung tâm = (depth/2, rows/2, cols/2).
     */
    public static ShapeModeManager from3D(String id, String label, String icon, int[][][] shape) {
        int centerDepth = shape.length / 2;
        int centerRow   = shape[0].length / 2;
        int centerCol   = shape[0][0].length / 2;
        List<int[]> offsets = new ArrayList<>();
        for (int d = 0; d < shape.length; d++) {
            for (int r = 0; r < shape[d].length; r++) {
                for (int c = 0; c < shape[d][r].length; c++) {
                    if (shape[d][r][c] == 0) continue;
                    int f = d - centerDepth; // trục chiều sâu (forward)
                    int s = c - centerCol;   // trục ngang (right)
                    int u = centerRow - r;   // trục dọc  (up)
                    if (f == 0 && s == 0 && u == 0) continue; // bỏ origin
                    offsets.add(new int[]{f, s, u});
                }
            }
        }
        return new ShapeModeManager(id, label, icon, ShapeType.BOX_3D,
                                    offsets.toArray(new int[0][]));
    }

    // ── MiningStrategy ────────────────────────────────────────────────────────

    @Override public String getId()    { return id; }
    @Override public String getLabel() { return label; }
    @Override public String getIcon()  { return icon; }

    @Override
    public List<BlockPos> collectBlocks(World world, BlockPos origin, BlockState target,
                                        int maxBlocks, OrientationContext ctx) {
        List<BlockPos> result = new ArrayList<>();
        for (int[] off : relOffsets) {
            if (result.size() >= maxBlocks) break;
            BlockPos pos = (type == ShapeType.PLANE_2D)
                    ? ctx.planeOffset(origin, off[0], off[1])
                    : ctx.offset(origin, off[0], off[1], off[2]);

            // SỬA TẠI ĐÂY: Thay !isAir() bằng kiểm tra trùng loại block đang đào
            if (world.getBlockState(pos).getBlock() == target.getBlock()) {
                result.add(pos);
            }
        }
        return result;
    }
}
