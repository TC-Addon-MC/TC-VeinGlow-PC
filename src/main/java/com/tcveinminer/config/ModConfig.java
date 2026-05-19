package com.tcveinminer.config;

import java.util.*;

public class ModConfig {

    /**
     * MiningShape enum — ánh xạ đến strategy ID trong StrategyRegistry.
     * strategyId phải khớp chính xác với MiningStrategy.getId().
     *
     * [ĐÃ CẬP NHẬT]: dùng string literal thay vì import class cũ.
     * Thêm mode mới → chỉ cần thêm một dòng ở đây + đăng ký trong StrategyRegistry.
     */
    public enum MiningShape {
        FACE        ("FACE",        "Standard (Face)",        "⬛"),
        EDGES       ("EDGES",       "Standard V2 (Edges)",    "🔷"),
        CORNERS     ("CORNERS",     "Standard V3 (Corners)",  "💎"),
        TALL_1x2    ("TALL_1x2",    "1×2 (Tall)",             "🧱"),
        TUNNEL_1x2  ("TUNNEL_1x2",  "Tunnel 1×2",             "🚇"),
        TUNNEL_3x3  ("TUNNEL_3x3",  "Tunnel 3×3",             "🚇"),
        STAIR_UP    ("STAIR_UP",    "Stair Up",               "⬆"),
        STAIR_DOWN  ("STAIR_DOWN",  "Stair Down",             "⬇"),
        AREA_3x3    ("AREA_3x3",    "3×3 Area",               "🟦"),
        AREA_5x5    ("AREA_5x5",    "5×5 Area",               "🔵"),
        TREE_CAP    ("TREE_CAP",    "TreeCapitator",           "🌳");

        public final String strategyId;
        public final String label;
        public final String icon;

        MiningShape(String strategyId, String label, String icon) {
            this.strategyId = strategyId;
            this.label      = label;
            this.icon       = icon;
        }
    }

    // Cài đặt chung
    public boolean enabled            = true;
    public int     maxBlocks          = 64;
    public boolean requireCorrectTool = true;
    public boolean consumeDurability  = true;
    public int     cooldownTicks      = 0;
    public boolean showHud            = true;
    public boolean requireSneak       = false;
    public boolean diagonalMining     = false;
    public int     tickSliceSize      = 4;

    // Shape hiện tại
    public MiningShape miningShape = MiningShape.FACE;

    // Shapes bật trong radial menu
    public Set<String> enabledShapes = new LinkedHashSet<>(
        List.of("FACE", "EDGES", "CORNERS", "TUNNEL_1x2", "AREA_3x3", "TREE_CAP")
    );

    // Block và tool config
    public Set<String>          blacklistedBlocks = new HashSet<>();
    public Map<String, Boolean> enabledTools      = new LinkedHashMap<>(Map.of(
            "pickaxe", true,
            "axe",     true,
            "shovel",  false,
            "sword",   false,
            "hand",    false,
            "hoe",     false
    ));

    // Convenience: lấy strategyId từ miningShape hiện tại
    public String getStrategyId() {
        return miningShape.strategyId;
    }
}
