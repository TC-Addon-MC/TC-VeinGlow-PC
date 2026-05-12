package com.tcveinminer.gui;

import com.tcveinminer.util.DrawHelper;
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
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Tự vẽ nền tùy chỉnh bằng DrawHelper của bạn
        DrawHelper.drawButton(context, this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.isHovered(), false);

        // Lấy TextRenderer từ MinecraftClient
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        // Vẽ Text ra giữa nút
        int color = this.isHovered() ? ThemeColors.BTN_TEXT : 0xFFAAAAAA;
        int textWidth = textRenderer.getWidth(this.getMessage());

        context.drawTextWithShadow(textRenderer, this.getMessage(),
                this.getX() + (this.getWidth() - textWidth) / 2,
                this.getY() + (this.getHeight() - 8) / 2, color);
    }
}