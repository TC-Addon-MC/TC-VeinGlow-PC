package com.tcveinminer.client.config;

import com.tcveinminer.config.ModConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ClientConfig {

    /** Một chế độ đào tùy chỉnh do người chơi tạo. */
    public static class CustomShapeEntry {
        public String name = "";
        public String equation = "";
        /** strategyId = "custom:" + slugified name, dùng để tra StrategyRegistry */
        public String strategyId = "";

        public CustomShapeEntry() {
        }

        public CustomShapeEntry(String name, String equation) {
            this.name = name;
            this.equation = equation;
            this.strategyId = customShapeId(name);
        }
    }

    public static String customShapeId(String name) {
        if (name == null || name.isBlank())
            return "custom:shape";

        String slug = name.toLowerCase()
                .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
                .replaceAll("[èéẹẻẽêềếệểễ]", "e")
                .replaceAll("[ìíịỉĩ]", "i")
                .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
                .replaceAll("[ùúụủũưừứựửữ]", "u")
                .replaceAll("[ỳýỵỷỹ]", "y")
                .replaceAll("đ", "d")
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");

        return "custom:" + (slug.isBlank() ? "shape" : slug);
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

    public int outlineAlpha = 204; // 0-255, mặc định ~80%
    public float outlineThickness = 3.0f;

    /**
     * Danh sách màu outline (hex string "RRGGBB").
     * Nếu rỗng: dùng colorR/G/B.
     * Nếu ≥2: nội suy gradient qua từng màu.
     */
    public List<String> colorList = new ArrayList<>();

    /** Cho phép các màu chạy động theo viền. */
    public boolean enableFlowAnimation = true;

    public float segmentLength = 2.0f;
    public float flowSmoothness = 0.5f;

    /** Thời gian chuyển tiếp giữa các màu (giây). */
    public float colorTransitionTime = 1.0f;

    public boolean showHud = false; // Đổi từ enableHud → showHud cho khớp MenuState
    public int hudPositionX = 10;
    public int hudPositionY = 10;

    // ==========================================
    // NHÓM ĐIỀU KHIỂN & CHẾ ĐỘ ĐÀO - LƯU VÀO JSON
    // ==========================================

    // Lưu dạng int cho khớp với MenuState (1=HOLD_KEY, 2=HOLD_SNEAK, 3=TOGGLE,
    // 4=TOGGLE_SNEAK)
    public int activationMode = 1;

    public String currentShape = "FACE";

    public String customShapeEquation = "x^2 + y^2 + z^2 <= 16";

    // Danh sách chế độ đào đang bật (khớp với MenuState.enabledShapes)
    public Set<String> enabledShapes = new LinkedHashSet<>(List.of(
            "FACE", "EDGES", "CORNERS", "TUNNEL_1x2", "AREA_3x3", "TREE_CAP"));

    // ==========================================
    // NHÓM ƯU TIÊN CÁ NHÂN - LƯU VÀO JSON
    // ==========================================
    public int clientMaxBlocks = 64;

    public List<String> personalBlacklist = new ArrayList<>();

    public boolean requireCorrectTool = false;

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
        if (serverBlacklist.contains(blockId))
            return false;
        if (personalBlacklist.contains(blockId))
            return false;
        return true;
    }

    public void postLoad() {
        if (customShapes == null)
            customShapes = new ArrayList<>();
            
        boolean hasHeart = false;
        for (CustomShapeEntry entry : customShapes) {
            if ("custom:heart".equals(entry.strategyId) || "Heart".equals(entry.name)) {
                hasHeart = true;
                break;
            }
        }
        if (!hasHeart) {
            customShapes.add(new CustomShapeEntry("Heart", "(x^2 + 2.25*y^2 + z^2 - 1)^3 - x^2*z^3 - 0.1125*y^2*z^3 <= 0"));
            if (enabledShapes != null) {
                enabledShapes.add("custom:heart");
            }
        }
        if (enabledShapes == null)
            enabledShapes = new LinkedHashSet<>(List.of("FACE", "custom:heart"));
        if (currentShape == null || currentShape.isBlank())
            currentShape = "FACE";
        if (personalBlacklist == null)
            personalBlacklist = new ArrayList<>();
        if (serverDisabledShapes == null)
            serverDisabledShapes = new ArrayList<>();
        if (serverBlacklist == null)
            serverBlacklist = new ArrayList<>();
        if (serverMaxBlocks <= 0)
            serverMaxBlocks = 64;

        normalizeShapeConfig();
    }

    private void normalizeShapeConfig() {
        Set<String> builtinIds = Arrays.stream(ModConfig.MiningShape.values())
                .map(Enum::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> customIds = customShapes.stream()
                .map(entry -> entry.strategyId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        currentShape = normalizeShapeId(currentShape);
        enabledShapes = enabledShapes.stream()
                .map(ClientConfig::normalizeShapeId)
                .filter(id -> builtinIds.contains(id) || customIds.contains(id))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (enabledShapes.isEmpty()) {
            enabledShapes.add("FACE");
        }

        if (!builtinIds.contains(currentShape) && !customIds.contains(currentShape)) {
            System.out.println("[TCVeinMiner] Invalid currentShape in client config: " + currentShape + " -> FACE");
            currentShape = "FACE";
        }

        if (!enabledShapes.contains(currentShape)) {
            enabledShapes.add(currentShape);
        }
    }

    private static String normalizeShapeId(String id) {
        if (id == null)
            return "FACE";
        String trimmed = id.trim();
        if (trimmed.startsWith("custom:"))
            return trimmed;
        return switch (trimmed.toUpperCase()) {
            case "STAIRUP", "STAIR_UPWARD", "STAIRS_UP" -> "STAIR_UP";
            case "STAIRDOWN", "STAIR_DOWNWARD", "STAIRS_DOWN" -> "STAIR_DOWN";
            case "TREECAP", "TREE_CAPITATOR" -> "TREE_CAP";
            case "AREA3X3", "AREA_3X3" -> "AREA_3x3";
            case "AREA5X5", "AREA_5X5" -> "AREA_5x5";
            case "TUNNEL1X2", "TUNNEL_1X2" -> "TUNNEL_1x2";
            case "TUNNEL3X3", "TUNNEL_3X3" -> "TUNNEL_3x3";
            default -> trimmed;
        };
    }
}
