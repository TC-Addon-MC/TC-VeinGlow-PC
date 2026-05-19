package com.tcveinminer.config;

import com.tcveinminer.engine.strategy.*;

import java.util.*;

public class ModConfig {

    /**
     * MiningShape enum — ánh xạ đến strategy ID trong StrategyRegistry.
     * strategyId phải khớp chính xác với Strategy.getId().
     */
    public enum MiningShape {
        FACE        (FaceStrategy.ID,             "Standard (Face)",        "⬛"),
        EDGES       (EdgeStrategy.ID,             "Standard V2 (Edges)",    "🔷"),
        CORNERS     (CornerStrategy.ID,           "Standard V3 (Corners)",  "💎"),
        TALL_1x2    (TallStrategy.ID,             "1×2 (Tall)",             "🧱"),
        TUNNEL_1x2  (TunnelStrategy.ID,           "Tunnel 1×2",             "🚇"),
        STAIR_UP    (StairStrategy.ID_UP,         "Stair Up",               "⬆"),
        STAIR_DOWN  (StairStrategy.ID_DOWN,       "Stair Down",             "⬇"),
        AREA_3x3    (AreaStrategy.ID,             "3×3 Area",               "🟦"),
        AREA_5x5    (Area5x5Strategy.ID,          "5×5 Area",               "🔵"),
        TREE_CAP    (TreeCapitatorStrategy.ID,    "TreeCapitator",          "🌳");

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

    // Shapes bật trong radial menu (default: 3 vein modes + tunnel + tree)
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
}
