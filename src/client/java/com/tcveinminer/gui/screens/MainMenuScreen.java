package com.tcveinminer.gui.screens;

import com.tcveinminer.gui.CustomButton;
import com.tcveinminer.util.DrawHelper;
import com.tcveinminer.util.ThemeColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class MainMenuScreen extends Screen {

    private static final int W = 320, H = 210;
    private static final int HEADER_H = 26;

    private final Screen parent;
    private int x, y;

    public MainMenuScreen(Screen parent) {
        super(Text.literal("TC-VeinMiner"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        x = (width - W) / 2;
        y = (height - H) / 2;

        // Ghi chú hướng dẫn sẽ vẽ trong render(), không dùng nút

        // 4 nút điều hướng (lưới 2x2)
        int navY1 = y + HEADER_H + 30;
        int navY2 = navY1 + 48;
        int btnW = 120, btnH = 38;
        int col1 = x + 20, col2 = x + W / 2 + 10;

        addDrawableChild(new CustomButton(col1, navY1, btnW, btnH,
                Text.literal("⚙ CÀI ĐẶT"),
                btn -> client.setScreen(new SettingsScreen(this))));

        addDrawableChild(new CustomButton(col2, navY1, btnW, btnH,
                Text.literal("📋 BLOCK LIST"),
                btn -> client.setScreen(new BlockListScreen(this))));

        addDrawableChild(new CustomButton(col1, navY2, btnW, btnH,
                Text.literal("🔧 TOOL CONFIG"),
                btn -> client.setScreen(new ToolConfigScreen(this))));

        addDrawableChild(new CustomButton(col2, navY2, btnW, btnH,
                Text.literal("📊 STATS"),
                btn -> client.setScreen(new StatsScreen(this))));

        // Nút Đóng
        addDrawableChild(new CustomButton(x + W / 2 - 45, y + H - 28, 90, 18,
                Text.literal("ĐÓNG"),
                btn -> client.setScreen(parent)));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);

        DrawHelper.drawPanel(ctx, x, y, W, H);
        DrawHelper.drawHeader(ctx, x, y, W, HEADER_H);

        // Tiêu đề và phiên bản
        ctx.drawTextWithShadow(textRenderer, "⛏ TC-VeinMiner", x + 12, y + 8, ThemeColors.TEXT_TITLE);
        String ver = "v1.0";
        int verW = textRenderer.getWidth(ver);
        ctx.fill(x + W - verW - 30, y + 6, x + W - 18, y + 20, ThemeColors.BTN_BG);
        DrawHelper.drawSolidBorder(ctx, x + W - verW - 30, y + 6, verW + 12, 14, ThemeColors.BTN_BORDER);
        ctx.drawTextWithShadow(textRenderer, ver, x + W - verW - 24, y + 9, ThemeColors.BTN_TEXT);

        // Nút X để đóng
        int xBtnX = x + W - 16;
        boolean xHov = mouseX >= xBtnX && mouseX <= xBtnX + 12 && mouseY >= y + 6 && mouseY <= y + 20;
        ctx.drawTextWithShadow(textRenderer, "✕", xBtnX, y + 8, xHov ? ThemeColors.TEXT_ERROR : ThemeColors.TEXT_LABEL);

        // Hướng dẫn: giữ V để kích hoạt
        ctx.drawTextWithShadow(textRenderer,
                "Giữ phím [V] để kích hoạt vein mining",
                x + W / 2 - textRenderer.getWidth("Giữ phím [V] để kích hoạt vein mining") / 2,
                y + HEADER_H + 10,
                ThemeColors.TEXT_LABEL);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int xBtnX = x + W - 16;
        if (mouseX >= xBtnX && mouseX <= xBtnX + 12 && mouseY >= y + 6 && mouseY <= y + 20) {
            client.setScreen(parent);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() { return false; }
}
