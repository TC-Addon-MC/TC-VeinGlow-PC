package com.tcveinminer.client.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tcveinminer.client.config.ClientConfig;
import com.tcveinminer.client.config.ClientConfigManager;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.client.util.ThemeColors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class RadialMenuScreen extends Screen {

    /** Unified entry for both builtin enum shapes and custom equation shapes. */
    private record SliceEntry(String id, String icon, String label) {
    }

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

    // Keyboard navigation state
    private int keyboardSelectedIndex = -1;
    private long lastKeyPressTime = 0;
    private static final long KEY_REPEAT_DELAY = 100; // ms

    public RadialMenuScreen(Screen parent) {
        super(Component.literal("TC VeinMiner"));
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
        ensureHoverArray();
        keyboardSelectedIndex = -1;
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
        // Keep keyboard selection valid after rebuilding shapes; -1 means "not selected
        // yet"
        if (keyboardSelectedIndex >= activeShapes.size())
            keyboardSelectedIndex = -1;
        ensureHoverArray();
    }

    // Ensure sliceHoverProgress has room for all slices plus center
    private void ensureHoverArray() {
        int needed = activeShapes == null ? 1 : activeShapes.size() + 1;
        if (sliceHoverProgress == null || sliceHoverProgress.length < needed) {
            float[] next = new float[needed];
            if (sliceHoverProgress != null)
                System.arraycopy(sliceHoverProgress, 0, next, 0, Math.min(sliceHoverProgress.length, next.length));
            sliceHoverProgress = next;
        }
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {

        if (isClosing) {
            clickProgress += delta * 0.15f;
            if (clickProgress >= 1.0f) {
                executeClickAction();
                return;
            }
        } else {
            // Smoothly approach 1.0 but clamp when very close to avoid asymptotic behavior
            animOpen = Mth.lerp(delta * 0.3f, animOpen, 1.0f);
            if (1.0f - animOpen < 0.001f)
                animOpen = 1.0f;
            hoveredSlice = getHoveredSlice(mouseX, mouseY);
        }

        // Resolve currentShape as string ID (works for both builtin and custom)
        String currentActiveId = ClientConfigManager.instance.currentShape;
        if (currentActiveId == null || currentActiveId.isBlank())
            currentActiveId = ModConfig.MiningShape.FACE.name();

        PoseStack matrices = ctx.pose();
        matrices.pushPose();
        matrices.translate(cx, cy, 0);

        float t = animOpen;
        float scale = 1.0f + (float) (Math.sin(t * Math.PI * 3.5f) * (1.0f - t) * 0.15f);
        if (t < 0.99f) {
            scale *= (float) Math.pow(t, 0.5);
        }
        matrices.scale(scale, scale, 1.0f);

        Matrix4f mat = matrices.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull();

        int n = activeShapes.size();
        float angleStep = 360f / n;

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < n; i++) {
            boolean isHov = (!isClosing
                    && (hoveredSlice == i || (keyboardSelectedIndex >= 0 && keyboardSelectedIndex == i)));
            boolean isAct = activeShapes.get(i).id().equals(currentActiveId);

            sliceHoverProgress[i] = Mth.lerp(delta * 0.3f, sliceHoverProgress[i], isHov ? 1.0f : 0.0f);
            float hovP = sliceHoverProgress[i];

            float delay = i * 0.05f;
            float sliceT = Mth.clamp((animOpen - delay) * 1.5f, 0f, 1f);
            float slicePop = 1.0f - (float) Math.pow(1.0f - sliceT, 3);

            int color = isAct ? COLOR_SLICE_ACTIVE : lerpColor(COLOR_SLICE_NORMAL, COLOR_SLICE_HOVER, hovP);

            float startRad = (float) Math.toRadians(-90f + i * angleStep);
            float endRad = (float) Math.toRadians(-90f + (i + 1) * angleStep);

            float hovRadiusOff = hovP * 4.0f;
            float currentInnerR = INNER_R * slicePop;
            float currentOuterR = (OUTER_R + hovRadiusOff) * slicePop;

            if (hovP > 0.01f && !isClosing) {
                int glowAlpha = (int) (hovP * 80);
                int glowColor = (0x00FFFFFF & COLOR_SLICE_HOVER) | (glowAlpha << 24);
                RadialDrawingUtils.fillArc(buf, mat, currentInnerR - 2, currentOuterR + 8, startRad, endRad, glowColor);
            }

            if (isClosing && clickedAction == i) {
                float waveR = INNER_R + (clickProgress * (OUTER_R - INNER_R + 20));
                float splitR = Mth.clamp(waveR, currentInnerR, currentOuterR);

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
        sliceHoverProgress[centerIdx] = Mth.lerp(delta * 0.3f, sliceHoverProgress[centerIdx], centerHov ? 1.0f : 0.0f);
        int centerColor = lerpColor(COLOR_CENTER_NORMAL, COLOR_CENTER_HOVER, sliceHoverProgress[centerIdx]);

        float pulse = 1.0f + Mth.sin((float) (System.currentTimeMillis() / 200.0)) * 0.03f;
        float currentCenterR = CENTER_R * animOpen * pulse;

        RadialDrawingUtils.fillCircle(buf, mat, currentCenterR, centerColor);

        BufferUploader.drawWithShader(buf.buildOrThrow());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        matrices.popPose();

        renderLabels(ctx, n, angleStep, currentActiveId);

        if (!isClosing
                && (hoveredSlice >= 0 && hoveredSlice < n || keyboardSelectedIndex >= 0 && keyboardSelectedIndex < n)) {
            renderTooltip(ctx, mouseX, mouseY);
        }
    }

    private void executeClickAction() {

        if (clickedAction == -2) {
            // Open settings; pass parent as the parent screen to avoid navigation loop
            minecraft.setScreen(new MainMenuScreen(parent));

        } else if (clickedAction >= 0) {

            ClientConfigManager.instance.currentShape = activeShapes.get(clickedAction).id();

            ClientConfigManager.save();

            minecraft.setScreen(parent);
        }
    }

    private void renderLabels(GuiGraphics ctx, int n, float angleStep, String currentActiveId) {
        for (int i = 0; i < n; i++) {
            SliceEntry entry = activeShapes.get(i);
            boolean isAct = entry.id().equals(currentActiveId);
            float mid = (float) Math.toRadians(-90f + (i + 0.5f) * angleStep);

            float hovP = sliceHoverProgress[i];
            int baseLabelR = (INNER_R + OUTER_R) / 2;

            float sliceT = Mth.clamp((animOpen - (i * 0.05f)) * 1.5f, 0f, 1f);
            float slicePop = 1.0f - (float) Math.pow(1.0f - sliceT, 3);

            float labelR = baseLabelR * slicePop;

            float pull = hovP * 6.0f;
            float dx = (float) Math.cos(mid) * pull;
            float dy = (float) Math.sin(mid) * pull;

            int lx = cx + (int) (Math.cos(mid) * labelR + dx);
            int ly = cy + (int) (Math.sin(mid) * labelR + dy);

            int alpha = (int) (slicePop * 255);
            if (alpha < 10)
                continue;

            int iconColor = applyAlpha(isAct ? ThemeColors.TOGGLE_ON_TEXT : ThemeColors.BTN_TEXT, alpha);
            int textColor = applyAlpha(isAct ? ThemeColors.TOGGLE_ON_TEXT : ThemeColors.TEXT_LABEL, alpha);

            String icon = entry.icon();
            ctx.drawString(font, Component.literal(icon), lx - font.width(icon) / 2, ly - 10, iconColor, true);

            // Tính max width dựa trên vị trí label và khung màn hình
            int maxWidth = calculateMaxWidthForLabel(lx);
            String name = shortenLabel(entry.label(), maxWidth);
            ctx.drawString(font, Component.literal(name), lx - font.width(name) / 2, ly + 1, textColor, true);
        }

        int centerAlpha = (int) (animOpen * 255);
        if (centerAlpha > 10) {
            ctx.drawString(font, Component.literal("⚙"), cx - font.width("⚙") / 2, cy - 9,
                    applyAlpha(ThemeColors.BTN_TEXT, centerAlpha), true);
            ctx.drawString(font, Component.literal("Settings"), cx - font.width("Settings") / 2, cy + 1,
                    applyAlpha(ThemeColors.TEXT_LABEL, centerAlpha), true);
        }
    }

    private int applyAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (Mth.clamp(alpha, 0, 255) << 24);
    }

    private void renderTooltip(GuiGraphics ctx, int mouseX, int mouseY) {
        int idx = (keyboardSelectedIndex >= 0 && keyboardSelectedIndex < activeShapes.size())
                ? keyboardSelectedIndex
                : (hoveredSlice >= 0 && hoveredSlice < activeShapes.size() ? hoveredSlice : -1);

        if (idx < 0)
            return;

        String fullName = activeShapes.get(idx).label();
        int tw = font.width(fullName) + 8;
        ctx.fill(mouseX + 6, mouseY - 14, mouseX + 6 + tw, mouseY, 0xCC000000);
        ctx.drawString(font, Component.literal(fullName), mouseX + 10, mouseY - 11, 0xFFFFFFFF, true);
    }

    private int lerpColor(int c1, int c2, float p) {
        int a = (int) Mth.lerp(p, (c1 >> 24) & 0xFF, (c2 >> 24) & 0xFF);
        int r = (int) Mth.lerp(p, (c1 >> 16) & 0xFF, (c2 >> 16) & 0xFF);
        int g = (int) Mth.lerp(p, (c1 >> 8) & 0xFF, (c2 >> 8) & 0xFF);
        int b = (int) Mth.lerp(p, c1 & 0xFF, c2 & 0xFF);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        // Khi chuột di chuyển, reset keyboard selection để không có hai highlight cùng
        // lúc
        int h = getHoveredSlice((int) mouseX, (int) mouseY);
        if (h >= 0 || h == -2) {
            keyboardSelectedIndex = -1;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || isClosing)
            return false;

        int hovered = getHoveredSlice((int) mouseX, (int) mouseY);

        if (hovered == -2 || (hovered >= 0 && hovered < activeShapes.size())) {
            isClosing = true;
            clickedAction = hovered;
            return true;
        }

        minecraft.setScreen(parent);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC to close
        if (keyCode == 256) {
            minecraft.setScreen(parent);
            return true;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastKeyPressTime < KEY_REPEAT_DELAY) {
            return true;
        }

        int n = activeShapes.size();

        // Left/Right arrow to rotate (LEFT=263, RIGHT=262)
        if (keyCode == 263 || keyCode == 262) {
            if (keyboardSelectedIndex < 0)
                keyboardSelectedIndex = 0;
            else if (keyCode == 263)
                keyboardSelectedIndex = (keyboardSelectedIndex - 1 + n) % n;
            else
                keyboardSelectedIndex = (keyboardSelectedIndex + 1) % n;
            lastKeyPressTime = currentTime;
            return true;
        }

        // Number keys 1-9 to select directly (49=1 .. 57=9)
        if (keyCode >= 49 && keyCode <= 57) {
            int idx = keyCode - 49; // 0-based
            if (idx < n) {
                keyboardSelectedIndex = idx;
            }
            lastKeyPressTime = currentTime;
            return true;
        }

        // Up arrow to go to center (settings). UP=265
        if (keyCode == 265) {
            clickedAction = -2;
            isClosing = true;
            lastKeyPressTime = currentTime;
            return true;
        }

        // Down arrow or Enter to confirm current keyboard selection. DOWN=264,
        // ENTER=257
        if (keyCode == 264 || keyCode == 257) {
            if (keyboardSelectedIndex >= 0 && keyboardSelectedIndex < n) {
                clickedAction = keyboardSelectedIndex;
                isClosing = true;
            }
            lastKeyPressTime = currentTime;
            return true;
        }

        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int getHoveredSlice(int mx, int my) {
        float dx = mx - cx, dy = my - cy;
        float dist2 = dx * dx + dy * dy;
        if (dist2 < CENTER_R * CENTER_R)
            return -2;
        if (dist2 > (float) OUTER_R * OUTER_R)
            return -1;
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx)) + 90f;
        if (angle < 0)
            angle += 360f;
        return (int) (angle / (360f / activeShapes.size())) % activeShapes.size();
    }

    // Tính max width dựa trên chord width thực của sector tại vị trí label
    private int calculateMaxWidthForLabel(int labelX) {
        int n = activeShapes.size();
        if (n <= 0)
            return 40;
        // Góc của một slice (radian)
        float sliceAngle = (float) (2 * Math.PI / n);
        // Radius trung bình nơi đặt label
        float labelR = (INNER_R + OUTER_R) / 2f;
        // Chord width = 2 * r * sin(sliceAngle / 2), đây là chiều rộng thực của slice
        // tại radius đó
        float chordWidth = 2f * labelR * (float) Math.sin(sliceAngle / 2f);
        // Trừ padding mỗi bên
        int available = Math.max(20, (int) (chordWidth) - 8);
        return available;
    }

    // Check từng chữ cái, không check từ
    private String shortenLabel(String label, int maxWidth) {
        if (label == null || label.isEmpty())
            return "";

        // Nếu text vừa vặn, return nguyên bản
        if (font.width(label) <= maxWidth) {
            return label;
        }

        // Cắt từng chữ cái từ từ
        String result = label;
        while (font.width(result) > maxWidth && result.length() > 0) {
            result = result.substring(0, result.length() - 1);
        }

        // Thêm ".." nếu cắt ngắn
        if (result.length() < label.length()) {
            // Đảm bảo ".." vừa vặn trong max width
            while (font.width(result + "..") > maxWidth && result.length() > 0) {
                result = result.substring(0, result.length() - 1);
            }
            result = result + "..";
        }

        return result.isEmpty() ? ".." : result;
    }
}
