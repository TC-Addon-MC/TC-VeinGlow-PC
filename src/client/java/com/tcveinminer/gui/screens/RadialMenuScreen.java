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

public class RadialMenuScreen extends Screen {

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
    private List<ModConfig.MiningShape> activeShapes;

    private float animOpen = 0.0f;
    private float[] sliceHoverProgress;

    // Biến cho hiệu ứng click (Shockwave)
    private boolean isClosing = false;
    private float clickProgress = 0.0f;
    private int clickedAction = -1; // -2 cho settings, >= 0 cho shape

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
        for (ModConfig.MiningShape shape : ModConfig.MiningShape.values()) {
            if (ConfigManager.get().enabledShapes.contains(shape.name()))
                activeShapes.add(shape);
        }
        if (activeShapes.isEmpty()) activeShapes.add(ModConfig.MiningShape.FACE);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Cập nhật logic đóng/mở
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

        ModConfig.MiningShape currentActive = ConfigManager.get().miningShape;

        MatrixStack matrices = ctx.getMatrices();
        matrices.push();
        matrices.translate(cx, cy, 0);

        // HIỆU ỨNG 1: Elastic Open (phồng ra rồi co lại nhẹ)
        float t = animOpen;
        float scale = 1.0f + (float)(Math.sin(t * Math.PI * 3.5f) * (1.0f - t) * 0.15f);
        if (t < 0.99f) {
            scale *= (float)Math.pow(t, 0.5); // Giúp mượt phần xuất hiện ban đầu
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
            boolean isAct = (currentActive == activeShapes.get(i));

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

            // HIỆU ỨNG: Glow hover (sẽ mờ dần khi đang click)
            if (hovP > 0.01f && !isClosing) {
                int glowAlpha = (int)(hovP * 80);
                int glowColor = (0x00FFFFFF & COLOR_SLICE_HOVER) | (glowAlpha << 24);
                RadialDrawingUtils.fillArc(buf, mat, currentInnerR - 2, currentOuterR + 8, startRad, endRad, glowColor);
            }

            // LOGIC MỚI: Sóng quét tới đâu đổi màu tới đó
            if (isClosing && clickedAction == i) {
                // Tính bán kính của sóng dựa trên clickProgress
                // Thêm 20f để đảm bảo sóng quét qua hoàn toàn rìa ngoài
                float waveR = INNER_R + (clickProgress * (OUTER_R - INNER_R + 20));
                float splitR = MathHelper.clamp(waveR, currentInnerR, currentOuterR);

                // 1. Vẽ phần đã bị sóng quét qua (Màu Active)
                RadialDrawingUtils.fillArc(buf, mat, currentInnerR, splitR, startRad, endRad, COLOR_SLICE_ACTIVE);

                // 2. Vẽ phần còn lại chưa bị sóng quét tới (Màu cũ)
                if (splitR < currentOuterR) {
                    RadialDrawingUtils.fillArc(buf, mat, splitR, currentOuterR, startRad, endRad, color);
                }
            } else {
                // Vẽ bình thường cho các slice khác hoặc khi không click
                RadialDrawingUtils.fillArc(buf, mat, currentInnerR, currentOuterR, startRad, endRad, color);
            }
        }

        // Nút tâm
        int centerIdx = n;
        boolean centerHov = (!isClosing && hoveredSlice == -2);
        sliceHoverProgress[centerIdx] = MathHelper.lerp(delta * 0.3f, sliceHoverProgress[centerIdx], centerHov ? 1.0f : 0.0f);
        int centerColor = lerpColor(COLOR_CENTER_NORMAL, COLOR_CENTER_HOVER, sliceHoverProgress[centerIdx]);

        // HIỆU ỨNG 5: Center pulse breathing
        float pulse = 1.0f + MathHelper.sin((float)(Util.getMeasuringTimeMs() / 200.0)) * 0.03f;
        float currentCenterR = CENTER_R * animOpen * pulse;
        RadialDrawingUtils.fillCircle(buf, mat, currentCenterR, centerColor);

        BufferRenderer.drawWithGlobalProgram(buf.end());

