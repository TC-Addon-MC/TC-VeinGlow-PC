package com.tcveinminer.gui.screens.tabs;

import com.tcveinminer.gui.CustomButton;
import com.tcveinminer.gui.screens.MainMenuScreen;
import com.tcveinminer.gui.widgets.AmberButton;
import com.tcveinminer.engine.strategy.FilterModeManager;
import com.tcveinminer.util.DrawHelper;
import com.tcveinminer.util.ThemeColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class FilterTab implements MenuTab {
    private TextFieldWidget blockInput;
    private int blScroll = 0;
    private int searchScroll = 0;
    private String blError = "";
    private long errorTime = 0; // Thêm biến để ghi thời điểm phát sinh lỗi để control typing animation

    private int lastCx, lastCy, lastCw, lastCh;

    @Override
    public void init(MainMenuScreen screen, int cx, int cy, int cw, int ch) {
        int splitY = cy + 10;
        int inputW = (cw / 2) - 60;

        if (blockInput == null) {
            blockInput = new TextFieldWidget(screen.getTextRenderer(), cx + 6, splitY + 22, inputW, 16, Text.empty());
            blockInput.setMaxLength(100);
            FilterModeManager.updateBlockSearch("");
            blockInput.setChangedListener(text -> {
                FilterModeManager.updateBlockSearch(text);
                searchScroll = 0;
                blError = "";
                errorTime = 0; // reset khi người dùng nhập mới
            });
        } else {
            blockInput.setX(cx + 6); blockInput.setY(splitY + 22); blockInput.setWidth(inputW);
        }
        screen.addUIElement(blockInput);

        screen.addUIElement(new AmberButton(cx + (cw / 2) - 50, splitY + 22, 44, 16, Text.translatable("gui.tcveinminer.button.add"), btn -> {
            addBlock(screen);
        }));
    }

    private void addBlock(MainMenuScreen screen) {
        String input = blockInput.getText();
        Identifier validId = FilterModeManager.validateAndParseBlock(input);

        if (validId == null) {
            blError = Text.translatable("gui.tcveinminer.error.unknown_block").getString();
            errorTime = System.currentTimeMillis();
            return;
        }

        if (screen.getState().blacklist.contains(validId)) {
            blError = Text.translatable("gui.tcveinminer.error.duplicate_block").getString();
            errorTime = System.currentTimeMillis();
            return;
        }

        screen.getState().blacklist.add(validId);
        blockInput.setText("");
        FilterModeManager.updateBlockSearch("");
        blError = "";
    }

    @Override
    public void render(DrawContext ctx, MainMenuScreen screen, int cx, int cy, int cw, int ch, int mouseX, int mouseY, float delta) {
        this.lastCx = cx; this.lastCy = cy; this.lastCw = cw; this.lastCh = ch;

        int splitY = cy + 10;
        int splitH = ch - 20;
        int listY = splitY + 44;
        int maxItems = Math.max(1, (splitY + splitH - 4 - listY) / 20);

        int halfW = (cw - 10) / 2;
        int rightX = cx + halfW + 10;

        // VẼ KHUNG GỢI Ý TĨNH (BÊN TRÁI)
        DrawHelper.drawCard(ctx, cx, splitY, halfW, splitH);
        ctx.drawTextWithShadow(screen.getTextRenderer(), Text.translatable("gui.tcveinminer.filter.suggestion_title").getString(), cx + 8, splitY + 8, 0xFFFFFFFF);
        if (blockInput.getText().isEmpty()) {
            ctx.drawTextWithShadow(screen.getTextRenderer(), Text.translatable("gui.tcveinminer.filter.search_prompt").getString(), cx + 8, listY + 5, 0xFF6B7280);
        }

        // VẼ KHUNG BLACKLIST TĨNH (BÊN PHẢI)
        DrawHelper.drawCard(ctx, rightX, splitY, halfW, splitH);
        String rightTitle = Text.translatable("gui.tcveinminer.filter.blacklist_title").getString() + " (" + screen.getState().blacklist.size() + ")";
        ctx.drawTextWithShadow(screen.getTextRenderer(), rightTitle, rightX + 8, splitY + 8, 0xFFFFFFFF);
        if (!blError.isEmpty()) {
            long elapsed = (errorTime == 0) ? 0 : (System.currentTimeMillis() - errorTime);
            int charsToShow = (int) (elapsed / 40); // mỗi 40ms hiển thị thêm 1 ký tự
            charsToShow = Math.max(0, Math.min(blError.length(), charsToShow));
            String animatedError = blError.substring(0, charsToShow);

            // Giữ nguyên width(blError) gốc để vị trí chữ không bị dịch chuyển khi gõ
            int textX = rightX + halfW - 8 - screen.getTextRenderer().getWidth(blError);
            ctx.drawTextWithShadow(screen.getTextRenderer(), animatedError, textX, splitY + 8, ThemeColors.TEXT_ERROR);
        }

        List<Identifier> bl = new ArrayList<>(screen.getState().blacklist);
        if (bl.isEmpty()) {
            ctx.drawTextWithShadow(screen.getTextRenderer(), Text.translatable("gui.tcveinminer.filter.empty").getString(), rightX + 8, listY + 6, 0xFF6B7280);
        } else {
            int startBl = Math.max(0, Math.min(blScroll, bl.size() - maxItems));
            for (int i = startBl; i < Math.min(bl.size(), startBl + maxItems); i++) {
                int ry = listY + (i - startBl) * 20;
                Identifier id = bl.get(i);
                boolean hovered = mouseX >= rightX + 4 && mouseX <= rightX + halfW - 4 && mouseY >= ry && mouseY < ry + 20;
                if (hovered) {
                    ctx.fill(rightX + 4, ry, rightX + halfW - 4, ry + 20, 0x4DFF0000);
                }
                ctx.drawTextWithShadow(screen.getTextRenderer(), id.toString(), rightX + 8, ry + 5, hovered ? 0xFFFFFFFF : 0xFFFF7171);
            }
        }
    }

    // HÀM VẼ OVERLAY LỚP TRÊN CÙNG (Được gọi từ MainMenuScreen)
    public void renderOverlay(DrawContext ctx, MainMenuScreen screen, int mouseX, int mouseY) {
        int px = screen.getPx();
        int py = screen.getPy();
        int W = screen.getW();
        int H = screen.getH();

        int splitY = py + 70;
        int splitH = H - 70;

        // 1. HIỂN THỊ DANH SÁCH GỢI Ý (BÊN NGOÀI LỀ TRÁI - TỰ CO GIÃN THEO MÀN HÌNH)
        List<Identifier> suggestions = FilterModeManager.getBlockSearchCache();
        boolean hasQuery = blockInput != null && !blockInput.getText().isEmpty();
        if (hasQuery) {
            // Tự động tính toán chiều rộng dựa trên khoảng trống lề trái, tối đa là 150px
            int ovW = Math.min(150, px - 10);
            if (ovW < 50) ovW = Math.max(50, px - 4); // Cố gắng tận dụng không gian hẹp

            int ovX = Math.max(2, px - ovW - 4); // Đảm bảo không bị lút ra ngoài rìa trái màn hình (tối thiểu là cách rìa 2px)
            int ovItemH = 16;
            int ovPad = 6;

            // Giới hạn chiều cao hiển thị dựa theo đáy màn hình game để tránh tràn khung dưới
            int ovMaxVisible = Math.max(4, (screen.height - (splitY + 22) - 25) / ovItemH);
            int visibleCount = Math.min(suggestions.size(), ovMaxVisible);
            int ovH = ovPad * 2 + 12 + Math.max(1, visibleCount) * ovItemH;
            int ovY = splitY + 22;

            ctx.fill(ovX, ovY, ovX + ovW, ovY + ovH, 0xFF12121A);
            DrawHelper.drawSolidBorder(ctx, ovX, ovY, ovW, ovH, 0xFF6366F1);
            ctx.drawTextWithShadow(screen.getTextRenderer(), Text.translatable("gui.tcveinminer.filter.suggestion_title").getString() + " (" + suggestions.size() + ")", ovX + 6, ovY + 4, 0xFF6366F1);

            if (suggestions.isEmpty()) {
                ctx.drawTextWithShadow(screen.getTextRenderer(), Text.translatable("gui.tcveinminer.filter.empty_dots").getString(), ovX + 6, ovY + ovPad + 4, 0xFF6B7280);
            } else {
                int startIdx = Math.max(0, Math.min(searchScroll, suggestions.size() - visibleCount));
                for (int i = 0; i < visibleCount; i++) {
                    int idx = startIdx + i;
                    if (idx >= suggestions.size()) break;
                    Identifier id = suggestions.get(idx);
                    int ry = ovY + ovPad + 12 + (i * ovItemH);

                    boolean hov = mouseX >= ovX + 2 && mouseX <= ovX + ovW - 2 && mouseY >= ry && mouseY < ry + ovItemH;
                    if (hov) ctx.fill(ovX + 2, ry, ovX + ovW - 2, ry + ovItemH, 0x336366F1);

                    String renderTxt = id.toString();
                    int maxTextW = ovW - 12; // Chiều rộng tối đa chữ được phép chiếm

                    if (screen.getTextRenderer().getWidth(renderTxt) > maxTextW) {
                        renderTxt = "..." + id.getPath(); // Bỏ namespace, thay bằng ...

                        // Nếu sau khi đổi thành "..." + path mà vẫn dài hơn ô, tiến hành cắt đuôi
                        if (screen.getTextRenderer().getWidth(renderTxt) > maxTextW) {
                            renderTxt = screen.getTextRenderer().trimToWidth(renderTxt, maxTextW - 10) + "...";
                        }
                    }
                    ctx.drawTextWithShadow(screen.getTextRenderer(), renderTxt, ovX + 6, ry + (ovItemH - 8) / 2, hov ? 0xFFFFFFFF : 0xFFA0AEC0);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(MainMenuScreen screen, double mx, double my, int btn) {
        int px = screen.getPx();
        int splitY = lastCy + 10;
        int splitH = lastCh - 10;
        int listY = splitY + 44;
        int halfW = (lastCw - 10) / 2;
        int maxItems = Math.max(1, (splitY + splitH - 4 - listY) / 20);
        int rightX = lastCx + halfW + 10;

        // CLICK KHUNG GỢI Ý PHỤ NGOÀI LỀ TRÁI (Cập nhật đồng bộ theo tọa độ co giãn mới)
        List<Identifier> suggestions = FilterModeManager.getBlockSearchCache();
        boolean hasQuery = blockInput != null && !blockInput.getText().isEmpty();
        if (hasQuery && !suggestions.isEmpty()) {
            int ovW = Math.min(150, px - 10);
            if (ovW < 50) ovW = Math.max(50, px - 4);
            int ovX = Math.max(2, px - ovW - 4);
            int ovItemH = 16;
            int ovPad = 6;
            int ovMaxVisible = Math.max(4, (screen.height - (splitY + 22) - 25) / ovItemH);
            int visibleCount = Math.min(suggestions.size(), ovMaxVisible);
            int ovH = ovPad * 2 + 12 + Math.max(1, visibleCount) * ovItemH;
            int ovY = splitY + 22;
            int startIdx = Math.max(0, Math.min(searchScroll, suggestions.size() - visibleCount));

            if (mx >= ovX + 2 && mx <= ovX + ovW - 2 && my >= ovY + ovPad + 12 && my < ovY + ovH) {
                int i = (int)((my - (ovY + ovPad + 12)) / ovItemH);
                int idx = startIdx + i;
                if (idx >= 0 && idx < suggestions.size()) {
                    blockInput.setText(suggestions.get(idx).toString());
                    addBlock(screen);
                    return true;
                }
            }
        }

        // CLICK XÓA ITEM BLACKLIST
        List<Identifier> bl = new ArrayList<>(screen.getState().blacklist);
        if (!bl.isEmpty() && mx >= rightX + 4 && mx <= rightX + halfW - 4 && my >= listY && my < listY + maxItems * 20) {
            int startBl = Math.max(0, Math.min(blScroll, bl.size() - maxItems));
            int clickedRow = (int) ((my - listY) / 20);
            int index = startBl + clickedRow;

            if (index >= startBl && index < Math.min(bl.size(), startBl + maxItems)) {
                screen.getState().blacklist.remove(bl.get(index));
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(MainMenuScreen screen, double mx, double my, double h, double v) {
        int px = screen.getPx();
        int splitY = lastCy + 10;

        // CUỘN TRONG BẢNG GỢI Ý (Bên lề trái)
        boolean hasQuery = blockInput != null && !blockInput.getText().isEmpty();
        if (hasQuery) {
            List<Identifier> suggestions = FilterModeManager.getBlockSearchCache(); // Thêm dòng này để hết lỗi
            int ovW = Math.min(150, px - 10);
            if (ovW < 50) ovW = Math.max(50, px - 4);
            int ovX = Math.max(2, px - ovW - 4);
            int ovItemH = 16;
            int ovPad = 6;
            int ovMaxVisible = Math.max(4, (screen.height - (splitY + 22) - 25) / ovItemH);
            int ovH = ovPad * 2 + 12 + Math.min(suggestions.size(), ovMaxVisible) * ovItemH;
            int ovY = splitY + 22;
            if (mx >= ovX && mx <= ovX + ovW && my >= ovY && my <= ovY + ovH) {
                int amt = (int) v;
                if (amt == 0 && v != 0) amt = v > 0 ? 1 : -1;
                searchScroll = Math.max(0, searchScroll - amt);
                return true;
            }
        }

        // CUỘN TRONG BLACKLIST
        int amt = (int) (v * 2);
        if (amt == 0 && v != 0) amt = v > 0 ? 1 : -1;
        blScroll = Math.max(0, blScroll - amt);
        return true;
    }
}