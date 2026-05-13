package com.tcveinminer.gui.screens;

import com.tcveinminer.config.*;
import com.tcveinminer.gui.radial.*;
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

        activeShapes = new ArrayList<>();
        ModConfig cfg = ConfigManager.get();

        for (ModConfig.MiningShape s : ModConfig.MiningShape.values()) {
            if (cfg.enabledShapes.contains(s.name())) activeShapes.add(s);
        }

        if (activeShapes.isEmpty()) activeShapes.add(ModConfig.MiningShape.FACE);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {

        hoveredSlice = RadialMath.getHoveredIndex(
                mx, my, cx, cy,
                CENTER_R, OUTER_R,
                activeShapes.size()
        );

        int n = activeShapes.size();
        float step = RadialMath.angleStep(n);

        Matrix4f mat = ctx.getMatrices().peek().getPositionMatrix();
        VertexConsumer buf = ctx.getVertexConsumers().getBuffer(RenderLayer.getGuiOverlay());

        for (int i = 0; i < n; i++) {

            boolean active = ConfigManager.get().miningShape == activeShapes.get(i);

            int color =
                    active ? 0xCC1B4332 :
                            hoveredSlice == i ? 0xCC2A2A5A :
                                    0xAA1A1A3A;

            RadialRenderer.drawSegment(
                    mat, buf,
                    cx, cy,
                    INNER_R, OUTER_R,
                    -90f + i * step,
                    -90f + (i + 1) * step,
                    SEGMENTS,
                    color
            );
        }

        // center
        RadialRenderer.drawSegment(
                mat, buf,
                cx, cy,
                0f, CENTER_R,
                0f, 360f,
                SEGMENTS,
                hoveredSlice == -2 ? 0xCC2A2A52 : 0xCC12122A
        );

        ctx.draw();

        renderLabels(ctx, n, step);
    }

    private void renderLabels(DrawContext ctx, int n, float step) {
        for (int i = 0; i < n; i++) {

            ModConfig.MiningShape shape = activeShapes.get(i);

            float mid = RadialMath.midAngle(i, step);
            int[] pos = RadialMath.centerPoint(cx, cy, (INNER_R + OUTER_R) / 2f, mid);

            boolean active = ConfigManager.get().miningShape == shape;

            ctx.drawTextWithShadow(textRenderer,
                    shape.icon,
                    pos[0] - textRenderer.getWidth(shape.icon) / 2,
                    pos[1] - 10,
                    active ? ThemeColors.TOGGLE_ON_TEXT : ThemeColors.BTN_TEXT
            );

            String label = shape.label.length() > 5
                    ? shape.label.substring(0, 5) + ".."
                    : shape.label;

            ctx.drawTextWithShadow(textRenderer,
                    label,
                    pos[0] - textRenderer.getWidth(label) / 2,
                    pos[1] + 1,
                    active ? ThemeColors.TOGGLE_ON_TEXT : ThemeColors.TEXT_LABEL
            );
        }

        ctx.drawTextWithShadow(textRenderer, "⚙",
                cx - textRenderer.getWidth("⚙") / 2,
                cy - 9,
                ThemeColors.BTN_TEXT);

        ctx.drawTextWithShadow(textRenderer, "Settings",
                cx - textRenderer.getWidth("Settings") / 2,
                cy + 1,
                ThemeColors.TEXT_LABEL);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {

        int hovered = RadialMath.getHoveredIndex(
                (int) mx, (int) my,
                cx, cy,
                CENTER_R, OUTER_R,
                activeShapes.size()
        );

        if (hovered == -2) {
            client.setScreen(new SettingsScreen(this));
            return true;
        }

        if (hovered >= 0) {
            ConfigManager.get().miningShape = activeShapes.get(hovered);
            ConfigManager.save();
        }

        client.setScreen(parent);
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}