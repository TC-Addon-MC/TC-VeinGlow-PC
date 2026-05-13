package com.tcveinminer.gui;

import com.tcveinminer.gui.screens.SettingsScreen;
import com.tcveinminer.gui.screens.BlockListScreen;
import com.tcveinminer.gui.screens.ToolConfigScreen;
import com.tcveinminer.gui.screens.StatsScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * @deprecated Dùng com.tcveinminer.gui.screens.MainMenuScreen thay thế.
 * File này chỉ giữ lại để tránh break nếu có reference cũ.
 */
@Deprecated
public class MainMenuScreen extends BaseScreen {

    public MainMenuScreen(Screen parent) { super(parent, "TC-VeinMiner", 320, 235); }

    @Override
    protected void initWidgets() {
        int ny1 = y + HEADER_H + 30, ny2 = ny1 + 50;
        int c1 = x + 20, c2 = x + W / 2 + 10, bw = W / 2 - 30, bh = 40;

        addDrawableChild(new CustomButton(c1, ny1, bw, bh, Text.literal("⚙  CÀI ĐẶT"),
            b -> client.setScreen(new SettingsScreen(this))));
        addDrawableChild(new CustomButton(c2, ny1, bw, bh, Text.literal("📋 BLOCK LIST"),
            b -> client.setScreen(new BlockListScreen(this))));
        addDrawableChild(new CustomButton(c1, ny2, bw, bh, Text.literal("🔧 TOOL CONFIG"),
            b -> client.setScreen(new ToolConfigScreen(this))));
        addDrawableChild(new CustomButton(c2, ny2, bw, bh, Text.literal("📊 STATS"),
            b -> client.setScreen(new StatsScreen(this))));
        addDrawableChild(new CustomButton(x + W / 2 - 45, y + H - 28, 90, 18, Text.literal("ĐÓNG"),
            b -> client.setScreen(parent)));
    }

    @Override
    protected void renderContent(DrawContext ctx, int mx, int my, float delta) {
        drawTitle(ctx, "⛏ TC-VeinMiner");
        String hint = "Giữ phím [V] để kích hoạt";
        ctx.drawTextWithShadow(textRenderer, hint,
            x + W / 2 - textRenderer.getWidth(hint) / 2,
            y + HEADER_H + 10, TC.TXT_LABEL);
    }
}
