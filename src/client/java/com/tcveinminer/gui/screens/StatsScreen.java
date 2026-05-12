package com.tcveinminer.gui.screens;

import com.tcveinminer.util.DrawHelper;
import com.tcveinminer.util.SessionStats;
import com.tcveinminer.util.ThemeColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class StatsScreen extends Screen {

    private static final int W = 280, H = 220;
    private static final int HEADER_H = 24;

    private final Screen parent;
    private int x, y;

    public StatsScreen(Screen parent) {
        super(Text.literal("Stats"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        x = (width - W) / 2;
        y = (height - H) / 2;

        // Reset button
        addDrawableChild(ButtonWidget.builder(Text.literal("RESET STATS"), btn -> {
            SessionStats.reset();
        }).dimensions(x + 20, y + H - 36, 100, 18).build());

        // Back button
        addDrawableChild(ButtonWidget.builder(Text.literal("QUAY LẠI"), btn -> {
            client.setScreen(parent);
        }).dimensions(x + W - 120, y + H - 36, 100, 18).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        DrawHelper.drawPanel(ctx, x, y, W, H);
        DrawHelper.drawHeader(ctx, x, y, W, HEADER_H);

        // Title
        ctx.drawTextWithShadow(textRenderer, "📊 Thống Kê Phiên Này",
            x + 12, y + 8, ThemeColors.TEXT_TITLE);

        // Divider
        ctx.fill(x + 2, y + HEADER_H + 6, x + W - 2, y + HEADER_H + 7, ThemeColors.BORDER_INNER);

        int rowY = y + HEADER_H + 14;
        int rowStep = 18;

        drawStatRow(ctx, rowY,              "🪨 Tổng block đã đào:",      String.format("%,d", SessionStats.getTotalBlocks()));
        drawStatRow(ctx, rowY + rowStep,    "⛏  Lần vein mine kích hoạt:", String.format("%,d", SessionStats.getActivations()));
        drawStatRow(ctx, rowY + rowStep*2,  "💎 Block hiếm nhất đào được:", SessionStats.getRarestBlock());
        drawStatRow(ctx, rowY + rowStep*3,  "⏱  Thời gian bật mod:",       SessionStats.getUptimeFormatted());
        drawStatRow(ctx, rowY + rowStep*4,  "🔋 Durability đã tiêu tốn:",  String.format("%,d", SessionStats.getDurabilityUsed()));

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawStatRow(DrawContext ctx, int rowY, String label, String value) {
        ctx.drawTextWithShadow(textRenderer, label, x + 14, rowY, ThemeColors.TEXT_LABEL);
        ctx.drawTextWithShadow(textRenderer, value, x + W - 14 - textRenderer.getWidth(value), rowY, ThemeColors.TEXT_VALUE);
    }

    @Override
    public boolean shouldPause() { return false; }
}
