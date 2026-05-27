package com.tcveinminer.engine.strategy;

import com.tcveinminer.engine.traversal.OrientationContext;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public final class ShapeModeManager extends BaseBfsStrategy {

    private enum ShapeType { PLANE_2D, BOX_3D }

    private final String    id, label, icon;
    private final ShapeType type;
    private final int[][]   relOffsets;

    private ShapeModeManager(String id, String label, String icon, ShapeType type, int[][] relOffsets) {
        this.id = id;
        this.label = label;
        this.icon = icon;
        this.type = type;
        this.relOffsets = relOffsets;
    }

    public static ShapeModeManager from2D(String id, String label, String icon, int[][] shape) {
        int centerRow = shape.length / 2;
        int centerCol = shape[0].length / 2;
        List<int[]> offsets = new ArrayList<>();
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] == 0) continue;
                int s = c - centerCol;
                int u = centerRow - r;
                if (s == 0 && u == 0) continue;
                offsets.add(new int[]{s, u});
            }
        }
        return new ShapeModeManager(id, label, icon, ShapeType.PLANE_2D, offsets.toArray(new int[0][]));
    }

    public static ShapeModeManager from3D(String id, String label, String icon, int[][][] shape) {
        int centerDepth = shape.length / 2;
        int centerRow   = shape[0].length / 2;
        int centerCol   = shape[0][0].length / 2;
        List<int[]> offsets = new ArrayList<>();
        for (int d = 0; d < shape.length; d++) {
            for (int r = 0; r < shape[d].length; r++) {
                for (int c = 0; c < shape[d][r].length; c++) {
                    if (shape[d][r][c] == 0) continue;
                    int f = d - centerDepth;
                    int s = c - centerCol;
                    int u = centerRow - r;
                    if (f == 0 && s == 0 && u == 0) continue;
                    offsets.add(new int[]{f, s, u});
                }
            }
        }
        return new ShapeModeManager(id, label, icon, ShapeType.BOX_3D, offsets.toArray(new int[0][]));
    }

    @Override public String getId()    { return id; }
    @Override public String getLabel() { return label; }
    @Override public String getIcon()  { return icon; }
    @Override public FilterModeManager.MiningMode getModeType() { return FilterModeManager.MiningMode.SHAPE; }

    @Override
    protected boolean isWithinShape(BlockPos pos, BlockPos origin, OrientationContext ctx, int f, int s, int u) {
        for (int[] off : relOffsets) {
            BlockPos p = (type == ShapeType.PLANE_2D)
                    ? ctx.planeOffset(origin, off[0], off[1])
                    : ctx.offset(origin, off[0], off[1], off[2]);
            if (p.equals(pos)) return true;
        }
        return false;
    }

    @Override
    protected boolean shouldQueue(SearchNode node, BlockState state, MiningRequest req, int maxSolidF, int maxSolidDepth, boolean passedFilter) {
        return true; // Biên giới của hình dáng chính là giới hạn lan truyền
    }
}