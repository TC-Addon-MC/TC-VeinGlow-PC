package com.tcveinminer.client.gui;

import com.tcveinminer.client.util.ButtonDrawUtil;
import com.tcveinminer.client.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class CustomButton extends Button {

    private boolean selected = false;
    private float selectProgress = 0f;
    private long lastRenderTime = 0;

    public CustomButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
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
    protected void renderWidget(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        if (lastRenderTime == 0)
            lastRenderTime = now;
        float dt = (now - lastRenderTime) / 1000f;
        lastRenderTime = now;

        if (selected && selectProgress < 1f)
            selectProgress = Math.min(1f, selectProgress + dt * 8f);
        else if (!selected && selectProgress > 0f)
            selectProgress = Math.max(0f, selectProgress - dt * 8f);

        ButtonDrawUtil.drawPrimary(ctx, getX(), getY(), getWidth(), getHeight(), isHovered(), selectProgress);

        Font tr = Minecraft.getInstance().font;

        int color;
        String msg = getMessage().getString().toLowerCase();
        if (msg.equals("on") || msg.equals("bật")) {
            color = isHovered() ? ThemeColors.EMERALD_TEXT : ThemeColors.EMERALD;
        } else if (msg.equals("off") || msg.equals("tắt")) {
            color = isHovered() ? ThemeColors.REDSTONE_TEXT : ThemeColors.REDSTONE;
        } else if (isSelected()) {
            color = ThemeColors.TEXT_SELECTED;
        } else if (isHovered()) {
            color = 0xFFFFFFFF;
        } else {
            color = 0xFFCDD8E8;
        }

        int tw = tr.width(getMessage());
        ctx.drawString(tr, getMessage(),
                getX() + (getWidth() - tw) / 2,
                getY() + (getHeight() - 8) / 2,
                color);
    }
}