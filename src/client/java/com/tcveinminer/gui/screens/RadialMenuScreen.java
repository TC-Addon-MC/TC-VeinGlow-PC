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
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import com.tcveinminer.config.ClientConfigManager;
import com.tcveinminer.config.ClientConfig;

public class RadialMenuScreen extends Screen {

    /** Unified entry for both builtin enum shapes and custom equation shapes. */
    private record SliceEntry(String id, String icon, String label) {}

    private final Screen parent;
    private static final int OUTER_R = 90;
    private static final int INNER_R = 28;
    private static final int CENTER_R = 24;

    private static final int COLOR_SLICE_NORMAL = 0xAA1A1A3A;
    private static final int COLOR_SLICE_HOVER = 0xCC2A2A5A;
    private static final int COLOR_SLICE_ACTIVE = 0xCC1B4332;
    private static final int COLOR_CENTER_NORMAL = 0xCC12122A;
    private static final int COLOR_CENTER_HOVER = 0xCC2A2A52;
    private static final int COLOR_BORDER = 0xFF4A3F7A;
    private static final int COLOR_BORDER_ACTIVE = 0xFF52B788;
    private static final float BORDER_W = 1.5f;

    private int cx, cy;
    private int hoveredSlice = -1;
    private List<SliceEntry> activeShapes;

    private float animOpen = 0.0f;
    private float[] sliceHoverProgress;

    private boolean isClosing = false;
    private float clickProgress = 0.0f;
    private int clickedAction = -1;

