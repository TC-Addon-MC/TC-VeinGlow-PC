package com.tcveinminer.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.util.ThemeColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class RadialMenuScreen extends Screen {

    private final Screen parent;
    private static final int OUTER_R  = 90;
    private static final int INNER_R  = 28;
    private static final int CENTER_R = 24;

    // ARGB colors
    private static final int COLOR_SLICE_NORMAL  = 0xAA1A1A3A;
    private static final int COLOR_SLICE_HOVER   = 0xCC2A2A5A;
    private static final int COLOR_SLICE_ACTIVE  = 0xCC1B4332;
    private static final int COLOR_CENTER_NORMAL = 0xCC12122A;
    private static final int COLOR_CENTER_HOVER  = 0xCC2A2A52;
    private static final int COLOR_BORDER        = 0xFF4A3F7A;
    private static final int COLOR_BORDER_ACTIVE = 0xFF52B788;
    private static final float BORDER_W          = 1.5f;

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
        // // EFFECT: Thêm hiệu ứng khởi tạo/mở menu tại đây (vd: reset scale animation)
    }

    private void rebuildShapes() {
        activeShapes = new ArrayList<>();
        for (ModConfig.MiningShape shape : ModConfig.MiningShape.values()) {
            if (ConfigManager.get().enabledShapes.contains(shape.name()))
                activeShapes.add(shape);
        }
        if (activeShapes.isEmpty()) activeShapes.add(ModConfig.MiningShape.FACE);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        hoveredSlice = getHoveredSlice(mouseX, mouseY);
        ModConfig.MiningShape currentActive = ConfigManager.get().miningShape;

        MatrixStack matrices = ctx.getMatrices();
        matrices.push();
        matrices.translate(cx, cy, 0);

        // // EFFECT: Thêm hiệu ứng xoay hoặc scale toàn bộ menu tại đây

        Matrix4f mat = matrices.peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.disableCull();

        int n = activeShapes.size();
        float angleStep = 360f / n;

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        // Vẽ slices
        for (int i = 0; i < n; i++) {
            ModConfig.MiningShape shape = activeShapes.get(i);
            boolean isHov = hoveredSlice == i;
            boolean isAct = currentActive == shape;

            // // EFFECT: Tính toán màu sắc dựa trên animation transition tại đây
            int fillColor = isAct ? COLOR_SLICE_ACTIVE : (isHov ? COLOR_SLICE_HOVER : COLOR_SLICE_NORMAL);

            float startRad = (float) Math.toRadians(-90f + i * angleStep);
            float endRad   = (float) Math.toRadians(-90f + (i + 1) * angleStep);

            RadialDrawingUtils.fillArc(buf, mat, INNER_R, OUTER_R, startRad, endRad, fillColor);
        }

        // Nút tâm
        boolean centerHov = hoveredSlice == -2;
        RadialDrawingUtils.fillCircle(buf, mat, CENTER_R, centerHov ? COLOR_CENTER_HOVER : COLOR_CENTER_NORMAL);

        BufferRenderer.drawWithGlobalProgram(buf.end());

        // Vẽ Borders
        BufferBuilder borderBuf = tess.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < n; i++) {
            boolean isAct = currentActive == activeShapes.get(i);
            int borderColor = isAct ? COLOR_BORDER_ACTIVE : COLOR_BORDER;
            float startRad = (float) Math.toRadians(-90f + i * angleStep);
            float endRad   = (float) Math.toRadians(-90f + (i + 1) * angleStep);
            RadialDrawingUtils.strokeArc(borderBuf, mat, INNER_R, OUTER_R, startRad, endRad, BORDER_W, borderColor);
        }
        RadialDrawingUtils.strokeRing(borderBuf, mat, CENTER_R - BORDER_W, CENTER_R, 0, (float)(2 * Math.PI), COLOR_BORDER);
        BufferRenderer.drawWithGlobalProgram(borderBuf.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        matrices.pop();

        // Render Labels
        renderLabels(ctx, n, angleStep, currentActive);

        // Tooltip
        if (hoveredSlice >= 0 && hoveredSlice < n) {
            renderTooltip(ctx, mouseX, mouseY);
        }
    }

    private void renderLabels(DrawContext ctx, int n, float angleStep, ModConfig.MiningShape currentActive) {
        for (int i = 0; i < n; i++) {
            ModConfig.MiningShape shape = activeShapes.get(i);
            boolean isAct = currentActive == shape;
            float mid = (float) Math.toRadians(-90f + (i + 0.5f) * angleStep);

            // // EFFECT: Thêm hiệu ứng bay ra (pop-out) cho label khi hover
            int labelR = (INNER_R + OUTER_R) / 2;
            int lx = cx + (int)(Math.cos(mid) * labelR);
            int ly = cy + (int)(Math.sin(mid) * labelR);

            String icon = shape.icon;
            ctx.drawTextWithShadow(textRenderer, icon, lx - textRenderer.getWidth(icon) / 2, ly - 10,
                    isAct ? ThemeColors.TOGGLE_ON_TEXT : ThemeColors.BTN_TEXT);

            String name = shortenLabel(shape.label);
            ctx.drawTextWithShadow(textRenderer, name, lx - textRenderer.getWidth(name) / 2, ly + 1,
                    isAct ? ThemeColors.TOGGLE_ON_TEXT : ThemeColors.TEXT_LABEL);
        }

        ctx.drawTextWithShadow(textRenderer, "⚙", cx - textRenderer.getWidth("⚙") / 2, cy - 9, ThemeColors.BTN_TEXT);
        ctx.drawTextWithShadow(textRenderer, "Settings", cx - textRenderer.getWidth("Settings") / 2, cy + 1, ThemeColors.TEXT_LABEL);
    }

    private void renderTooltip(DrawContext ctx, int mouseX, int mouseY) {
        String fullName = activeShapes.get(hoveredSlice).label;
        int tw = textRenderer.getWidth(fullName) + 8;
        // // EFFECT: Thêm hiệu ứng fade-in cho tooltip
        ctx.fill(mouseX + 6, mouseY - 14, mouseX + 6 + tw, mouseY, 0xCC000000);
        ctx.drawTextWithShadow(textRenderer, fullName, mouseX + 10, mouseY - 11, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        int hovered = getHoveredSlice((int) mouseX, (int) mouseY);

        if (hovered == -2) {
            // // EFFECT: Thêm hiệu ứng click trước khi chuyển screen
            client.setScreen(new SettingsScreen(this));
            return true;
        }
        if (hovered >= 0 && hovered < activeShapes.size()) {
            ConfigManager.get().miningShape = activeShapes.get(hovered);
            ConfigManager.save();
            // // EFFECT: Thêm hiệu ứng chọn thành công
            client.setScreen(parent);
            return true;
        }
        client.setScreen(parent);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || keyCode == 71) {
            // // EFFECT: Hiệu ứng đóng menu
            client.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() { return false; }

    private int getHoveredSlice(int mx, int my) {
        float dx = mx - cx, dy = my - cy;
        float dist2 = dx * dx + dy * dy;
        if (dist2 < CENTER_R * CENTER_R) return -2;
        if (dist2 > (float) OUTER_R * OUTER_R) return -1;
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx)) + 90f;
        if (angle < 0) angle += 360f;
        return (int)(angle / (360f / activeShapes.size())) % activeShapes.size();
    }

    private String shortenLabel(String label) {
        if (textRenderer.getWidth(label) <= 55) return label;
        return label.split(" ")[0];
    }
}