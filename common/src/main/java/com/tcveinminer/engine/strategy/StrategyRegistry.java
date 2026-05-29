package com.tcveinminer.engine.strategy;

import java.util.*;

/**
 * Registry tập trung tất cả MiningStrategy.
 * Mỗi mode sử dụng manager tương ứng — dễ thêm/bớt/tuỳ chỉnh.
 */
public final class StrategyRegistry {

    private static final Map<String, MiningStrategy> REGISTRY = new LinkedHashMap<>();

    // ── Shape templates (1 = đào, 0 = bỏ qua) ───────────────────────────────

    /** 3×3 đầy trong mặt phẳng hitFace. */
    private static final int[][] SHAPE_3x3 = {
        {1, 1, 1},
        {1, 1, 1},
        {1, 1, 1}
    };

    /** 5×5 đầy trong mặt phẳng hitFace. */
    private static final int[][] SHAPE_5x5 = {
        {1, 1, 1, 1, 1},
        {1, 1, 1, 1, 1},
        {1, 1, 1, 1, 1},
        {1, 1, 1, 1, 1},
        {1, 1, 1, 1, 1}
    };

    static {
        // ── Vein spread modes ─────────────────────────────────────────────────
        register(new SpreadModeManager(SpreadModeManager.Mode.FACE,
                "FACE",      "Standard (Face)",        ""));
        register(new SpreadModeManager(SpreadModeManager.Mode.EDGES,
                "EDGES",     "Standard V2 (Edges)",    ""));
        register(new SpreadModeManager(SpreadModeManager.Mode.CORNERS,
                "CORNERS",   "Standard V3 (Corners)",  ""));
        register(new SpreadModeManager(SpreadModeManager.Mode.TREE_CAP,
                "TREE_CAP",  "TreeCapitator",           ""));

        // ── Tunnel modes ──────────────────────────────────────────────────────
        // TUNNEL_1x2: 1 rộng × 2 cao (chân + đầu), kéo dài vô tận
        register(new TunnelModeManager("TUNNEL_1x2",  "Tunnel 1×2",  "", 0, 0, 0, 1));
        // TUNNEL_3x3: 3×3, kéo dài vô tận
        register(new TunnelModeManager("TUNNEL_3x3",  "Tunnel 3×3",  "", -1, 1, -1, 1));

        // ── Stair modes ───────────────────────────────────────────────────────
        register(new StairModeManager(+1)); // STAIR_UP
        register(new StairModeManager(-1)); // STAIR_DOWN

        // ── Shape modes ───────────────────────────────────────────────────────
        register(ShapeModeManager.from2D("AREA_3x3", "3×3 Area", "", SHAPE_3x3));
        register(ShapeModeManager.from2D("AREA_5x5", "5×5 Area", "", SHAPE_5x5));
    }

    public static void register(MiningStrategy s) {
        REGISTRY.put(s.getId(), s);
    }

    public static MiningStrategy get(String id) {
        return REGISTRY.getOrDefault(id, REGISTRY.get("FACE"));
    }

    public static Collection<MiningStrategy> all() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    public static boolean contains(String id) {
        return REGISTRY.containsKey(id);
    }

    private StrategyRegistry() {}
}
