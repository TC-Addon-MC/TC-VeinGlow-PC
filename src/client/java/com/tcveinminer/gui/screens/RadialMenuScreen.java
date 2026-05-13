package com.tcveinminer.gui.screens;

import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.util.ThemeColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class RadialMenuScreen extends Screen {

    private final Screen parent;

    // Kích thước
    private static final int OUTER_R = 90;  // bán kính ngoài slice
    private static final int INNER_R = 28;  // bán kính trong (lỗ giữa)
    private static final int CENTER_R = 24; // nút tâm Settings

    // Màu slice
    private static final int COLOR_SLICE_NORMAL  = 0xAA1A1A3A;
    private static final int COLOR_SLICE_HOVER   = 0xCC2A2A5A;
    private static final int COLOR_SLICE_ACTIVE  = 0xCC1B4332;
    private static final int COLOR_CENTER_NORMAL = 0xCC12122A;
    private static final int COLOR_CENTER_HOVER  = 0xCC2A2A52;
    private static final int COLOR_BORDER        = 0xFF4A3F7A;
    private static final int COLOR_BORDER_ACTIVE = 0xFF52B788;

    private int cx, cy; // tâm màn hình
    private int hoveredSlice = -1; // index slice đang hover (-1 = không có, -2 = tâm)

    // Danh sách shape hiện đang bật
    private List<ModConfig.MiningShape> activeShapes;

    public RadialMenuScreen(Screen parent) {
        super(Text.literal("TC VeinMiner"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        cx = width / 2;
        cy = height / 2;
        rebuildShapes();
    }

    private void rebuildShapes() {
        activeShapes = new ArrayList<>();
        for (ModConfig.MiningShape shape : ModConfig.MiningShape.values()) {
            if (ConfigManager.get().enabledShapes.contains(shape.name())) {
                activeShapes.add(shape);
            }
        }
        // Luôn có ít nhất FACE
        if (activeShapes.isEmpty()) {
            activeShapes.add(ModConfig.MiningShape.FACE);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // KHÔNG gọi renderBackground → nền trong suốt hoàn toàn

        // Tính slice đang hover
        hoveredSlice = getHoveredSlice(mouseX, mouseY);

        int n = activeShapes.size();
        float angleStep = 360f / n;

        // Vẽ từng slice
        for (int i = 0; i < n; i++) {
            ModConfig.MiningShape shape = activeShapes.get(i);
            boolean isHovered = hoveredSlice == i;
            boolean isActive  = ConfigManager.get().miningShape == shape;

            float startAngle = -90f + i * angleStep;
            float endAngle   = startAngle + angleStep;

            int bg = isActive ? COLOR_SLICE_ACTIVE : (isHovered ? COLOR_SLICE_HOVER : COLOR_SLICE_NORMAL);
            int border = isActive ? COLOR_BORDER_ACTIVE : COLOR_BORDER;

            drawSlice(ctx, cx, cy, INNER_R, OUTER_R, startAngle, endAngle, bg, border);

            // Label: icon + tên ở giữa slice
            float midAngle = (float) Math.toRadians(startAngle + angleStep / 2f);
            int labelR = (INNER_R + OUTER_R) / 2;
            int lx = cx + (int)(Math.cos(midAngle) * labelR);
            int ly = cy + (int)(Math.sin(midAngle) * labelR);

            // Icon
            String icon = shape.icon;
            ctx.drawTextWithShadow(textRenderer, icon,
                lx - textRenderer.getWidth(icon) / 2,
                ly - 10,
                isActive ? ThemeColors.TOGGLE_ON_TEXT : ThemeColors.BTN_TEXT);

            // Tên (cắt ngắn nếu quá dài)
            String name = shortenLabel(shape.label);
            ctx.drawTextWithShadow(textRenderer, name,
                lx - textRenderer.getWidth(name) / 2,
                ly + 1,
                isActive ? ThemeColors.TOGGLE_ON_TEXT : ThemeColors.TEXT_LABEL);
        }

        // Vẽ nút tâm (Settings)
        boolean centerHovered = hoveredSlice == -2;
        drawCircle(ctx, cx, cy, CENTER_R,
            centerHovered ? COLOR_CENTER_HOVER : COLOR_CENTER_NORMAL, COLOR_BORDER);

        String gear = "⚙";
        ctx.drawTextWithShadow(textRenderer, gear,
            cx - textRenderer.getWidth(gear) / 2, cy - 9,
            ThemeColors.BTN_TEXT);
        String settingsLabel = "Settings";
        ctx.drawTextWithShadow(textRenderer, settingsLabel,
            cx - textRenderer.getWidth(settingsLabel) / 2, cy + 1,
            ThemeColors.TEXT_LABEL);

        // Tooltip: tên đầy đủ của slice đang hover
        if (hoveredSlice >= 0 && hoveredSlice < n) {
            String fullName = activeShapes.get(hoveredSlice).label;
            int tw = textRenderer.getWidth(fullName) + 8;
            ctx.fill(mouseX + 6, mouseY - 14, mouseX + 6 + tw, mouseY, 0xCC000000);
            ctx.drawTextWithShadow(textRenderer, fullName, mouseX + 10, mouseY - 11, 0xFFFFFFFF);
        }

        // Không gọi super.render() karena tidak ada widget standard
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        int hovered = getHoveredSlice((int) mouseX, (int) mouseY);

        if (hovered == -2) {
            // Click tâm → mở Settings screen
            client.setScreen(new SettingsScreen(this));
            return true;
        }

        if (hovered >= 0 && hovered < activeShapes.size()) {
            // Click slice → đổi mining shape
            ConfigManager.get().miningShape = activeShapes.get(hovered);
            ConfigManager.save();
            client.setScreen(parent); // đóng menu sau khi chọn
            return true;
        }

        // Click ngoài vòng tròn → đóng menu
        client.setScreen(parent);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC hoặc G đóng menu
        if (keyCode == 256 || keyCode == 71) { // 71 = G
            client.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() { return false; }

    // ── Helpers ───────────────────────────────────────────────────────

    /**
     * Trả về index slice đang hover, -2 nếu hover tâm, -1 nếu ngoài.
     */
    private int getHoveredSlice(int mx, int my) {
        float dx = mx - cx;
        float dy = my - cy;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist < CENTER_R) return -2; // tâm
        if (dist > OUTER_R)  return -1; // ngoài

        // Góc từ trên xuống (−90° ở đỉnh)
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx)) + 90f;
        if (angle < 0) angle += 360f;

        int n = activeShapes.size();
        float angleStep = 360f / n;
        return (int)(angle / angleStep) % n;
    }

    /**
     * Vẽ hình tròn đặc (approx bằng fill hình vuông + clip tròn bằng pixel check).
     * Dùng cách vẽ pixel-by-pixel cho tròn nhỏ.
     */
    private void drawCircle(DrawContext ctx, int x, int y, int r, int fillColor, int borderColor) {
        // Fill
        for (int px = -r; px <= r; px++) {
            for (int py = -r; py <= r; py++) {
                if (px * px + py * py <= r * r) {
                    ctx.fill(x + px, y + py, x + px + 1, y + py + 1, fillColor);
                }
            }
        }
        // Border
        for (int px = -r; px <= r; px++) {
            for (int py = -r; py <= r; py++) {
                int d2 = px * px + py * py;
                if (d2 <= r * r && d2 >= (r - 1) * (r - 1)) {
                    ctx.fill(x + px, y + py, x + px + 1, y + py + 1, borderColor);
                }
            }
        }
    }

    /**
     * Vẽ slice hình quạt từ innerR đến outerR, góc từ startDeg đến endDeg.
     */
    private void drawSlice(DrawContext ctx, int x, int y,
                            int innerR, int outerR,
                            float startDeg, float endDeg,
                            int fillColor, int borderColor) {
        // Vẽ pixel-by-pixel trong bounding box
        for (int px = -outerR; px <= outerR; px++) {
            for (int py = -outerR; py <= outerR; py++) {
                float d2 = px * px + py * py;
                if (d2 < innerR * innerR || d2 > (float) outerR * outerR) continue;

                // Tính góc (0° ở trên, tăng theo chiều kim đồng hồ)
                float ang = (float) Math.toDegrees(Math.atan2(py, px)) + 90f;
                if (ang < 0) ang += 360f;

                // Normalize start/end về [0, 360)
                float s = ((startDeg % 360f) + 360f) % 360f;
                float e = ((endDeg   % 360f) + 360f) % 360f;

                boolean inSlice;
                if (s < e) inSlice = ang >= s && ang < e;
                else       inSlice = ang >= s || ang < e; // wrap around 0

                if (!inSlice) continue;

                // Pixel nằm trong slice → vẽ fill
                ctx.fill(x + px, y + py, x + px + 1, y + py + 1, fillColor);

                // Border: cạnh ngoài, trong, hoặc cạnh bên (góc gần startDeg/endDeg)
                boolean isBorder = d2 >= (outerR - 1f) * (outerR - 1f)
                    || d2 <= (innerR + 1f) * (innerR + 1f);

                // Cạnh bên (theo góc)
                if (!isBorder) {
                    float delta = 360f / activeShapes.size();
                    float distToStart = Math.abs(ang - s);
                    if (distToStart > 180f) distToStart = 360f - distToStart;
                    float distToEnd = Math.abs(ang - e);
                    if (distToEnd > 180f) distToEnd = 360f - distToEnd;
                    // ≈ 1 pixel tại góc bên
                    float angBorder = 1.5f / ((float) Math.sqrt(d2));
                    if (distToStart < angBorder || distToEnd < angBorder) isBorder = true;
                }

                if (isBorder) ctx.fill(x + px, y + py, x + px + 1, y + py + 1, borderColor);
            }
        }
    }

    private String shortenLabel(String label) {
        if (textRenderer.getWidth(label) <= 55) return label;
        // Cắt lấy từ đầu
        int n = activeShapes.size();
        String[] words = label.split(" ");
        return words[0]; // chỉ lấy từ đầu tiên nếu quá dài
    }
}
