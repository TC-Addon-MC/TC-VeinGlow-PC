package com.tcveinminer.engine.strategy;

import com.tcveinminer.engine.traversal.OrientationContext;
import com.tcveinminer.util.ExpressionEvaluator;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public final class CustomEquationStrategy implements MiningStrategy {
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
    public List<BlockPos> collectBlocks(MiningRequest req) {
        List<BlockPos> result = new ArrayList<>();
        BlockPos origin = req.origin();
        OrientationContext ctx = req.orientCtx();
        Direction forwardDir = (ctx.hitFace == Direction.UP || ctx.hitFace == Direction.DOWN)
                ? ctx.playerFacing
                : ctx.hitFace.getOpposite();

        for (int f = -SCAN_RANGE; f <= SCAN_RANGE; f++) {
            for (int s = -SCAN_RANGE; s <= SCAN_RANGE; s++) {
                for (int u = -SCAN_RANGE; u <= SCAN_RANGE; u++) {
                    if (result.size() >= req.maxBlocks()) return result;
                    if (f == 0 && s == 0 && u == 0) continue;
                    
                    // x=side, y=up, z=forward
                    if (!evaluator.eval(s, u, f)) continue;

                    BlockPos pos = ctx.offset(origin, f, s, u);
                    BlockState currentState = req.world().getBlockState(pos);
                    int distance = (int)Math.sqrt(pos.getSquaredDistance(origin));

                    FilterModeManager.FilterContext fCtx = new FilterModeManager.FilterContext(
                            req.world(), req.player(), req.tool(), origin, pos,
                            req.targetState(), currentState, forwardDir,
                            Math.abs(f), distance, result.size(), getModeType(), req.cache(), req.blacklist(), req.requireCorrectTool()
                    );

                    if (req.filter().test(fCtx)) {
                        result.add(pos);
                    }
                }
            }
        }

        return result;
    }
}
