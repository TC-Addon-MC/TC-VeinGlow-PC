package com.tcveinminer.gui.screens;

import com.tcveinminer.gui.CustomButton;
import com.tcveinminer.util.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import com.tcveinminer.util.SessionStats;

public class StatsScreen extends Screen {

    private static final int W = LayoutUtil.SCREEN_W_SM;
    private static final int H = 220;

    private final Screen parent;
    private int x, y;

    public StatsScreen(Screen parent) {
        super(Text.literal("Stats"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        x = (width  - W) / 2;
        y = (height - H) / 2;

        addDrawableChild(new CustomButton(
                x + LayoutUtil.CONTENT_PAD_X,
                LayoutUtil.footerY(y, H),
                100, LayoutUtil.BTN_H,
                Text.literal("RESET STATS"),
                btn -> SessionStats.reset()));

        addDrawableChild(new CustomButton(
                x + W - 120,
                LayoutUtil.footerY(y, H),
                100, LayoutUtil.BTN_H,
                Text.literal("QUAY LẠI"),
                btn -> client.setScreen(parent)));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        PanelDrawUtil.panel(ctx, x, y, W, H);
        DrawHelper.drawHeader(ctx, x, y, W, LayoutUtil.HEADER_H);

        ctx.drawTextWithShadow(textRenderer, "📊 Thống Kê Phiên Này",
                x + LayoutUtil.HEADER_PAD_X, y + LayoutUtil.HEADER_PAD_Y,
                ThemeColors.TEXT_TITLE);

        // Section card
        int cardY = y + LayoutUtil.HEADER_H + 8;
        PanelDrawUtil.card(ctx, x + 8, cardY, W - 16, H - LayoutUtil.HEADER_H - 48);

        int rowY = cardY + LayoutUtil.CONTENT_PAD_Y;
        drawStatRow(ctx, rowY,                           "🪨 Block đã đào:",        String.format("%,d", SessionStats.getTotalBlocks()));
        drawStatRow(ctx, rowY + LayoutUtil.ROW_STEP_SM,  "⛏  Lần kích hoạt:",       String.format("%,d", SessionStats.getActivations()));
        drawStatRow(ctx, rowY + LayoutUtil.ROW_STEP_SM*2,"💎 Block hiếm nhất:",      SessionStats.getRarestBlock());
        drawStatRow(ctx, rowY + LayoutUtil.ROW_STEP_SM*3,"⏱  Uptime mod:",           SessionStats.getUptimeFormatted());
        drawStatRow(ctx, rowY + LayoutUtil.ROW_STEP_SM*4,"🔋 Durability tiêu tốn:",  String.format("%,d", SessionStats.getDurabilityUsed()));

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawStatRow(DrawContext ctx, int rowY, String label, String value) {
        ctx.drawTextWithShadow(textRenderer, label,
                x + LayoutUtil.CONTENT_PAD_X + 4, rowY, ThemeColors.TEXT_LABEL);
        ctx.drawTextWithShadow(textRenderer, value,
                x + W - LayoutUtil.CONTENT_PAD_X - 4 - textRenderer.getWidth(value),
                rowY, ThemeColors.TEXT_VALUE);
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext ctx, int mx, int my, float delta) {}
}
