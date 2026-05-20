package com.tcveinminer.gui.screens;

import com.tcveinminer.gui.CustomButton;
import com.tcveinminer.util.DrawHelper;
import com.tcveinminer.util.LayoutUtil;
import com.tcveinminer.util.PanelDrawUtil;
import com.tcveinminer.util.ThemeColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class MainMenuScreen extends Screen {

    private static final int W = LayoutUtil.SCREEN_W_MD;
    private static final int H = 210;

    private final Screen parent;
    private int x, y;

    public MainMenuScreen(Screen parent) {
        super(Text.literal("TC-VeinMiner"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        x = (width  - W) / 2;
        y = (height - H) / 2;

        int navY1 = y + LayoutUtil.HEADER_H + 30;
        int navY2 = navY1 + 48;
        int btnW  = 120;
        int col1  = x + 20;
        int col2  = x + W / 2 + 10;

        addDrawableChild(new CustomButton(col1, navY1, btnW, LayoutUtil.BTN_H_LG,
                Text.literal("⚙ CÀI ĐẶT"),
                btn -> client.setScreen(new SettingsScreen(this))));

        addDrawableChild(new CustomButton(col2, navY1, btnW, LayoutUtil.BTN_H_LG,
                Text.literal("📋 BLOCK LIST"),
                btn -> client.setScreen(new BlockListScreen(this))));

        addDrawableChild(new CustomButton(col1, navY2, btnW, LayoutUtil.BTN_H_LG,
                Text.literal("🔧 TOOL CONFIG"),
                btn -> client.setScreen(new ToolConfigScreen(this))));

        addDrawableChild(new CustomButton(col2, navY2, btnW, LayoutUtil.BTN_H_LG,
                Text.literal("📊 STATS"),
                btn -> client.setScreen(new StatsScreen(this))));

        addDrawableChild(new CustomButton(
                LayoutUtil.centerX(x, W, LayoutUtil.BTN_W_CLOSE),
                LayoutUtil.footerY(y, H),
                LayoutUtil.BTN_W_CLOSE, LayoutUtil.BTN_H,
                Text.literal("ĐÓNG"),
                btn -> client.setScreen(parent)));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        PanelDrawUtil.panel(ctx, x, y, W, H);
        DrawHelper.drawHeader(ctx, x, y, W, LayoutUtil.HEADER_H);

        ctx.drawTextWithShadow(textRenderer, "⛏ TC-VeinMiner",
                x + LayoutUtil.HEADER_PAD_X, y + LayoutUtil.HEADER_PAD_Y,
                ThemeColors.TEXT_TITLE);

        // Version badge
        String ver = "v1.0";
        int verW = textRenderer.getWidth(ver);
        PanelDrawUtil.tag(ctx, x + W - verW - 30, y + 6, verW + 12, 14, 0);
        ctx.drawTextWithShadow(textRenderer, ver,
                x + W - verW - 24, y + 9, ThemeColors.TEXT_VALUE);

        // Close X
        int xBtnX = x + W - 16;
        boolean xHov = mouseX >= xBtnX && mouseX <= xBtnX + 12
                    && mouseY >= y + 6  && mouseY <= y + 20;
        ctx.drawTextWithShadow(textRenderer, "✕", xBtnX, y + LayoutUtil.HEADER_PAD_Y,
                xHov ? ThemeColors.TEXT_ERROR : ThemeColors.TEXT_LABEL);

        // Subtitle
        String hint = "Giữ phím [V] để kích hoạt vein mining";
        ctx.drawTextWithShadow(textRenderer, hint,
                LayoutUtil.centerX(x, W, textRenderer.getWidth(hint)),
                y + LayoutUtil.HEADER_H + 10,
                ThemeColors.TEXT_LABEL);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int xBtnX = x + W - 16;
        if (mouseX >= xBtnX && mouseX <= xBtnX + 12
                && mouseY >= y + 6 && mouseY <= y + 20) {
            client.setScreen(parent);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext ctx, int mx, int my, float delta) {}
}
