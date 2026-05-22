package com.tcveinminer.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClientConfig {

    /** Một chế độ đào tùy chỉnh do người chơi tạo. */
    public static class CustomShapeEntry {
        public String name = "";
        public String equation = "";
        /** strategyId = "custom:" + slugified name, dùng để tra StrategyRegistry */
        public String strategyId = "";

        public CustomShapeEntry() {}
        public CustomShapeEntry(String name, String equation) {
            this.name = name;
            this.equation = equation;
            this.strategyId = "custom:" + name.toLowerCase().replaceAll("[^a-z0-9_]", "_");
        }
    }

    /** Danh sách chế độ tùy chỉnh — lưu vào JSON. */
    public List<CustomShapeEntry> customShapes = new ArrayList<>();


    // ==========================================
    // NHÓM HIỂN THỊ (Visual) - LƯU VÀO JSON
    // ==========================================
    public boolean showOutline = true;

    // Màu outline: lưu trực tiếp R/G/B thay vì hex để đồng bộ với GUI
    public int colorR = 255;
    public int colorG = 140;
    public int colorB = 0;
    public boolean colorRainbow = false;
    public boolean colorDisabled = false;

    public float outlineAlpha = 0.8f;
    public float outlineThickness = 3.0f;

    public boolean showHud = true;         // Đổi từ enableHud → showHud cho khớp MenuState
    public int hudPositionX = 10;
    public int hudPositionY = 10;


    // ==========================================
    // NHÓM ĐIỀU KHIỂN & CHẾ ĐỘ ĐÀO - LƯU VÀO JSON
    // ==========================================

    // Lưu dạng int cho khớp với MenuState (1=HOLD_KEY, 2=HOLD_SNEAK, 3=TOGGLE, 4=TOGGLE_SNEAK)
    public int activationMode = 1;

    public String currentShape = "shapeless";

    public String customShapeEquation = "x^2 + y^2 + z^2 <= 16";

    // Danh sách chế độ đào đang bật (khớp với MenuState.enabledShapes)
    public Set<String> enabledShapes = new LinkedHashSet<>(List.of(
            "FACE", "EDGES", "CORNERS", "TUNNEL_1x2", "AREA_3x3", "TREE_CAP"
    ));


    // ==========================================
    // NHÓM ƯU TIÊN CÁ NHÂN - LƯU VÀO JSON
    // ==========================================
    public int clientMaxBlocks = 64;

    public List<String> personalBlacklist = new ArrayList<>();

    public boolean requireCorrectTool = true;

    // Công cụ được phép kích hoạt mod (khớp với MenuState.enabledTools)
    public Map<String, Boolean> enabledTools = new LinkedHashMap<>() {{
        put("all", true);
        put("hand", false);
        put("item", false);
        put("pickaxe", false);
        put("axe", false);
        put("shovel", false);
        put("sword", false);
        put("hoe", false);
    }};


    // ==========================================
    // DỮ LIỆU ĐỒNG BỘ TỪ SERVER (KHÔNG LƯU VÀO JSON)
    // ==========================================
    public transient int serverMaxBlocks = 64;
    public transient List<String> serverDisabledShapes = new ArrayList<>();
    public transient List<String> serverBlacklist = new ArrayList<>();


    // ==========================================
    // CÁC HÀM TIỆN ÍCH
    // ==========================================

    /** Giới hạn block thực tế: lấy min(client, server) để chống hack */
    public int getEffectiveMaxBlocks() {
        return Math.min(this.clientMaxBlocks, this.serverMaxBlocks);
    }

    /** Kiểm tra chế độ đào hiện tại có bị server cấm không */
    public boolean isCurrentShapeAllowed() {
        return !serverDisabledShapes.contains(currentShape);
    }

    /** Kiểm tra block có được phép đào không (so cả server + cá nhân) */
    public boolean isBlockAllowed(String blockId) {
        if (serverBlacklist.contains(blockId)) return false;
        if (personalBlacklist.contains(blockId)) return false;
        return true;
    }
}