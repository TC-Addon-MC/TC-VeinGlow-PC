package com.tcveinminer.client.gui.widgets;

import com.tcveinminer.client.gui.screens.MenuState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

/**
 * AlphaSlider — Thanh trượt điều chỉnh độ trong suốt của outline.
 * Hiển thị gradient từ trong suốt → màu outline hiện tại.
 * Phạm vi: 0 (ẩn hoàn toàn) → 255 (không trong suốt).
 */
public class AlphaSlider extends AbstractSliderButton {

    private final MenuState state;

    public AlphaSlider(int x, int y, int w, int h, MenuState state) {
        super(x, y, w, h,
                Component.literal(String.valueOf(state.outlineAlpha)),
                state.outlineAlpha / 255.0);
        this.state = state;
    }

    @Override
    protected void updateMessage() {
        int v = (int) (value * 255);
        state.outlineAlpha = v;
        setMessage(Component.literal(String.valueOf(v)));
    }

    @Override
    protected void applyValue() {
        updateMessage();
    }

    /** Vẽ gradient nền checkered (ô vuông cờ vua) để thể hiện độ trong suốt. */
    @Override
    public void renderWidget(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        // Vẽ nền checker (2 màu xen kẽ) để biểu diễn alpha
        int bx = getX() + 1, by = getY() + 1;
        int bw = getWidth() - 2, bh = getHeight() - 2;
        int cell = 4;
        for (int cx2 = 0; cx2 < bw; cx2 += cell) {
            for (int cy2 = 0; cy2 < bh; cy2 += cell) {
                boolean dark = ((cx2 / cell) + (cy2 / cell)) % 2 == 0;
                int c = dark ? 0xFF666666 : 0xFF999999;
                ctx.fill(bx + cx2, by + cy2,
                        Math.min(bx + cx2 + cell, bx + bw),
                        Math.min(by + cy2 + cell, by + bh), c);
            }
        }

        // Gradient: trong suốt → màu outline hiện tại
        int r = state.colorR, g = state.colorG, b = state.colorB;
        ctx.fillGradient(bx, by, bx + bw, by + bh,
                0x00000000, 0xFF000000 | (r << 16) | (g << 8) | b);

        // Đường viền slider
        ctx.fill(getX(), getY(), getX() + getWidth(), getY() + 1, 0xFF334155);
        ctx.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), 0xFF334155);
        ctx.fill(getX(), getY(), getX() + 1, getY() + getHeight(), 0xFF334155);
        ctx.fill(getX() + getWidth() - 1, getY(), getX() + getWidth(), getY() + getHeight(), 0xFF334155);

        // Handle (thanh kéo)
        int hx = getX() + (int) (value * (getWidth() - 8));
        ctx.fill(hx, getY(), hx + 8, getY() + getHeight(), 0xFFFFFFFF);
        ctx.fill(hx + 1, getY() + 1, hx + 7, getY() + getHeight() - 1, 0xFF94A3B8);
    }
}
