package com.tcveinminer.gui;

import com.tcveinminer.config.ModConfig;
import com.tcveinminer.hud.SessionStats;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class MainMenuScreen extends BaseScreen {

    public MainMenuScreen(Screen parent) { super(parent, "TC-VeinMiner", 320, 235); }

    @Override
    protected void initWidgets() {
        // Big toggle
        addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
            boolean now = !ModConfig.get().enabled;
            ModConfig.get().enabled = now;
            if (now) SessionStats.onModEnabled(); else SessionStats.onModDisabled();
            ModConfig.save();
        }).dimensions(x+30, y+HEADER_H+14, W-60, 22).build());

        // Radio buttons
        addDrawableChild(ButtonWidget.builder(Text.literal("Giữ phím [V]"), b -> {
            ModConfig.get().holdMode=true; ModConfig.save();
        }).dimensions(x+30, y+HEADER_H+50, 115, 16).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Bật hẳn"), b -> {
            ModConfig.get().holdMode=false; ModConfig.save();
        }).dimensions(x+160, y+HEADER_H+50, 115, 16).build());

        // 4 nav buttons (2×2)
        int ny1=y+HEADER_H+82, ny2=ny1+50;
        int c1=x+20, c2=x+W/2+10, bw=W/2-30, bh=40;

        addDrawableChild(ButtonWidget.builder(Text.literal("⚙  CÀI ĐẶT"),
            b -> client.setScreen(new SettingsScreen(this))).dimensions(c1,ny1,bw,bh).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("📋 BLOCK LIST"),
            b -> client.setScreen(new BlockListScreen(this))).dimensions(c2,ny1,bw,bh).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("🔧 TOOL CONFIG"),
            b -> client.setScreen(new ToolConfigScreen(this))).dimensions(c1,ny2,bw,bh).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("📊 STATS"),
            b -> client.setScreen(new StatsScreen(this))).dimensions(c2,ny2,bw,bh).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("ĐÓNG"),
            b -> client.setScreen(parent)).dimensions(x+W/2-45, y+H-28, 90, 18).build());
    }

    @Override
    protected void renderContent(DrawContext ctx, int mx, int my, float delta) {
        // Header title + version
        drawTitle(ctx, "⛏ TC-VeinMiner");
        String ver="v1.0"; int vw=textRenderer.getWidth(ver)+10;
        Draw.btn(ctx, x+W-vw-22, y+6, vw+4, 13, false);
        ctx.drawTextWithShadow(textRenderer, ver, x+W-vw-19, y+9, TC.BTN_TEXT);

        // ✕
        boolean xh=mx>=x+W-15&&mx<=x+W-7&&my>=y+6&&my<=y+18;
        ctx.drawTextWithShadow(textRenderer,"✕",x+W-14,y+8, xh?TC.TXT_ERROR:TC.TXT_LABEL);

        // Big toggle card (vẽ sau header trước widget)
        boolean on=ModConfig.get().enabled;
        Draw.toggle(ctx, x+30, y+HEADER_H+14, W-60, 22, on);
        String tl = on ? "● VEIN MINER: BẬT" : "○ VEIN MINER: TẮT";
        ctx.drawTextWithShadow(textRenderer, tl,
            x+30+(W-60-textRenderer.getWidth(tl))/2, y+HEADER_H+21,
            on ? TC.ON_TEXT : TC.OFF_TEXT);

        // Mode label
        label(ctx,"Chế độ hoạt động:", x+14, y+HEADER_H+54);

        // Radio dots
        boolean hold=ModConfig.get().holdMode;
        ctx.fill(x+20, y+HEADER_H+55, x+27, y+HEADER_H+62, hold  ? TC.ON_BORDER : TC.TXT_LABEL);
        ctx.fill(x+150,y+HEADER_H+55, x+157,y+HEADER_H+62, !hold ? TC.ON_BORDER : TC.TXT_LABEL);

        // Divider
        ctx.fill(x+10, y+HEADER_H+77, x+W-10, y+HEADER_H+78, 0xFF2A2A4A);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (mx>=x+W-15&&mx<=x+W-7&&my>=y+6&&my<=y+18) { client.setScreen(parent); return true; }
        return super.mouseClicked(mx,my,btn);
    }
}
