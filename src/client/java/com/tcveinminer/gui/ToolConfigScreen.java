package com.tcveinminer.gui;

import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.util.*;

public class ToolConfigScreen extends BaseScreen {

    private static final String[][] TOOLS = {
        {"pickaxe","⛏ Pickaxe"}, {"axe","🪓 Axe"},
        {"shovel","🔪 Shovel"},   {"sword","🗡 Sword"},
        {"hand","✋ Tay trần"},   {"hoe","🪚 Hoe"},
    };
    private final Map<String, Boolean> state = new LinkedHashMap<>();
    private static final int CW=116, CH=40, GAP=8;

    public ToolConfigScreen(Screen parent) {
        super(parent, "Tool Config", 300, 220);
        ConfigManager.get().enabledTools.forEach(state::put); // Sửa ở đây
    }

    @Override
    protected void initWidgets() {
        int sx = x+(W-(CW*2+GAP))/2, sy = y+HEADER_H+8;
        for (int i=0; i<TOOLS.length; i++) {
            final String key = TOOLS[i][0];
            int cx=sx+(i%2)*(CW+GAP), cy=sy+(i/2)*(CH+6);
            addDrawableChild(ButtonWidget.builder(Text.empty(),
                b -> state.merge(key, false, (o,v)->!o))
                .dimensions(cx, cy, CW, CH).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("LƯU & QUAY LẠI"), b -> {
            ConfigManager.get().enabledTools.clear(); // Sửa ở đây
            ConfigManager.get().enabledTools.putAll(state); // Sửa ở đây
            ConfigManager.save(); // Sửa ở đây
            client.setScreen(parent);
        }).dimensions(x+W/2-70, y+H-26, 140, 18).build());
    }

    @Override
    protected void renderContent(DrawContext ctx, int mx, int my, float delta) {
        drawTitle(ctx, "🔧 Cấu Hình Công Cụ");
        int sx=x+(W-(CW*2+GAP))/2, sy=y+HEADER_H+8;
        for (int i=0; i<TOOLS.length; i++) {
            String key=TOOLS[i][0], lbl=TOOLS[i][1];
            boolean on=state.getOrDefault(key, false);
            int cx=sx+(i%2)*(CW+GAP), cy=sy+(i/2)*(CH+6);
            Draw.toggle(ctx, cx, cy, CW, CH, on);
            ctx.drawTextWithShadow(textRenderer, lbl,           cx+16, cy+8,  on?TC.ON_TEXT:TC.OFF_TEXT);
            ctx.drawTextWithShadow(textRenderer, on?"✓ BẬT":"✗ TẮT", cx+16, cy+22, on?TC.ON_TEXT:TC.OFF_TEXT);
        }
    }
}
