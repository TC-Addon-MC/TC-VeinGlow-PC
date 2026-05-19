package com.tcveinminer.engine.strategy;

import com.tcveinminer.engine.traversal.OrientationContext;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý các chế độ Tunnel.
 *
 * Cách dùng: truyền vào tiết diện (sMin..sMax) × (uMin..uMax).
 * Chiều sâu (forward) sẽ tự động kéo dài cho đến khi:
 *   1. Đạt maxBlocks, HOẶC
 *   2. Gặp 3 tầng liên tiếp toàn là air (hang động / khoảng trống).
 *
 * Ví dụ:
 *   TUNNEL_1x2  : sMin=0, sMax=0, uMin=0, uMax=1  → 1 rộng × 2 cao (chân + đầu)
 *   TUNNEL_1x1  : sMin=0, sMax=0, uMin=0, uMax=0  → 1×1
 *   TUNNEL_3x3  : sMin=-1, sMax=1, uMin=-1, uMax=1 → 3×3 đối xứng
 */
public final class TunnelModeManager implements MiningStrategy {

    /** Số tầng air liên tiếp trước khi dừng. */
    private static final int AIR_SLICE_STOP = 3;

    public static final String ID_1x2  = "TUNNEL_1x2";
    public static final String ID_3x3  = "TUNNEL_3x3";

    private final String id;
    private final String label;
    private final String icon;
    private final int sMin, sMax; // tiết diện: trục ngang (right)
    private final int uMin, uMax; // tiết diện: trục dọc (up)

    public TunnelModeManager(String id, String label, String icon,
                              int sMin, int sMax, int uMin, int uMax) {
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

    @Override
    public List<BlockPos> collectBlocks(World world, BlockPos origin, BlockState target,
                                        int maxBlocks, OrientationContext ctx) {
        List<BlockPos> result    = new ArrayList<>();
        int consecutiveAirSlices = 0;
        int depth                = 0;

        while (result.size() < maxBlocks) {
            depth++;

            // Thu thập tất cả block trong tầng này (tiết diện tại chiều sâu `depth`)
            List<BlockPos> sliceBlocks = new ArrayList<>();
            for (int s = sMin; s <= sMax; s++) {
                for (int u = uMin; u <= uMax; u++) {
                    BlockPos pos = ctx.offset(origin, depth, s, u);
                    if (!world.getBlockState(pos).isAir()) {
                        sliceBlocks.add(pos);
                    }
                }
            }

            if (sliceBlocks.isEmpty()) {
                // Tầng toàn air
                consecutiveAirSlices++;
                if (consecutiveAirSlices >= AIR_SLICE_STOP) break; // dừng sau 3 tầng air
            } else {
                // Tầng có block thực → reset đếm air
                consecutiveAirSlices = 0;
                for (BlockPos pos : sliceBlocks) {
                    if (result.size() >= maxBlocks) break;
                    result.add(pos);
                }
            }
        }

        return result;
    }
}
