package com.tcveinminer.gui.screens.tabs;

import com.tcveinminer.gui.CustomButton;
import com.tcveinminer.gui.screens.MainMenuScreen;
import com.tcveinminer.gui.widgets.AmberButton;
import com.tcveinminer.util.BlockFilterManager;
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

    // BIẾN LƯU TỌA ĐỘ (Cần thiết để click chuột không bị lệch)
    private int lastCx, lastCy, lastCw, lastCh;

    @Override
    public void init(MainMenuScreen screen, int cx, int cy, int cw, int ch) {
        int btnW = (cw - 21) / 4;
        String[][] tools = {
                {"all", "TẤT CẢ"}, {"hand", "Tay"}, {"item", "Vật"}, {"pickaxe", "Cúp"},
                {"axe", "Rìu"}, {"shovel", "Xẻng"}, {"sword", "Kiếm"}, {"hoe", "Cuốc"}
        };

        // Nút công cụ
        for (int i = 0; i < 8; i++) {
            final String key = tools[i][0];
            int col = i % 4, row = i / 4;
            screen.addUIElement(new CustomButton(cx + col * (btnW + 7), cy + 12 + row * 24, btnW, 20, Text.empty(), btn -> toggleTool(screen, key)));
        }

        int splitY = cy + 70;
        int inputW = (cw / 2) - 60;

        if (blockInput == null) {
            blockInput = new TextFieldWidget(screen.getTextRenderer(), cx + 6, splitY + 22, inputW, 16, Text.empty());
            blockInput.setMaxLength(100);
            BlockFilterManager.updateSearch("");
            blockInput.setChangedListener(text -> {
                BlockFilterManager.updateSearch(text);
                searchScroll = 0;
                blError = "";
            });
        } else {
            blockInput.setX(cx + 6); blockInput.setY(splitY + 22); blockInput.setWidth(inputW);
        }
        screen.addUIElement(blockInput);

        screen.addUIElement(new AmberButton(cx + (cw / 2) - 50, splitY + 22, 44, 16, Text.literal("Thêm"), btn -> {
            addBlock(screen);
        }));

        // LƯU Ý: Không còn các vòng lặp tạo CustomButton "XÓA" hay "CHỌN" ở đây nữa.
        // Giao diện giờ là Data-driven hoàn toàn!
    }

    private void addBlock(MainMenuScreen screen) {
        String input = blockInput.getText();
        Identifier validId = BlockFilterManager.validateAndParseBlock(input);

        if (validId == null) {
            blError = "Block không tồn tại!";
            return;
        }

        if (screen.getState().blacklist.contains(validId)) {
            blError = "Đã có trong danh sách!";
            return;
        }

        screen.getState().blacklist.add(validId);
        blockInput.setText("");
        BlockFilterManager.updateSearch(""); // Reset suggest sau khi thêm thành công
        blError = "";
    }

    private void toggleTool(MainMenuScreen screen, String key) {
        var tools = screen.getState().enabledTools;
        if ("all".equals(key)) {
            boolean now = !tools.getOrDefault("all", false);
            tools.put("all", now);
            if (now) for (String k : List.of("hand", "item", "pickaxe", "axe", "shovel", "sword", "hoe")) tools.put(k, false);
        } else {
            tools.put(key, !tools.getOrDefault(key, false));
            if (Boolean.TRUE.equals(tools.get(key))) tools.put("all", false);
        }
        screen.rebuildMenu();
    }

    @Override
    public void render(DrawContext ctx, MainMenuScreen screen, int cx, int cy, int cw, int ch, int mouseX, int mouseY, float delta) {
        // Lưu tọa độ thật để dùng cho sự kiện Click
        this.lastCx = cx; this.lastCy = cy; this.lastCw = cw; this.lastCh = ch;

        // Vẽ Khung Công Cụ
        ctx.drawTextWithShadow(screen.getTextRenderer(), "CÔNG CỤ KÍCH HOẠT QUÉT", cx, cy, 0xFFA0AEC0);
        int btnW = (cw - 21) / 4;
        String[][] toolsInfo = {
                {"all", "TẤT CẢ"}, {"hand", "Tay không"}, {"item", "Vật phẩm"}, {"pickaxe", "Cúp"},
                {"axe", "Rìu"}, {"shovel", "Xẻng"}, {"sword", "Kiếm"}, {"hoe", "Cuốc"}
        };
        for (int i = 0; i < 8; i++) {
            boolean on = Boolean.TRUE.equals(screen.getState().enabledTools.get(toolsInfo[i][0]));
            int col = i % 4, row = i / 4;
            int bx = cx + col * (btnW + 7), by = cy + 12 + row * 24;

            ctx.fill(bx, by, bx + btnW, by + 20, on ? 0x4D10B981 : DrawHelper.BG_CARD);
            DrawHelper.drawSolidBorder(ctx, bx, by, btnW, 20, on ? 0xFF10B981 : DrawHelper.BORDER_MODERN);
            int tc = on ? 0xFFFFFFFF : 0xFFA0AEC0;
            ctx.drawTextWithShadow(screen.getTextRenderer(), toolsInfo[i][1], bx + (btnW - screen.getTextRenderer().getWidth(toolsInfo[i][1])) / 2, by + 6, tc);
        }

        int splitY = cy + 70;
        int splitH = ch - 70;
        int listY = splitY + 44;
        int maxItems = Math.max(1, (splitH - 44 - 6) / 20);

        int halfW = (cw - 10) / 2;
        int rightX = cx + halfW + 10;

        // VẼ KHUNG GỢI Ý (BÊN TRÁI)
        DrawHelper.drawCard(ctx, cx, splitY, halfW, splitH);
        ctx.drawTextWithShadow(screen.getTextRenderer(), "GỢI Ý BLOCK", cx + 8, splitY + 8, 0xFFFFFFFF);

        List<Identifier> suggestions = BlockFilterManager.getSearchCache();
        if (suggestions.isEmpty() && !blockInput.getText().isEmpty()) {
            ctx.drawTextWithShadow(screen.getTextRenderer(), "Không tìm thấy...", cx + 8, listY + 6, 0xFF6B7280);
        } else {
            // Hiển thị gợi ý bên trong khung trái
            for (int i = 0; i < Math.min(suggestions.size(), maxItems); i++) {
                int ry = listY + i * 20;
                Identifier id = suggestions.get(i);
                boolean hovered = mouseX >= cx + 4 && mouseX <= cx + halfW - 4 && mouseY >= ry && mouseY < ry + 20;
                if (hovered) ctx.fill(cx + 4, ry, cx + halfW - 4, ry + 20, 0x33FFFFFF);
                ctx.drawTextWithShadow(screen.getTextRenderer(), id.toString(), cx + 8, ry + 5, hovered ? 0xFFFFFFFF : 0xFFA0AEC0);
            }
        }

        // VẼ KHUNG BLACKLIST (BÊN PHẢI)
        DrawHelper.drawCard(ctx, rightX, splitY, halfW, splitH);
        String rightTitle = "ĐANG BỊ CẤM (" + screen.getState().blacklist.size() + ")";
        ctx.drawTextWithShadow(screen.getTextRenderer(), rightTitle, rightX + 8, splitY + 8, 0xFFFFFFFF);
        if (!blError.isEmpty()) {
            ctx.drawTextWithShadow(screen.getTextRenderer(), blError, rightX + halfW - 8 - screen.getTextRenderer().getWidth(blError), splitY + 8, ThemeColors.TEXT_ERROR);
        }

        List<Identifier> bl = new ArrayList<>(screen.getState().blacklist);
        if (bl.isEmpty()) {
            ctx.drawTextWithShadow(screen.getTextRenderer(), "Trống", rightX + 8, listY + 6, 0xFF6B7280);
        } else {
            int startBl = Math.max(0, Math.min(blScroll, bl.size() - maxItems));
            for (int i = startBl; i < Math.min(bl.size(), startBl + maxItems); i++) {
                int ry = listY + (i - startBl) * 20;
                Identifier id = bl.get(i);
                boolean hovered = mouseX >= rightX + 4 && mouseX <= rightX + halfW - 4 && mouseY >= ry && mouseY < ry + 20;
                if (hovered) ctx.fill(rightX + 4, ry, rightX + halfW - 4, ry + 20, 0x4DFF0000);
                ctx.drawTextWithShadow(screen.getTextRenderer(), id.toString(), rightX + 8, ry + 5, hovered ? 0xFFFFFFFF : 0xFFFF7171);
            }
        }


    }

    @Override
    public boolean mouseClicked(MainMenuScreen screen, double mx, double my, int btn) {
        int splitY = lastCy + 70;
        int listY = splitY + 44;
        int splitH = lastCh - 70;
        int maxItems = Math.max(1, (splitH - 44 - 6) / 20);
        int halfW = (lastCw - 10) / 2;
        int rightX = lastCx + halfW + 10;

        // CLICK GỢI Ý (BÊN TRÁI)
        List<Identifier> suggestions = BlockFilterManager.getSearchCache();
        if (mx >= lastCx + 4 && mx <= lastCx + halfW - 4 && my >= listY && my < listY + maxItems * 20) {
            int idx = (int)((my - listY) / 20);
            if (idx >= 0 && idx < suggestions.size()) {
                blockInput.setText(suggestions.get(idx).toString());
                addBlock(screen);
                return true;
            }
        }

        // CLICK XÓA BLACKLIST (BÊN PHẢI)
        if (mx >= rightX + 4 && mx <= rightX + halfW - 4 && my >= listY && my < listY + maxItems * 20) {
            int index = blScroll + (int) ((my - listY) / 20);
            List<Identifier> bl = new ArrayList<>(screen.getState().blacklist);
            if (index >= 0 && index < bl.size()) {
                screen.getState().blacklist.remove(bl.get(index));
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(MainMenuScreen screen, double mx, double my, double h, double v) {
        int halfW = (screen.getW() - 20 - 10) / 2;

        // Chỉ cập nhật Offset cuộn. KHÔNG gọi rebuildMenu() để tránh giật lag.
        if (false) {
            searchScroll = Math.max(0, searchScroll - (int) v);
        } else {
            blScroll = Math.max(0, blScroll - (int) (v * 2));
        }
        return true;
    }
}