package com.tcveinminer.config;

import com.tcveinminer.engine.strategy.*;

import java.util.*;

public class ModConfig {

    /**
     * MiningShape enum — ánh xạ đến strategy ID trong StrategyRegistry.
     * Giữ lại enum để tương thích config JSON + radial menu UI.
     * Logic thật sự nằm trong Strategy classes — không phải ở đây.
     */
    public enum MiningShape {
        FACE        (FaceStrategy.ID,    "Standard (Face)",        "⬛"),
        EDGES       (EdgeStrategy.ID,    "Standard V2 (Edges)",    "🔷"),
        CORNERS     (CornerStrategy.ID,  "Standard V3 (Corners)",  "💎"),
        TALL_1x2    (TallStrategy.ID,    "1×2 (Tall)",             "🧱"),
        STAIR_UP    (StairStrategy.ID_UP,   "Stair Up",            "⬆"),
        STAIR_DOWN  (StairStrategy.ID_DOWN, "Stair Down",          "⬇"),
        AREA_3x3    (AreaStrategy.ID,    "3×3 Area",               "🟦");

        public final String strategyId; // key vào StrategyRegistry
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

    /** Số block tối đa execute mỗi server tick (tick slicing). */
    public int tickSliceSize = 4;

    // Shape hiện tại
    public MiningShape miningShape = MiningShape.FACE;

    // Shapes bật trong radial menu
    public Set<String> enabledShapes = new LinkedHashSet<>(List.of("FACE", "EDGES", "CORNERS"));

    // Block và tool config
    public Set<String>         blacklistedBlocks = new HashSet<>();
    public Map<String, Boolean> enabledTools = new LinkedHashMap<>(Map.of(
            "pickaxe", true,
            "axe",     false,
            "shovel",  false,
            "sword",   false,
            "hand",    false,
            "hoe",     false
    ));
}
