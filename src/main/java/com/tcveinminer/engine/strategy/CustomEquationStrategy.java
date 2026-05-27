package com.tcveinminer.engine.strategy;

import com.tcveinminer.engine.traversal.OrientationContext;
import com.tcveinminer.util.ExpressionEvaluator;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public final class CustomEquationStrategy extends BaseBfsStrategy {
    private static final int SCAN_RANGE = 8;

    private final String id;
    private final ExpressionEvaluator evaluator;

    public CustomEquationStrategy(String id, ExpressionEvaluator evaluator) {
        this.id = id;
        this.evaluator = evaluator;
    }

    @Override public String getId() { return id; }
    @Override public String getLabel() { return id; }
    @Override public String getIcon() { return ""; }
    @Override public FilterModeManager.MiningMode getModeType() { return FilterModeManager.MiningMode.SHAPE; }

    @Override
    protected boolean isWithinShape(BlockPos pos, BlockPos origin, OrientationContext ctx, int f, int s, int u) {
        if (Math.abs(f) > SCAN_RANGE || Math.abs(s) > SCAN_RANGE || Math.abs(u) > SCAN_RANGE) {
            return false;
        }
        // Phương trình sử dụng (s, u, f) tương ứng (x, y, z)
        return evaluator.eval(s, u, f);
    }

    @Override
    protected boolean shouldQueue(SearchNode node, BlockState state, MiningRequest req, int maxSolidF, int maxSolidDepth, boolean passedFilter) {
        return true;
    }
}
