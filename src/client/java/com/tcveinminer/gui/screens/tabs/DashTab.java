package com.tcveinminer.gui.screens.tabs;

import com.tcveinminer.config.ModConfig;
import com.tcveinminer.gui.screens.MainMenuScreen;
import com.tcveinminer.config.ClientConfigManager;
import com.tcveinminer.gui.widgets.MaxBlockSlider;
import com.tcveinminer.util.DrawHelper;
import com.tcveinminer.util.ThemeColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class DashTab implements MenuTab {

    private String getActivationText(int mode) {
        return switch (mode) {
            case 0 -> "Đè nút sẽ đào (Mặc định)";
            case 1 -> "Đè nút + sneak";
            case 2 -> "Nhấn nút thì nó sẽ bật";
            case 3 -> "Nhấn nút bật + sneak";
            default -> "Không xác định";
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
    public void render(DrawContext ctx, MainMenuScreen screen, int cx, int cy, int cw, int ch, int mouseX, int mouseY, float delta) {
        int gap = 10;
        int boxH = (ch - gap) / 2;

        // ==========================================
        // BOX 1: TRẠNG THÁI HOẠT ĐỘNG HIỆN TẠI
        // ==========================================
        DrawHelper.drawCard(ctx, cx, cy, cw, boxH);
        ctx.drawTextWithShadow(screen.getTextRenderer(), "TRẠNG THÁI HOẠT ĐỘNG HIỆN TẠI", cx + 12, cy + 10, 0xFFA0AEC0);

        int innerW = (cw - 34) / 2;
        int innerY = cy + 24;
        int innerH = boxH - 32; // Trừ lề trên và lề dưới an toàn hơn

        if (innerH > 15) {
            // Cột trái
            DrawHelper.drawCard(ctx, cx + 12, innerY, innerW, innerH);
            ctx.drawTextWithShadow(screen.getTextRenderer(), "CHẾ ĐỘ ĐÀO", cx + 20, innerY + 6, 0xFF6B7280);
            String label = screen.getState().selectedShapeId;
            try { label = ModConfig.MiningShape.valueOf(screen.getState().selectedShapeId).label; } catch (Exception ignored) {}
            ctx.drawTextWithShadow(screen.getTextRenderer(), label, cx + 20, innerY + 18, 0xFFFFFFFF);

            // Cột phải
            int rightX = cx + 12 + innerW + 10;
            DrawHelper.drawCard(ctx, rightX, innerY, innerW, innerH);
            ctx.drawTextWithShadow(screen.getTextRenderer(), "CƠ CHẾ KÍCH HOẠT", rightX + 8, innerY + 6, 0xFF6B7280);
            String actText = getActivationText(screen.getState().activationMode);
            ctx.drawTextWithShadow(screen.getTextRenderer(), actText, rightX + 8, innerY + 18, ThemeColors.GOLD);
        }

        // ==========================================
        // BOX 2: GIỚI HẠN KHỐI (MAX BLOCKS)
        // ==========================================
        int box2Y = cy + boxH + gap;
        DrawHelper.drawCard(ctx, cx, box2Y, cw, boxH);

        ctx.drawTextWithShadow(screen.getTextRenderer(), "GIỚI HẠN KHỐI TỐI ĐA (MAX BLOCKS)", cx + 12, box2Y + 10, 0xFFFFFFFF);

        String lim = screen.getState().maxBlocks + " / " + ClientConfigManager.instance.serverMaxBlocks + " Khối";
        int limW = screen.getTextRenderer().getWidth(lim);
        ctx.drawTextWithShadow(screen.getTextRenderer(), lim, cx + cw - 12 - limW, box2Y + 10, ThemeColors.GOLD);

        // Đặt dòng chú thích bám theo vị trí Slider, cách slider 24px trở xuống
        int sliderY = box2Y + 24;
        int textY = sliderY + 24;

        String footerNote = "Giới hạn số lượng khối tối đa một lần đào (Thắt chặt theo giới hạn từ Server).";
        // Giới hạn chiều cao cho text bọc dòng, tránh tràn boxH
        ctx.drawTextWrapped(screen.getTextRenderer(), Text.literal(footerNote), cx + 12, textY, cw - 24, 0xFF4B5563);
    }
}
