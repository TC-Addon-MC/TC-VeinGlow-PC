package com.tcveinminer.engine.strategy;

import com.tcveinminer.engine.traversal.OrientationContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

public final class StairModeManager extends BaseBfsStrategy {

    public static final String ID_UP   = "STAIR_UP";
    public static final String ID_DOWN = "STAIR_DOWN";

    private final int    dy;
    private final String id;

    public StairModeManager(int dy) {
        this.dy = dy;
        this.id = dy > 0 ? ID_UP : ID_DOWN;
    }

    @Override public String getId()    { return id; }
    @Override public String getLabel() { return dy > 0 ? "Stair Up" : "Stair Down"; }
    @Override public String getIcon()  { return ""; }
    @Override public FilterModeManager.MiningMode getModeType() { return FilterModeManager.MiningMode.TUNNEL; }

    @Override
    protected boolean isWithinShape(BlockPos pos, BlockPos origin, OrientationContext ctx, int f, int s, int u) {
        if (s != 0) return false;
        if (f < 0) return false;
        int expectedU = f * dy;
        // Cho phép đào block bước chân (expectedU) và block trên đầu (expectedU + 1) để có thể đi lọt
        return u == expectedU || u == expectedU + 1;
    }

    @Override
    protected boolean shouldQueue(SearchNode node, BlockState state, MiningRequest req, int maxSolidF, int maxSolidDepth, boolean passedFilter) {
        // Dừng lan truyền về phía trước nếu đi qua 3 bước không có block rắn
        return node.f() <= maxSolidF + 3;
    }
}
