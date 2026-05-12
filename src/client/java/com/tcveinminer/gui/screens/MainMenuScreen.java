package com.tcveinminer.gui.screens;

import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.util.DrawHelper;
import com.tcveinminer.util.ThemeColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class MainMenuScreen extends Screen {

    private static final int W = 320, H = 230;
    private static final int HEADER_H = 26;

    private final Screen parent;
    private int x, y;

    // Radio buttons (chế độ kích hoạt)
    private static final int RADIO_HOLD   = 0;
    private static final int RADIO_TOGGLE = 1;
    private int selectedRadio;

    public MainMenuScreen(Screen parent) {
        super(Text.literal("TC-VeinMiner"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        x = (width - W) / 2;
        y = (height - H) / 2;

        // Cập nhật: Sử dụng biến boolean holdMode từ ModConfig
        selectedRadio = ConfigManager.get().holdMode ? RADIO_HOLD : RADIO_TOGGLE;

        // Nút Bật/Tắt chính
        addDrawableChild(ButtonWidget.builder(
                Text.literal(ConfigManager.get().enabled ? "● VEIN MINER: BẬT" : "○ VEIN MINER: TẮT"),
                btn -> {
                    ConfigManager.get().enabled = !ConfigManager.get().enabled;
                    btn.setMessage(Text.literal(ConfigManager.get().enabled
                            ? "● VEIN MINER: BẬT" : "○ VEIN MINER: TẮT"));
                    ConfigManager.save();
                }
        ).dimensions(x + 30, y + HEADER_H + 12, W - 60, 22).build());

        // Radio: Giữ phím V
        addDrawableChild(ButtonWidget.builder(Text.literal("Giữ phím [V]"), btn -> {
            selectedRadio = RADIO_HOLD;
            ConfigManager.get().holdMode = true; // Chuyển sang chế độ Hold
            ConfigManager.save();
        }).dimensions(x + 30, y + HEADER_H + 46, 110, 16).build());

        // Radio: Bật hẳn (Toggle)
        addDrawableChild(ButtonWidget.builder(Text.literal("Bật hẳn"), btn -> {
            selectedRadio = RADIO_TOGGLE;
            ConfigManager.get().holdMode = false; // Chuyển sang chế độ Toggle
            ConfigManager.save();
        }).dimensions(x + 160, y + HEADER_H + 46, 100, 16).build());

        // 4 nút điều hướng (lưới 2x2)
        int navY1 = y + HEADER_H + 76;
        int navY2 = navY1 + 48;
        int btnW = 120, btnH = 38;
        int col1 = x + 20, col2 = x + W / 2 + 10;

        addDrawableChild(ButtonWidget.builder(Text.literal("⚙ CÀI ĐẶT"), btn ->
                client.setScreen(new SettingsScreen(this))
        ).dimensions(col1, navY1, btnW, btnH).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("📋 BLOCK LIST"), btn ->
                client.setScreen(new BlockListScreen(this))
        ).dimensions(col2, navY1, btnW, btnH).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("🔧 TOOL CONFIG"), btn ->
                client.setScreen(new ToolConfigScreen(this))
        ).dimensions(col1, navY2, btnW, btnH).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("📊 STATS"), btn ->
                client.setScreen(new StatsScreen(this))
        ).dimensions(col2, navY2, btnW, btnH).build());

        // Nút Đóng
        addDrawableChild(ButtonWidget.builder(Text.literal("ĐÓNG"), btn ->
                client.setScreen(parent)
        ).dimensions(x + W / 2 - 45, y + H - 28, 90, 18).build());
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

        // Nhãn chế độ
        ctx.drawTextWithShadow(textRenderer, "Chế độ hoạt động:",
                x + 14, y + HEADER_H + 50, ThemeColors.TEXT_LABEL);

        // Hiển thị lựa chọn Radio
        int radioHoldX  = x + 30;
        int radioToggleX= x + 160;
        int radioY      = y + HEADER_H + 46;

        ctx.fill(radioHoldX - 10, radioY + 5, radioHoldX - 4, radioY + 11,
                selectedRadio == RADIO_HOLD ? ThemeColors.TOGGLE_ON_DOT : ThemeColors.TEXT_LABEL);
        ctx.fill(radioToggleX - 10, radioY + 5, radioToggleX - 4, radioY + 11,
                selectedRadio == RADIO_TOGGLE ? ThemeColors.TOGGLE_ON_DOT : ThemeColors.TEXT_LABEL);

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