    public RadialMenuScreen(Screen parent) {
        super(Text.literal("TC VeinMiner"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        cx = width / 2;
        cy = height / 2;
        rebuildShapes();
        animOpen = 0.0f;
        isClosing = false;
        clickProgress = 0.0f;
        sliceHoverProgress = new float[activeShapes.size() + 1];
    }

    private void rebuildShapes() {
        activeShapes = new ArrayList<>();
        // Builtin enum shapes
        for (ModConfig.MiningShape shape : ModConfig.MiningShape.values()) {
            if (ClientConfigManager.instance.enabledShapes.contains(shape.name())
                    && !ClientConfigManager.instance.serverDisabledShapes.contains(shape.name()))
                activeShapes.add(new SliceEntry(shape.name(), shape.icon, shape.label));
        }
        // Custom equation shapes
        for (ClientConfig.CustomShapeEntry entry : ClientConfigManager.instance.customShapes) {
            if (ClientConfigManager.instance.enabledShapes.contains(entry.strategyId)
                    && !ClientConfigManager.instance.serverDisabledShapes.contains("custom")
                    && !ClientConfigManager.instance.serverDisabledShapes.contains(entry.strategyId))
                activeShapes.add(new SliceEntry(entry.strategyId, "✦", entry.name));
        }
        if (activeShapes.isEmpty())
            activeShapes.add(new SliceEntry(ModConfig.MiningShape.FACE.name(),
                    ModConfig.MiningShape.FACE.icon, ModConfig.MiningShape.FACE.label));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {

        if (isClosing) {
            clickProgress += delta * 0.15f;
            if (clickProgress >= 1.0f) {
                executeClickAction();
                return;
            }
        } else {
            animOpen = MathHelper.lerp(delta * 0.3f, animOpen, 1.0f);
            hoveredSlice = getHoveredSlice(mouseX, mouseY);
        }

        // Resolve currentShape as string ID (works for both builtin and custom)
        String currentActiveId = ClientConfigManager.instance.currentShape;
        if (currentActiveId == null || currentActiveId.isBlank())
            currentActiveId = ModConfig.MiningShape.FACE.name();

        MatrixStack matrices = ctx.getMatrices();
        matrices.push();
        matrices.translate(cx, cy, 0);

        float t = animOpen;
        float scale = 1.0f + (float)(Math.sin(t * Math.PI * 3.5f) * (1.0f - t) * 0.15f);
        if (t < 0.99f) {
            scale *= (float)Math.pow(t, 0.5);
        }
        matrices.scale(scale, scale, 1.0f);

        Matrix4f mat = matrices.peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.disableCull();

        int n = activeShapes.size();
        float angleStep = 360f / n;

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < n; i++) {
            boolean isHov = (!isClosing && hoveredSlice == i);
            boolean isAct = activeShapes.get(i).id().equals(currentActiveId);

            sliceHoverProgress[i] = MathHelper.lerp(delta * 0.3f, sliceHoverProgress[i], isHov ? 1.0f : 0.0f);
            float hovP = sliceHoverProgress[i];

            float delay = i * 0.05f;
            float sliceT = MathHelper.clamp((animOpen - delay) * 1.5f, 0f, 1f);
            float slicePop = 1.0f - (float)Math.pow(1.0f - sliceT, 3);

            int color = isAct ? COLOR_SLICE_ACTIVE : lerpColor(COLOR_SLICE_NORMAL, COLOR_SLICE_HOVER, hovP);

            float startRad = (float) Math.toRadians(-90f + i * angleStep);
            float endRad = (float) Math.toRadians(-90f + (i + 1) * angleStep);

            float hovRadiusOff = hovP * 4.0f;
            float currentInnerR = INNER_R * slicePop;
            float currentOuterR = (OUTER_R + hovRadiusOff) * slicePop;

            if (hovP > 0.01f && !isClosing) {
                int glowAlpha = (int)(hovP * 80);
                int glowColor = (0x00FFFFFF & COLOR_SLICE_HOVER) | (glowAlpha << 24);
                RadialDrawingUtils.fillArc(buf, mat, currentInnerR - 2, currentOuterR + 8, startRad, endRad, glowColor);
            }

            if (isClosing && clickedAction == i) {
                float waveR = INNER_R + (clickProgress * (OUTER_R - INNER_R + 20));
                float splitR = MathHelper.clamp(waveR, currentInnerR, currentOuterR);

                RadialDrawingUtils.fillArc(buf, mat, currentInnerR, splitR, startRad, endRad, COLOR_SLICE_ACTIVE);

                if (splitR < currentOuterR) {
                    RadialDrawingUtils.fillArc(buf, mat, splitR, currentOuterR, startRad, endRad, color);
                }
            } else {
                RadialDrawingUtils.fillArc(buf, mat, currentInnerR, currentOuterR, startRad, endRad, color);
            }
        }

        int centerIdx = n;
        boolean centerHov = (!isClosing && hoveredSlice == -2);
        sliceHoverProgress[centerIdx] = MathHelper.lerp(delta * 0.3f, sliceHoverProgress[centerIdx], centerHov ? 1.0f : 0.0f);
        int centerColor = lerpColor(COLOR_CENTER_NORMAL, COLOR_CENTER_HOVER, sliceHoverProgress[centerIdx]);

        float pulse = 1.0f + MathHelper.sin((float)(Util.getMeasuringTimeMs() / 200.0)) * 0.03f;
        float currentCenterR = CENTER_R * animOpen * pulse;

        RadialDrawingUtils.fillCircle(buf, mat, currentCenterR, centerColor);

        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        matrices.pop();

        renderLabels(ctx, n, angleStep, currentActiveId);

        if (!isClosing && hoveredSlice >= 0 && hoveredSlice < n) {
            renderTooltip(ctx, mouseX, mouseY);
        }
    }

    private void executeClickAction() {

        if (clickedAction == -2) {
            client.setScreen(new MainMenuScreen(this));

        } else if (clickedAction >= 0) {

            ClientConfigManager.instance.currentShape =
                    activeShapes.get(clickedAction).id();

            ClientConfigManager.save();

            client.setScreen(parent);
        }
    }

    // ===== phần còn lại giữ nguyên =====

    private void renderLabels(DrawContext ctx, int n, float angleStep, String currentActiveId) {
        for (int i = 0; i < n; i++) {
            SliceEntry entry = activeShapes.get(i);
            boolean isAct = entry.id().equals(currentActiveId);
            float mid = (float) Math.toRadians(-90f + (i + 0.5f) * angleStep);

            float hovP = sliceHoverProgress[i];
            int baseLabelR = (INNER_R + OUTER_R) / 2;

            float sliceT = MathHelper.clamp((animOpen - (i * 0.05f)) * 1.5f, 0f, 1f);
            float slicePop = 1.0f - (float)Math.pow(1.0f - sliceT, 3);

            float labelR = baseLabelR * slicePop;

            float pull = hovP * 6.0f;
            float dx = (float)Math.cos(mid) * pull;
            float dy = (float)Math.sin(mid) * pull;

            int lx = cx + (int)(Math.cos(mid) * labelR + dx);
            int ly = cy + (int)(Math.sin(mid) * labelR + dy);

            int alpha = (int)(slicePop * 255);
            if (alpha < 10) continue;

            int iconColor = applyAlpha(isAct ? ThemeColors.TOGGLE_ON_TEXT : ThemeColors.BTN_TEXT, alpha);
            int textColor = applyAlpha(isAct ? ThemeColors.TOGGLE_ON_TEXT : ThemeColors.TEXT_LABEL, alpha);

            String icon = entry.icon();
            ctx.drawTextWithShadow(textRenderer, icon, lx - textRenderer.getWidth(icon) / 2, ly - 10, iconColor);

            String name = shortenLabel(entry.label());
            ctx.drawTextWithShadow(textRenderer, name, lx - textRenderer.getWidth(name) / 2, ly + 1, textColor);
        }

        int centerAlpha = (int)(animOpen * 255);
        if (centerAlpha > 10) {
            ctx.drawTextWithShadow(textRenderer, "⚙", cx - textRenderer.getWidth("⚙") / 2, cy - 9, applyAlpha(ThemeColors.BTN_TEXT, centerAlpha));
            ctx.drawTextWithShadow(textRenderer, "Settings", cx - textRenderer.getWidth("Settings") / 2, cy + 1, applyAlpha(ThemeColors.TEXT_LABEL, centerAlpha));
        }
    }

    private int applyAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (MathHelper.clamp(alpha, 0, 255) << 24);
    }

    private void renderTooltip(DrawContext ctx, int mouseX, int mouseY) {
        String fullName = activeShapes.get(hoveredSlice).label();
        int tw = textRenderer.getWidth(fullName) + 8;
        ctx.fill(mouseX + 6, mouseY - 14, mouseX + 6 + tw, mouseY, 0xCC000000);
        ctx.drawTextWithShadow(textRenderer, fullName, mouseX + 10, mouseY - 11, 0xFFFFFFFF);
    }

    private int lerpColor(int c1, int c2, float p) {
        int a = (int) MathHelper.lerp(p, (c1 >> 24) & 0xFF, (c2 >> 24) & 0xFF);
        int r = (int) MathHelper.lerp(p, (c1 >> 16) & 0xFF, (c2 >> 16) & 0xFF);
        int g = (int) MathHelper.lerp(p, (c1 >> 8) & 0xFF, (c2 >> 8) & 0xFF);
        int b = (int) MathHelper.lerp(p, c1 & 0xFF, c2 & 0xFF);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || isClosing) return false;

        int hovered = getHoveredSlice((int) mouseX, (int) mouseY);

        if (hovered == -2 || (hovered >= 0 && hovered < activeShapes.size())) {
            isClosing = true;
            clickedAction = hovered;
            return true;
        }

        client.setScreen(parent);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || keyCode == 71) {
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
