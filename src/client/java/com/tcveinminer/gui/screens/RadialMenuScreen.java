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
    private static final float OUTER_R = 90f;
    private static final float INNER_R = 28f;
    private static final float CENTER_R = 24f;
    private static final int SEGMENTS = 64;

    private int cx, cy, hoveredSlice = -1;
    private List<ModConfig.MiningShape> activeShapes;

    public RadialMenuScreen(Screen parent) {
        super(Text.literal("TC VeinMiner"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        cx = width / 2;
        cy = height / 2;
        activeShapes = new ArrayList<>();
        ModConfig cfg = ConfigManager.get();
        for (ModConfig.MiningShape shape : ModConfig.MiningShape.values()) {
            if (cfg.enabledShapes.contains(shape.name())) activeShapes.add(shape);
        }
        if (activeShapes.isEmpty()) activeShapes.add(ModConfig.MiningShape.FACE);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        hoveredSlice = getHoveredSlice(mouseX, mouseY);
        int n = activeShapes.size();
        float angleStep = 360f / n;
        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();

        VertexConsumer buffer = ctx.getVertexConsumers().getBuffer(RenderLayer.getGuiOverlay());

        // 1. Vẽ các nút xung quanh
        for (int i = 0; i < n; i++) {
            boolean isActive = ConfigManager.get().miningShape == activeShapes.get(i);
            int color = isActive ? 0xCC1B4332 : (hoveredSlice == i ? 0xCC2A2A5A : 0xAA1A1A3A);

            drawRadialSegment(matrix, buffer, cx, cy, INNER_R, OUTER_R,
                    -90f + i * angleStep, -90f + (i + 1) * angleStep, color);
        }

        // 2. Vẽ nút tròn giữa (bán kính trong = 0, góc 0 -> 360)
        int centerColor = hoveredSlice == -2 ? 0xCC2A2A52 : 0xCC12122A;
        drawRadialSegment(matrix, buffer, cx, cy, 0f, CENTER_R, 0f, 360f, centerColor);

        ctx.draw();

        // 3. Vẽ Text và Icon
        renderLabels(ctx, n, angleStep);
    }

    // Hàm vẽ chung cho mọi hình (tròn, quạt, vành khuyên)
    private void drawRadialSegment(Matrix4f mat, VertexConsumer buf, int x, int y, float inR, float outR, float start, float end, int color) {
        float sRad = (float) Math.toRadians(start);
        float eRad = (float) Math.toRadians(end);
        float step = (eRad - sRad) / SEGMENTS;
        int a = (color >> 24) & 0xFF, r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;

        for (int i = 0; i < SEGMENTS; i++) {
            float a1 = sRad + i * step, a2 = sRad + (i + 1) * step;
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float c2 = (float) Math.cos(a2), s2 = (float) Math.sin(a2);

            buf.vertex(mat, x + c1 * inR,  y + s1 * inR,  0.01f).color(r, g, b, a);
            buf.vertex(mat, x + c1 * outR, y + s1 * outR, 0.01f).color(r, g, b, a);
            buf.vertex(mat, x + c2 * outR, y + s2 * outR, 0.01f).color(r, g, b, a);
            buf.vertex(mat, x + c2 * inR,  y + s2 * inR,  0.01f).color(r, g, b, a);
        }
    }

    private void renderLabels(DrawContext ctx, int n, float angleStep) {
        for (int i = 0; i < n; i++) {
            ModConfig.MiningShape shape = activeShapes.get(i);
            float midAngle = (float)Math.toRadians(-90f + i * angleStep + angleStep / 2f);
            int lx = cx + (int)(Math.cos(midAngle) * (INNER_R + OUTER_R) / 2f);
            int ly = cy + (int)(Math.sin(midAngle) * (INNER_R + OUTER_R) / 2f);
            boolean isActive = ConfigManager.get().miningShape == shape;

            ctx.drawTextWithShadow(textRenderer, shape.icon, lx - textRenderer.getWidth(shape.icon) / 2, ly - 10, isActive ? ThemeColors.TOGGLE_ON_TEXT : ThemeColors.BTN_TEXT);
            String label = textRenderer.getWidth(shape.label) > 50 ? shape.label.substring(0, 5) + ".." : shape.label;
            ctx.drawTextWithShadow(textRenderer, label, lx - textRenderer.getWidth(label) / 2, ly + 1, isActive ? ThemeColors.TOGGLE_ON_TEXT : ThemeColors.TEXT_LABEL);
        }
        ctx.drawTextWithShadow(textRenderer, "⚙", cx - textRenderer.getWidth("⚙") / 2, cy - 9, ThemeColors.BTN_TEXT);
        ctx.drawTextWithShadow(textRenderer, "Settings", cx - textRenderer.getWidth("Settings") / 2, cy + 1, ThemeColors.TEXT_LABEL);
    }

    private int getHoveredSlice(int mx, int my) {
        float dx = mx - cx, dy = my - cy, dist = (float)Math.sqrt(dx * dx + dy * dy);
        if (dist < CENTER_R) return -2;
        if (dist > OUTER_R) return -1;
        float angle = (float)Math.toDegrees(Math.atan2(dy, dx)) + 90f;
        if (angle < 0) angle += 360f;
        return (int)(angle / (360f / activeShapes.size())) % activeShapes.size();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int hovered = getHoveredSlice((int)mx, (int)my);
        if (hovered == -2) { client.setScreen(new SettingsScreen(this)); return true; }
        if (hovered >= 0) {
            ConfigManager.get().miningShape = activeShapes.get(hovered);
            ConfigManager.save();
        }
        client.setScreen(parent);
        return true;
    }

    @Override
    public boolean shouldPause() { return false; }
}