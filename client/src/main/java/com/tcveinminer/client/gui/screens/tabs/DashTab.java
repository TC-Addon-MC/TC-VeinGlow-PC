package com.tcveinminer.client.gui.screens.tabs;

import com.tcveinminer.client.gui.screens.MainMenuScreen;
import com.tcveinminer.client.config.ClientConfigManager;
import com.tcveinminer.client.gui.widgets.MaxBlockSlider;
import com.tcveinminer.client.util.DrawHelper;
import com.tcveinminer.client.util.ThemeColors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class DashTab implements MenuTab {

    private String getActivationText(int mode) {
        return switch (mode) {
            case 1 -> Component.translatable("gui.tcveinminer.activation.hold").getString();
            case 2 -> Component.translatable("gui.tcveinminer.activation.hold_sneak").getString();
            case 3 -> Component.translatable("gui.tcveinminer.activation.toggle").getString();
            case 4 -> Component.translatable("gui.tcveinminer.activation.toggle_sneak").getString();
            default -> Component.translatable("gui.tcveinminer.activation.unknown").getString();
        };
    }

    @Override
    public void init(MainMenuScreen screen, int cx, int cy, int cw, int ch) {
        int gap = 10;
        int boxH = (ch - gap) / 2;
        int box2Y = cy + boxH + gap;

        int sliderY = box2Y + 24;
        screen.addUIElement(new MaxBlockSlider(cx + 12, sliderY, cw - 24, 20, screen.getState()));
    }

    @Override
    public void render(GuiGraphics ctx, MainMenuScreen screen, int cx, int cy, int cw, int ch, int mouseX, int mouseY, float delta) {
        int gap = 10;
        int boxH = (ch - gap) / 2;

        // ==========================================
        // BOX 1: TRẠNG THÁI HOẠT ĐỘNG HIỆN TẠI
        // ==========================================
        DrawHelper.drawCard(ctx, cx, cy, cw, boxH);
        ctx.drawString(screen.getMinecraft().font, Component.translatable("gui.tcveinminer.dash.status_title").getString(), cx + 12, cy + 10, 0xFFA0AEC0);

        int innerW = (cw - 34) / 2;
        int innerY = cy + 24;
        int innerH = boxH - 32; // Trừ lề trên và lề dưới an toàn hơn

        if (innerH > 15) {
            // Cột trái
            DrawHelper.drawCard(ctx, cx + 12, innerY, innerW, innerH);
            ctx.drawString(screen.getMinecraft().font, Component.translatable("gui.tcveinminer.dash.shape_mode").getString(), cx + 20, innerY + 6, 0xFF6B7280);
            String label = screen.getState().selectedShapeId;
            try { label = Component.translatable("tc_veinminer.mode." + screen.getState().selectedShapeId).getString(); } catch (Exception ignored) {}
            ctx.drawString(screen.getMinecraft().font, label, cx + 20, innerY + 18, 0xFFFFFFFF);

            // Cột phải
            int rightX = cx + 12 + innerW + 10;
            DrawHelper.drawCard(ctx, rightX, innerY, innerW, innerH);
            ctx.drawString(screen.getMinecraft().font, Component.translatable("gui.tcveinminer.dash.activation_mode").getString(), rightX + 8, innerY + 6, 0xFF6B7280);
            String actText = getActivationText(screen.getState().activationMode);
            ctx.drawString(screen.getMinecraft().font, actText, rightX + 8, innerY + 18, ThemeColors.GOLD);
        }

        // ==========================================
        // BOX 2: GIỚI HẠN KHỐI (MAX BLOCKS)
        // ==========================================
        int box2Y = cy + boxH + gap;
        DrawHelper.drawCard(ctx, cx, box2Y, cw, boxH);

        ctx.drawString(screen.getMinecraft().font, Component.translatable("gui.tcveinminer.dash.max_blocks_title").getString(), cx + 12, box2Y + 10, 0xFFFFFFFF);

        String lim = screen.getState().maxBlocks + " / " + ClientConfigManager.instance.serverMaxBlocks + " " + Component.translatable("gui.tcveinminer.dash.blocks_unit").getString();
        int limW = screen.getMinecraft().font.width(lim);
        ctx.drawString(screen.getMinecraft().font, lim, cx + cw - 12 - limW, box2Y + 10, ThemeColors.GOLD);

        // Đặt dòng chú thích bám theo vị trí Slider, cách slider 24px trở xuống
        int sliderY = box2Y + 24;
        int textY = sliderY + 24;

        String footerNote = Component.translatable("gui.tcveinminer.dash.max_blocks_desc").getString();
        // Giới hạn chiều cao cho text bọc dòng, tránh tràn boxH
        ctx.drawWordWrap(screen.getMinecraft().font, Component.literal(footerNote), cx + 12, textY, cw - 24, 0xFF4B5563);
    }
}
