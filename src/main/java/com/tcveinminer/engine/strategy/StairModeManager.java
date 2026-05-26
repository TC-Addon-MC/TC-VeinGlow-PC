package com.tcveinminer.engine.strategy;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;

public final class StairModeManager implements MiningStrategy {

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
    public List<BlockPos> collectBlocks(MiningRequest req) {
        List<BlockPos>  result  = new ArrayList<>();
        Set<BlockPos>   visited = new HashSet<>();

        Direction forwardDir = (req.orientCtx().hitFace == Direction.UP || req.orientCtx().hitFace == Direction.DOWN)
                ? req.orientCtx().playerFacing
                : req.orientCtx().hitFace.getOpposite();

        int maxSteps = Math.max(req.maxBlocks() * 4, req.maxBlocks());
        for (int stepIndex = 1; result.size() < req.maxBlocks() && stepIndex <= maxSteps; stepIndex++) {
            BlockPos step = req.origin().add(
                    req.orientCtx().forward.getX() * stepIndex + req.orientCtx().up.getX() * dy * stepIndex,
                    req.orientCtx().forward.getY() * stepIndex + req.orientCtx().up.getY() * dy * stepIndex,
                    req.orientCtx().forward.getZ() * stepIndex + req.orientCtx().up.getZ() * dy * stepIndex
            );

            // Thử đào cả block chân (step) và block đầu (step.up()) để người đi vừa
            BlockPos[] positions = {step, step.up()};
            for (BlockPos p : positions) {
                if (result.size() >= req.maxBlocks()) break;
                if (!visited.add(p)) continue;

                BlockState state = req.world().getBlockState(p);
                int dist = (int) Math.sqrt(p.getSquaredDistance(req.origin()));

                FilterModeManager.FilterContext ctx = new FilterModeManager.FilterContext(
                        req.world(), req.player(), req.tool(), req.origin(), p,
                        req.targetState(), state, forwardDir,
                        stepIndex, dist, result.size(), getModeType(), req.cache(), req.blacklist(), req.requireCorrectTool()
                );

                if (req.filter().test(ctx)) {
                    result.add(p);
                }
            }
        }

        result.sort(dy > 0 ? Comparator.comparingInt(BlockPos::getY) : Comparator.comparingInt((BlockPos p) -> p.getY()).reversed());
        return result;
    }
}
