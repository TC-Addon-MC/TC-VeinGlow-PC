package com.tcveinminer.client.gui.widgets;

import com.tcveinminer.client.gui.screens.MenuState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

/**
 * RGBSlider — Thanh trượt chọn màu kênh R / G / B.
 *
 * Cải tiến từ bản cũ:
 *  - Gradient nền theo kênh (đen → đỏ / xanh lá / xanh lam)
 *  - Handle hình chữ nhật có viền trắng, shadow tối
 *  - Disabled: nền xám mờ, handle bị khoá
 */
public class RGBSlider extends SliderWidget {

    private final MenuState state;
    private final char channel; // 'R', 'G', 'B'

    public RGBSlider(int x, int y, int w, int h, char channel, MenuState state,
                     double initValue, boolean disabled) {
        super(x, y, w, h,
                Component.literal(String.valueOf((int) (initValue * 255))),
                initValue);
        this.state   = state;
        this.channel = channel;
        this.active  = !disabled;
    }

    @Override
    protected void updateMessage() {
        int v = (int) (value * 255);
        switch (channel) {
            case 'R' -> state.colorR = v;
            case 'G' -> state.colorG = v;
            case 'B' -> state.colorB = v;
        }
        setMessage(Component.literal(String.valueOf(v)));
    }

    @Override
    protected void applyValue() {
        updateMessage();
    }

    // ─────────────────────────────────────────────────────────────
    // Custom rendering — gradient kênh màu
    // ─────────────────────────────────────────────────────────────

    @Override
    public void renderWidget(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();

        if (!active) {
            // Disabled: nền xám mờ
            ctx.fill(x, y, x + w, y + h, 0x44334155);
            ctx.fill(x, y, x + w, y + 1, 0xFF334155);
            ctx.fill(x, y + h - 1, x + w, y + h, 0xFF334155);
            ctx.fill(x, y, x + 1, y + h, 0xFF334155);
            ctx.fill(x + w - 1, y, x + w, y + h, 0xFF334155);
            // Text "N/A"
            return;
        }

        // Nền gradient theo kênh: đen → màu kênh
        int endColor = switch (channel) {
            case 'R' -> 0xFFFF3030;
            case 'G' -> 0xFF30D060;
            default  -> 0xFF4080FF; // B
        };
        ctx.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF101010, endColor);

        // Viền mỏng xung quanh
        int borderColor = switch (channel) {
            case 'R' -> 0xFF7F1D1D;
            case 'G' -> 0xFF14532D;
            default  -> 0xFF1E3A5F;
        };
        ctx.fill(x, y, x + w, y + 1, borderColor);
        ctx.fill(x, y + h - 1, x + w, y + h, borderColor);
        ctx.fill(x, y, x + 1, y + h, borderColor);
        ctx.fill(x + w - 1, y, x + w, y + h, borderColor);

        // Handle (thanh kéo trắng)
        int hx = x + 1 + (int)(value * (w - 8));
        hx = Math.max(x + 1, Math.min(hx, x + w - 7));

        // Shadow handle
        ctx.fill(hx + 1, y + 1, hx + 7, y + h, 0x66000000);
        // Thân handle
        ctx.fill(hx, y + 1, hx + 6, y + h - 1, 0xFFFFFFFF);
        // Inner handle tối (tạo cảm giác 3D)
        ctx.fill(hx + 1, y + 2, hx + 5, y + h - 2, 0xFFD0D8E8);
    }
}
