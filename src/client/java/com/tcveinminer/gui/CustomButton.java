package com.tcveinminer.gui;

import com.tcveinminer.util.ButtonDrawUtil;
import com.tcveinminer.util.ThemeColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class CustomButton extends ButtonWidget {

    public CustomButton(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ButtonDrawUtil.drawPrimary(ctx, getX(), getY(), getWidth(), getHeight(),
                isHovered(), false);

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int color = isHovered() ? ThemeColors.GOLD : ThemeColors.TEXT_LABEL;
        int tw    = tr.getWidth(getMessage());
        ctx.drawTextWithShadow(tr, getMessage(),
                getX() + (getWidth()  - tw) / 2,
                getY() + (getHeight() - 8)  / 2,
                color);
    }
}
