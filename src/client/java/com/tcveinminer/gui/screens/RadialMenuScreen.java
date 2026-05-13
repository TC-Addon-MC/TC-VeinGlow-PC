package com.tcveinminer.gui.screens;

import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.util.ThemeColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.text.Text;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class RadialMenuScreen extends Screen {

    private final Screen parent;

    // Kích thước hằng số
    private static final float OUTER_R = 90f;
    private static final float INNER_R = 28f;
    private static final float CENTER_R = 24f;

    // Hệ màu
    private static final int COLOR_SLICE_NORMAL  = 0xAA1A1A3A;
    private static final int COLOR_SLICE_HOVER   = 0xCC2A2A5A;
    private static final int COLOR_SLICE_ACTIVE  = 0xCC1B4332;
    private static final int COLOR_CENTER_NORMAL = 0xCC12122A;
    private static final int COLOR_CENTER_HOVER  = 0xCC2A2A52;
    private static final int COLOR_BORDER        = 0xFF4A3F7A;
    private static final int COLOR_BORDER_ACTIVE = 0xFF52B788;

    private int cx, cy;
    private int hoveredSlice = -1;
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
        ModConfig cfg = ConfigManager.get();
        if (cfg != null && cfg.enabledShapes != null) {
            for (ModConfig.MiningShape shape : ModConfig.MiningShape.values()) {
                if (cfg.enabledShapes.contains(shape.name())) {
                    activeShapes.add(shape);
                }
            }
        }
        if (activeShapes.isEmpty()) activeShapes.add(ModConfig.MiningShape.FACE);
    }

    /**
     * CẢI TIẾN 7: Tập trung logic đóng màn hình
     */
    private void closeMenu() {
        client.setScreen(parent);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        hoveredSlice = getHoveredSlice(mouseX, mouseY);
        int n = activeShapes.size();
        float angleStep = 360f / n;

        for (int i = 0; i < n; i++) {
            ModConfig.MiningShape shape = activeShapes.get(i);
            boolean isHovered = hoveredSlice == i;
            boolean isActive  = ConfigManager.get().miningShape == shape;

            float startAngle = -90f + i * angleStep;
            float endAngle   = startAngle + angleStep;

            int bg = isActive ? COLOR_SLICE_ACTIVE : (isHovered ? COLOR_SLICE_HOVER : COLOR_SLICE_NORMAL);

            // Vẽ mảnh menu
            drawSliceEfficient(ctx, cx, cy, INNER_R, OUTER_R, startAngle, endAngle, bg);

            // Tính toán vị trí Text/Icon
            float midAngle = (float) Math.toRadians(startAngle + angleStep / 2f);
            float labelR = (INNER_R + OUTER_R) / 2f;
            int lx = cx + (int)(Math.cos(midAngle) * labelR);
            int ly = cy + (int)(Math.sin(midAngle) * labelR);

            ctx.drawTextWithShadow(textRenderer, shape.icon, lx - textRenderer.getWidth(shape.icon) / 2, ly - 10,
                    isActive ? ThemeColors.TOGGLE_ON_TEXT : ThemeColors.BTN_TEXT);

            String name = shortenLabel(shape.label);
            ctx.drawTextWithShadow(textRenderer, name, lx - textRenderer.getWidth(name) / 2, ly + 1,
                    isActive ? ThemeColors.TOGGLE_ON_TEXT : ThemeColors.TEXT_LABEL);
        }

        // Nút Settings ở tâm
        boolean centerHovered = hoveredSlice == -2;
        drawCircleEfficient(ctx, cx, cy, CENTER_R, centerHovered ? COLOR_CENTER_HOVER : COLOR_CENTER_NORMAL);

        ctx.drawTextWithShadow(textRenderer, "⚙", cx - textRenderer.getWidth("⚙") / 2, cy - 9, ThemeColors.BTN_TEXT);
        ctx.drawTextWithShadow(textRenderer, "Settings", cx - textRenderer.getWidth("Settings") / 2, cy + 1, ThemeColors.TEXT_LABEL);

        if (hoveredSlice >= 0 && hoveredSlice < n) {
            ctx.drawTooltip(textRenderer, Text.literal(activeShapes.get(hoveredSlice).label), mouseX, mouseY);
        }
    }

    private void drawSliceEfficient(DrawContext ctx, int x, int y, float innerR, float outerR, float startDeg, float endDeg, int color) {
        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
        VertexConsumer buffer = ctx.getVertexConsumers().getBuffer(RenderLayer.getGui());

        float startRad = (float) Math.toRadians(startDeg);
        float endRad = (float) Math.toRadians(endDeg);

        // CẢI TIẾN 6: Adaptive segments dựa trên độ dài cung tròn
        int segments = Math.max(6, (int)((outerR * Math.abs(endRad - startRad)) / 8f));
        float step = (endRad - startRad) / segments;

        for (int i = 0; i < segments; i++) {
            float a1 = startRad + i * step;
            float a2 = startRad + (i + 1) * step;

            // Sửa lỗi: Bỏ .next(), thứ tự POSITION -> COLOR
            drawQuad(matrix, buffer,
                    x + (float)Math.cos(a1) * innerR, y + (float)Math.sin(a1) * innerR,
                    x + (float)Math.cos(a1) * outerR, y + (float)Math.sin(a1) * outerR,
                    x + (float)Math.cos(a2) * outerR, y + (float)Math.sin(a2) * outerR,
                    x + (float)Math.cos(a2) * innerR, y + (float)Math.sin(a2) * innerR, color);
        }
    }

    private void drawCircleEfficient(DrawContext ctx, int x, int y, float r, int color) {
        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
        VertexConsumer buffer = ctx.getVertexConsumers().getBuffer(RenderLayer.getGui());
        int segments = 32;
        float step = (float) (Math.PI * 2 / segments);

        for (int i = 0; i < segments; i++) {
            float a1 = i * step;
            float a2 = (i + 1) * step;
            drawQuad(matrix, buffer, (float)x, (float)y, x + (float)Math.cos(a1) * r, y + (float)Math.sin(a1) * r,
                    x + (float)Math.cos(a2) * r, y + (float)Math.sin(a2) * r, (float)x, (float)y, color);
        }
    }

    private void drawQuad(Matrix4f matrix, VertexConsumer buffer, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // POSITION_COLOR format: vertex(matrix, x, y, z).color(r, g, b, a)
        buffer.vertex(matrix, x1, y1, 0).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, 0).color(r, g, b, a);
        buffer.vertex(matrix, x3, y3, 0).color(r, g, b, a);
        buffer.vertex(matrix, x4, y4, 0).color(r, g, b, a);
    }

    /**
     * CẢI TIẾN 9: Truncate văn bản bằng "..." thay vì cắt mất nghĩa
     */
    private String shortenLabel(String label) {
        int maxWidth = 55;
        if (textRenderer.getWidth(label) <= maxWidth) return label;

        String truncated = label;
        while (textRenderer.getWidth(truncated + "...") > maxWidth && truncated.length() > 0) {
            truncated = truncated.substring(0, truncated.length() - 1);
        }
        return truncated.isEmpty() ? "" : truncated.trim() + "...";
    }

    private int getHoveredSlice(int mx, int my) {
        float dx = mx - cx;
        float dy = my - cy;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < CENTER_R) return -2;
        if (dist > OUTER_R)  return -1;

        float angle = (float) Math.toDegrees(Math.atan2(dy, dx)) + 90f;
        if (angle < 0) angle += 360f;
        return (int)(angle / (360f / activeShapes.size())) % activeShapes.size();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        int hovered = getHoveredSlice((int) mouseX, (int) mouseY);
        if (hovered == -2) {
            client.setScreen(new SettingsScreen(this));
            return true;
        }
        if (hovered >= 0 && hovered < activeShapes.size()) {
            ConfigManager.get().miningShape = activeShapes.get(hovered);
            ConfigManager.save();
            closeMenu();
            return true;
        }
        closeMenu();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || keyCode == 71) {
            closeMenu();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() { return false; }
}