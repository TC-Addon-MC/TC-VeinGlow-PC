package com.tcveinminer.engine.strategy;

import com.tcveinminer.engine.traversal.OrientationContext;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public final class TunnelModeManager implements MiningStrategy {

    private static final int AIR_SLICE_STOP = 3;

    public static final String ID_1x2  = "TUNNEL_1x2";
    public static final String ID_3x3  = "TUNNEL_3x3";

    private final String id;
    private final String label;
    private final String icon;
    private final int sMin, sMax;
    private final int uMin, uMax;

    public TunnelModeManager(String id, String label, String icon, int sMin, int sMax, int uMin, int uMax) {
        this.id    = id;
        this.label = label;
        this.icon  = icon;
        this.sMin  = sMin;
        this.sMax  = sMax;
        this.uMin  = uMin;
        this.uMax  = uMax;
    }

    @Override public String getId()    { return id; }
    @Override public String getLabel() { return label; }
    @Override public String getIcon()  { return icon; }
    @Override public FilterModeManager.MiningMode getModeType() { return FilterModeManager.MiningMode.TUNNEL; }

    @Override
    public List<BlockPos> collectBlocks(MiningRequest req) {
        List<BlockPos> result = new ArrayList<>();
        int consecutiveAirSlices = 0;
        int depth = -1;
        OrientationContext ctx = req.orientCtx();

        // ÉP KIỂU DIRECTION
        Direction forwardDir = (ctx.hitFace == Direction.UP || ctx.hitFace == Direction.DOWN)
                ? ctx.playerFacing
                : ctx.hitFace.getOpposite();

        while (result.size() < req.maxBlocks()) {
            depth++;
            List<BlockPos> sliceBlocks = new ArrayList<>();
            boolean hasSolidInSlice = false;

            for (int s = sMin; s <= sMax; s++) {
                for (int u = uMin; u <= uMax; u++) {
                    if (depth == 0 && s == 0 && u == 0) continue;
                    BlockPos pos = ctx.offset(req.origin(), depth, s, u);
                    BlockState currentState = req.world().getBlockState(pos);

                    if (!currentState.isAir()) {
                        hasSolidInSlice = true;
                    }

                    int distance = (int) Math.sqrt(pos.getSquaredDistance(req.origin()));

                    // SỬA TẠI ĐÂY: forwardDir
                    FilterModeManager.FilterContext fCtx = new FilterModeManager.FilterContext(
                            req.world(), req.player(), req.tool(), req.origin(), pos,
                            req.targetState(), currentState, forwardDir,
                            depth, distance, result.size(), getModeType(), req.cache()
                    );

                    if (req.filter().test(fCtx)) {
                        sliceBlocks.add(pos);
                    }
                }
            }

            if (!hasSolidInSlice) {
                consecutiveAirSlices++;
                if (consecutiveAirSlices >= AIR_SLICE_STOP) break;
            } else {
                consecutiveAirSlices = 0;
                for (BlockPos pos : sliceBlocks) {
                    if (result.size() >= req.maxBlocks()) break;
                    result.add(pos);
                }
            }
        }
        return result;
    }
}