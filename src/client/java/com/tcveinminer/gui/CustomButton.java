package com.tcveinminer.gui;

import com.tcveinminer.util.ButtonDrawUtil;
import com.tcveinminer.util.ThemeColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class CustomButton extends ButtonWidget {

    private boolean selected = false; // Thêm trạng thái được chọn

    public CustomButton(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    // Hàm mới để thiết lập trạng thái được chọn
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    // Hàm mới để kiểm tra trạng thái được chọn
    public boolean isSelected() {
        return selected;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Sử dụng ButtonDrawUtil.drawPrimary được cải tiến với trạng thái được chọn
        ButtonDrawUtil.drawPrimary(ctx, getX(), getY(), getWidth(), getHeight(),
                isHovered(), isSelected());

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        // Cải thiện màu văn bản dựa trên trạng thái (isSelected > isHovered > default)
        int color = isSelected() ? ThemeColors.TEXT_SELECTED : isHovered() ? ThemeColors.TEXT_HOVER : ThemeColors.TEXT_LABEL;
        int tw    = tr.getWidth(getMessage());
        ctx.drawTextWithShadow(tr, getMessage(),
                getX() + (getWidth()  - tw) / 2,
                getY() + (getHeight() - 8)  / 2,
                color);
    }
}