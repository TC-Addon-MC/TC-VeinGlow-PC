package com.tcveinminer.client.util;

/**
 * ColorManager — Quản lý tập trung tất cả dữ liệu màu cho outline.
 *
 * Thiết kế lấy cảm hứng từ enchanted.games/app/custom-outlines/:
 * - Preset màu đa dạng với tên gợi nhớ
 * - Utility packing/unpacking ARGB
 * - Rainbow generator theo thời gian thực
 * - Chuyển đổi Hex ↔ RGB
 */
public final class ColorManager {

    // ─────────────────────────────────────────────────────────────
    // PRESET COLOURS  (lấy cảm hứng từ enchanted.games palette)
    // ─────────────────────────────────────────────────────────────

    public static final int PRESET_COUNT = 16;

    /** Tên hiển thị của từng preset */
    public static final String[] PRESET_NAMES = {
            "Emerald",   "Gold",      "Sapphire",  "Ruby",
            "Amethyst",  "Coral",     "Ice",       "Cyan",
            "Lime",      "Rose",      "Sunset",    "Sky",
            "Lava",      "Mint",      "Lavender",  "White"
    };

    /**
     * Giá trị RGB của từng preset [R, G, B] — 0‒255.
     * Thứ tự tương ứng với PRESET_NAMES.
     */
    public static final int[][] PRESET_RGB = {
            { 16, 185, 129},   // Emerald   — xanh ngọc nổi bật
            {216, 161,  91},   // Gold      — vàng cam ấm áp
            { 59, 130, 246},   // Sapphire  — xanh lam đậm
            {239,  68,  68},   // Ruby      — đỏ tươi
            {168,  85, 247},   // Amethyst  — tím hồng
            {249, 115,  22},   // Coral     — cam san hô
            {203, 213, 225},   // Ice       — xám bạc lạnh
            {  6, 182, 212},   // Cyan      — xanh lơ điện
            {132, 204,  22},   // Lime      — xanh lá chanh
            {236,  72, 153},   // Rose      — hồng đậm
            {251, 146,  60},   // Sunset    — cam hoàng hôn
            {125, 211, 252},   // Sky       — xanh trời nhạt
            {220,  38,  38},   // Lava      — đỏ lửa
            { 52, 211, 153},   // Mint      — xanh bạc hà
            {196, 181, 253},   // Lavender  — tím oải hương
            {255, 255, 255},   // White     — trắng thuần
    };

    // ─────────────────────────────────────────────────────────────
    // PACKING / UNPACKING
    // ─────────────────────────────────────────────────────────────

    /** Pack ARGB thành int 32‑bit (alpha 0‑255). */
    public static int packARGB(int a, int r, int g, int b) {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    /** Pack RGB → opaque ARGB (alpha = 255). */
    public static int packRGB(int r, int g, int b) {
        return packARGB(255, r, g, b);
    }

    public static int alphaOf(int argb)  { return (argb >> 24) & 0xFF; }
    public static int redOf(int argb)    { return (argb >> 16) & 0xFF; }
    public static int greenOf(int argb)  { return (argb >> 8)  & 0xFF; }
    public static int blueOf(int argb)   { return  argb        & 0xFF; }

    // ─────────────────────────────────────────────────────────────
    // HEX CONVERSION
    // ─────────────────────────────────────────────────────────────

    /**
     * Chuyển R/G/B → chuỗi hex 6 ký tự (không có '#').
     * Ví dụ: toHex(16, 185, 129) → "10B981"
     */
    public static String toHex(int r, int g, int b) {
        return String.format("%02X%02X%02X", clamp(r), clamp(g), clamp(b));
    }

    /**
     * Parse chuỗi hex (có hoặc không có '#') → int[3] {R, G, B}.
     * Trả về null nếu chuỗi không hợp lệ.
     */
    public static int[] fromHex(String hex) {
        if (hex == null) return null;
        String s = hex.strip().replaceFirst("^#", "");
        if (s.length() != 6) return null;
        try {
            int val = Integer.parseUnsignedInt(s, 16);
            return new int[]{(val >> 16) & 0xFF, (val >> 8) & 0xFF, val & 0xFF};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // RAINBOW GENERATOR
    // ─────────────────────────────────────────────────────────────

    /**
     * Tính màu cầu vồng tại thời điểm {@code timeMs} (System.currentTimeMillis()).
     * Tốc độ chu kỳ mặc định: ~3 giây / vòng.
     * @return int[3] {R, G, B}
     */
    public static int[] rainbowRGB(long timeMs) {
        double t = timeMs / 3000.0;  // chu kỳ 3s
        int r = sinChannel(t, 0.0);
        int g = sinChannel(t, 2.0944); // 2π/3
        int b = sinChannel(t, 4.1888); // 4π/3
        return new int[]{r, g, b};
    }

    /** Tính ARGB cầu vồng với alpha chỉ định. */
    public static int rainbowARGB(long timeMs, int alpha) {
        int[] rgb = rainbowRGB(timeMs);
        return packARGB(alpha, rgb[0], rgb[1], rgb[2]);
    }

    // ─────────────────────────────────────────────────────────────
    // PRESET MATCHING
    // ─────────────────────────────────────────────────────────────

    /**
     * Trả về index preset khớp với R/G/B hiện tại, hoặc -1 nếu không khớp.
     */
    public static int findPreset(int r, int g, int b) {
        for (int i = 0; i < PRESET_COUNT; i++) {
            if (PRESET_RGB[i][0] == r && PRESET_RGB[i][1] == g && PRESET_RGB[i][2] == b) return i;
        }
        return -1;
    }

    // ─────────────────────────────────────────────────────────────
    // LINEAR INTERPOLATION
    // ─────────────────────────────────────────────────────────────

    /** Lerp tuyến tính giữa 2 màu ARGB theo t ∈ [0,1]. */
    public static int lerpARGB(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = lerp8(alphaOf(c1), alphaOf(c2), t);
        int r = lerp8(redOf(c1),   redOf(c2),   t);
        int g = lerp8(greenOf(c1), greenOf(c2), t);
        int b = lerp8(blueOf(c1),  blueOf(c2),  t);
        return packARGB(a, r, g, b);
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────

    private static int clamp(int v)          { return Math.max(0, Math.min(255, v)); }
    private static int lerp8(int a, int b, float t) { return (int)(a + (b - a) * t); }

    /** Tính giá trị kênh sine trong [20, 255] cho rainbow. */
    private static int sinChannel(double t, double phase) {
        return (int) ((Math.sin(t * Math.PI * 2 + phase) + 1.0) * 0.5 * 235 + 20);
    }

    private ColorManager() {}
}
