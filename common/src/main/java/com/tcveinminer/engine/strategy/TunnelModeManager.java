package com.tcveinminer.engine.strategy;

import com.tcveinminer.engine.traversal.OrientationContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

public final class TunnelModeManager extends BaseBfsStrategy {



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
    protected boolean isWithinShape(BlockPos pos, BlockPos origin, OrientationContext ctx, int f, int s, int u) {
        if (f < 0) return false;
        if (s < sMin || s > sMax) return false;
        if (u < uMin || u > uMax) return false;
        return true;
    }

}
