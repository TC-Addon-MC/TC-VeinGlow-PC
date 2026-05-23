package com.tcveinminer.gui.screens;

import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.gui.CustomButton;
import com.tcveinminer.util.DrawHelper;
import com.tcveinminer.util.ToggleDrawUtil;
import com.tcveinminer.util.ThemeColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.*;
import com.tcveinminer.config.ClientConfigManager;
public class ToolConfigScreen extends Screen {

    private static final int W = 300, H = 230;
    private static final int HEADER_H = 24;

    private static final String[][] TOOLS = {
            {"pickaxe", "Pickaxe"},
            {"axe",     "🪓 Axe"},
            {"shovel",  "🔪 Shovel"},
            {"sword",   "🗡 Sword"},
            {"hand",    "✋ Tay trần"},
            {"hoe",     "🪚 Hoe"},
    };

    private final Screen parent;
    private final Map<String, Boolean> toolState = new LinkedHashMap<>();
    private int x, y;

    private static final int CARD_W = 120, CARD_H = 44, CARD_GAP = 10;

    public ToolConfigScreen(Screen parent) {
        super(Text.literal("Tool Config"));
        this.parent = parent;
        ClientConfigManager.instance.enabledTools.forEach(toolState::put);
    }

    @Override
    protected void init() {
        x = (width - W) / 2;
        y = (height - H) / 2;

        int startX = x + (W - (CARD_W * 2 + CARD_GAP)) / 2;
        int startY = y + HEADER_H + 10;

        for (int i = 0; i < TOOLS.length; i++) {
            String toolKey = TOOLS[i][0];
            int col = i % 2;
            int row = i / 2;
            int cx = startX + col * (CARD_W + CARD_GAP);
            int cy = startY + row * (CARD_H + 8);

            addDrawableChild(new CustomButton(cx, cy, CARD_W, CARD_H, Text.empty(), btn -> {
                toolState.merge(toolKey, false, (old, v) -> !old);
            }));
        }

        addDrawableChild(new CustomButton(x + W / 2 - 80, y + H - 34, 160, 18,
                Text.literal("LƯU & QUAY LẠI"), btn -> {
            ClientConfigManager.instance.enabledTools.clear();
            ClientConfigManager.instance.enabledTools.putAll(toolState);
            ClientConfigManager.save();
            client.setScreen(parent);
        }));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        DrawHelper.drawPanel(ctx, x, y, W, H);
        DrawHelper.drawHeader(ctx, x, y, W, HEADER_H);
        ctx.drawTextWithShadow(textRenderer, "Cấu Hình Công Cụ", x + 12, y + 8, ThemeColors.TEXT_TITLE);

        int startX = x + (W - (CARD_W * 2 + CARD_GAP)) / 2;
        int startY = y + HEADER_H + 10;

        for (int i = 0; i < TOOLS.length; i++) {
            String toolKey = TOOLS[i][0];
            String label   = TOOLS[i][1];
            boolean on     = toolState.getOrDefault(toolKey, false);

            int col = i % 2;
            int row = i / 2;
            int cx = startX + col * (CARD_W + CARD_GAP);
            int cy = startY + row * (CARD_H + 8);

            // Dùng ToggleDrawUtil thay DrawHelper.drawToggleButton
            ToggleDrawUtil.draw(ctx, cx, cy, CARD_W, CARD_H, on);

            int textColor = on ? ThemeColors.EMERALD_TEXT : ThemeColors.REDSTONE_TEXT;
            ctx.drawTextWithShadow(textRenderer, label,    cx + 16, cy + 10, textColor);
            ctx.drawTextWithShadow(textRenderer, on ? "BẬT" : "TẮT", cx + 16, cy + 24, textColor);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override public boolean shouldPause() { return false; }
}