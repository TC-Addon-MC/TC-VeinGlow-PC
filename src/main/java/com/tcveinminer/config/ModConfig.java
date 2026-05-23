package com.tcveinminer.config;

import net.minecraft.util.Identifier;

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
        FACE        ("FACE",        "Standard (Face)",        "⬛", "Đào các khối kề mặt", 4),
        EDGES       ("EDGES",       "Standard V2 (Edges)",    "🔷", "Đào các khối kề cạnh", 8),
        CORNERS     ("CORNERS",     "Standard V3 (Corners)",  "💎", "Đào các khối kề góc", 10),
        TALL_1x2    ("TALL_1x2",    "1×2 (Tall)",             "🧱", "Đào hầm cao 2 block", 5),
        TUNNEL_1x2  ("TUNNEL_1x2",  "Tunnel 1×2",             "🚇", "Đào hầm 1x2", 4),
        TUNNEL_3x3  ("TUNNEL_3x3",  "Tunnel 3×3",             "🚇", "Đào hầm 3x3", 8),
        STAIR_UP    ("STAIR_UP",    "Stair Up",               "⬆",  "Đào cầu thang lên", 4),
        STAIR_DOWN  ("STAIR_DOWN",  "Stair Down",             "⬇",  "Đào cầu thang xuống", 4),
        AREA_3x3    ("AREA_3x3",    "3×3 Area",               "🟦", "Đào khu vực 3x3", 8),
        AREA_5x5    ("AREA_5x5",    "5×5 Area",               "🔵", "Đào khu vực 5x5", 11),
        TREE_CAP    ("TREE_CAP",    "TreeCapitator",          "🌳", "Chặt toàn bộ cây", 5);

        public final String strategyId;
        public final String label;
        public final String icon;
        public final String desc;
        public final int blockCount;

        MiningShape(String strategyId, String label, String icon, String desc, int blockCount) {
            this.strategyId = strategyId;
            this.label      = label;
            this.icon       = icon;
            this.desc       = desc;
            this.blockCount = blockCount;
        }
    }
    public Set<Identifier> blacklist = new LinkedHashSet<>();

    // Cài đặt chung
    public boolean enabled            = true;
    public int     maxBlocks          = 64;
    public boolean requireCorrectTool = true;
    public boolean consumeDurability  = true;
    public int     cooldownTicks      = 0;
    public boolean showHud            = true;
    public boolean requireSneak       = false;
    public int     tickSliceSize      = 4;

    // Chế độ kích hoạt: 1=giữ, 2=giữ+sneak, 3=toggle, 4=toggle+sneak
    public int  activationMode  = 1;
    // Hiển thị outline khối quét
    public boolean showOutline  = true;
    // Màu outline: hex string hoặc "RAINBOW" hoặc "DISABLED"
    public String outlineColor  = "#D8A15B";

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
