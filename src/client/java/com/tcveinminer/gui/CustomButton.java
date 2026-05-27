package com.tcveinminer.gui;

import com.tcveinminer.util.ButtonDrawUtil;
import com.tcveinminer.util.ThemeColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class CustomButton extends ButtonWidget {

    private boolean selected = false;
    private float selectProgress = 0f;
    private long lastRenderTime = 0;

    public CustomButton(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setSelectedInstant(boolean selected) {
        this.selected = selected;
        this.selectProgress = selected ? 1f : 0f;
    }

    public boolean isSelected() {
        return selected;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        if (lastRenderTime == 0) lastRenderTime = now;
        float dt = (now - lastRenderTime) / 1000f;
        lastRenderTime = now;

        if (selected && selectProgress < 1f) selectProgress = Math.min(1f, selectProgress + dt * 8f);
        else if (!selected && selectProgress > 0f) selectProgress = Math.max(0f, selectProgress - dt * 8f);

        ButtonDrawUtil.drawPrimary(ctx, getX(), getY(), getWidth(), getHeight(), isHovered(), selectProgress);

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;

        // Chữ trắng sáng khi selected/hover để nổi bật trên nền tối
        int color;
        String msg = getMessage().getString().toLowerCase();
        if (msg.equals("on") || msg.equals("bật")) {
            // Xanh lá nổi bật
            color = isHovered() ? ThemeColors.EMERALD_TEXT : ThemeColors.EMERALD;
        } else if (msg.equals("off") || msg.equals("tắt")) {
            // Đỏ nhạt
            color = isHovered() ? ThemeColors.REDSTONE_TEXT : ThemeColors.REDSTONE;
        } else if (isSelected()) {
            color = ThemeColors.TEXT_SELECTED; // vàng sáng
        } else if (isHovered()) {
            color = 0xFFFFFFFF; // trắng tinh khi hover
        } else {
            color = 0xFFCDD8E8; // trắng xanh nhạt — dễ đọc trên nền navy tối
        }

        int tw = tr.getWidth(getMessage());
        ctx.drawTextWithShadow(tr, getMessage(),
                getX() + (getWidth()  - tw) / 2,
                getY() + (getHeight() - 8)  / 2,
                color);
    }
}