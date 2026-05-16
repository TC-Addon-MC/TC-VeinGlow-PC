package com.tcveinminer.gui;

import com.tcveinminer.util.SessionStats;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class StatsScreen extends BaseScreen {

    public StatsScreen(Screen parent) { super(parent, "Stats", 280, 205); }

    @Override
    protected void initWidgets() {
        addDrawableChild(ButtonWidget.builder(Text.literal("RESET STATS"), b -> SessionStats.reset())
            .dimensions(x+16, y+H-28, 95, 18).build());
        addDrawableChild(backBtn(x+W-110, y+H-28, 95, "QUAY LẠI"));
    }

    @Override
    protected void renderContent(DrawContext ctx, int mx, int my, float delta) {
        drawTitle(ctx, "📊 Thống Kê Phiên Này");
        ctx.fill(x+8, y+HEADER_H+6, x+W-8, y+HEADER_H+7, 0xFF2A2A4A);

        int ry = y+HEADER_H+14, step = 20;
        row(ctx, ry,        "🪨 Tổng block đã đào:",        n(SessionStats.getTotalBlocks()));
        row(ctx, ry+step,   "⛏  Lần vein mine kích hoạt:",  n(SessionStats.getActivations()));
        row(ctx, ry+step*2, "💎 Block hiếm nhất đào được:",  SessionStats.getRarestBlock());
        row(ctx, ry+step*3, "⏱  Thời gian bật mod:",        SessionStats.getUptimeFormatted());
        row(ctx, ry+step*4, "🔋 Durability đã tiêu tốn:",    n(SessionStats.getDurabilityUsed()));
    }

    private void row(DrawContext ctx, int ry, String l, String v) {
        label(ctx, l, x+14, ry);
        value(ctx, v, x+W-14-textRenderer.getWidth(v), ry);
    }
    private String n(long v) { return String.format("%,d", v); }
}