        // Vẽ Borders
        BufferBuilder borderBuf = tess.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < n; i++) {
            float sliceT = MathHelper.clamp((animOpen - (i * 0.05f)) * 1.5f, 0f, 1f);
            float slicePop = 1.0f - (float)Math.pow(1.0f - sliceT, 3);

            boolean isAct = currentActive == activeShapes.get(i);
            int borderColor = isAct ? COLOR_BORDER_ACTIVE : COLOR_BORDER;
            float startRad = (float) Math.toRadians(-90f + i * angleStep);
            float endRad = (float) Math.toRadians(-90f + (i + 1) * angleStep);

            float currentInnerR = INNER_R * slicePop;
            float currentOuterR = (OUTER_R + (sliceHoverProgress[i] * 4.0f)) * slicePop;

            RadialDrawingUtils.strokeArc(borderBuf, mat, currentInnerR, currentOuterR, startRad, endRad, BORDER_W, borderColor);
        }
        RadialDrawingUtils.strokeRing(borderBuf, mat, currentCenterR - BORDER_W, currentCenterR, 0, (float)(2 * Math.PI), COLOR_BORDER);

        // HIỆU ỨNG 6: Click Shockwave (vòng sóng lan tỏa khi chọn)
        if (isClosing && clickedAction >= 0) {
            float shockWaveR = INNER_R + (clickProgress * 60f);
            int shockAlpha = (int)((1.0f - clickProgress) * 200);
            int shockColor = (0x00FFFFFF & COLOR_BORDER_ACTIVE) | (shockAlpha << 24);
            float startRad = (float) Math.toRadians(-90f + clickedAction * angleStep);
            float endRad = (float) Math.toRadians(-90f + (clickedAction + 1) * angleStep);
            RadialDrawingUtils.strokeArc(borderBuf, mat, shockWaveR, shockWaveR + 2, startRad, endRad, 2.0f, shockColor);
        } else if (isClosing && clickedAction == -2) {
            float shockWaveR = CENTER_R + (clickProgress * 40f);
            int shockAlpha = (int)((1.0f - clickProgress) * 200);
            int shockColor = (0x00FFFFFF & COLOR_BORDER) | (shockAlpha << 24);
            RadialDrawingUtils.strokeRing(borderBuf, mat, shockWaveR, shockWaveR + 2, 0, (float)(2 * Math.PI), shockColor);
        }

        BufferRenderer.drawWithGlobalProgram(borderBuf.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        matrices.pop();

        renderLabels(ctx, n, angleStep, currentActive);

        if (!isClosing && hoveredSlice >= 0 && hoveredSlice < n) {
            renderTooltip(ctx, mouseX, mouseY);
        }
    }

    private void renderLabels(DrawContext ctx, int n, float angleStep, ModConfig.MiningShape currentActive) {
        for (int i = 0; i < n; i++) {
            ModConfig.MiningShape shape = activeShapes.get(i);
            boolean isAct = (currentActive == shape);
            float mid = (float) Math.toRadians(-90f + (i + 0.5f) * angleStep);

            float hovP = sliceHoverProgress[i];
            int baseLabelR = (INNER_R + OUTER_R) / 2;

            // Lấy theo tỷ lệ mở để chữ bay ra cùng với khung
            float sliceT = MathHelper.clamp((animOpen - (i * 0.05f)) * 1.5f, 0f, 1f);
            float slicePop = 1.0f - (float)Math.pow(1.0f - sliceT, 3);

            float labelR = baseLabelR * slicePop;

            // HIỆU ỨNG 4: Magnetic cursor pull (kéo icon/text nhẹ ra ngoài góc khi hover)
            float pull = hovP * 6.0f;
            float dx = (float)Math.cos(mid) * pull;
            float dy = (float)Math.sin(mid) * pull;

            int lx = cx + (int)(Math.cos(mid) * labelR + dx);
            int ly = cy + (int)(Math.sin(mid) * labelR + dy);

            // Xử lý alpha để fade in text
            int alpha = (int)(slicePop * 255);
            if (alpha < 10) continue;

            int iconColor = applyAlpha(isAct ? ThemeColors.TOGGLE_ON_TEXT : ThemeColors.BTN_TEXT, alpha);
            int textColor = applyAlpha(isAct ? ThemeColors.TOGGLE_ON_TEXT : ThemeColors.TEXT_LABEL, alpha);

            String icon = shape.icon;
            ctx.drawTextWithShadow(textRenderer, icon, lx - textRenderer.getWidth(icon) / 2, ly - 10, iconColor);

            String name = shortenLabel(shape.label);
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
        String fullName = activeShapes.get(hoveredSlice).label;
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

    private void executeClickAction() {
        if (clickedAction == -2) {
            client.setScreen(new SettingsScreen(this));
        } else if (clickedAction >= 0) {
            ConfigManager.get().miningShape = activeShapes.get(clickedAction);
            ConfigManager.save();
            client.setScreen(parent);
        }
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