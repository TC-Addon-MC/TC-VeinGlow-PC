package com.tcveinminer.gui.screens.tabs;

import com.tcveinminer.gui.CustomButton;
import com.tcveinminer.gui.screens.MainMenuScreen;
import com.tcveinminer.gui.widgets.AmberButton;
import com.tcveinminer.util.DrawHelper;
import com.tcveinminer.util.ThemeColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import java.util.List;

public class FilterTab implements MenuTab {
    private TextFieldWidget blockInput;
    private int blScroll = 0;
    private String blError = "";

    @Override
    public void init(MainMenuScreen screen, int cx, int cy, int cw, int ch) {
        int btnW = (cw - 9) / 4;
        String[][] tools = {
                {"all", "TẤT CẢ"}, {"hand", "Tay"}, {"item", "Vật"}, {"pickaxe", "Cúp"},
                {"axe", "Rìu"}, {"shovel", "Xẻng"}, {"sword", "Kiếm"}, {"hoe", "Cuốc"}
        };
        for (int i = 0; i < 8; i++) {
            final String key = tools[i][0];
            int col = i % 4, row = i / 4;
            screen.addUIElement(new CustomButton(cx + col * (btnW + 3), cy + 12 + row * 22, btnW, 20, Text.empty(), btn -> toggleTool(screen, key)));
        }

        int inputY = cy + 60;
        if (blockInput == null) {
            blockInput = new TextFieldWidget(screen.getTextRenderer(), cx, inputY, cw - 50, 16, Text.empty());
            blockInput.setMaxLength(100);
        } else {
            blockInput.setX(cx); blockInput.setY(inputY); blockInput.setWidth(cw - 50);
        }
        screen.addUIElement(blockInput);

        screen.addUIElement(new AmberButton(cx + cw - 46, inputY, 46, 16, Text.literal("Thêm"), btn -> {
            String id = blockInput.getText().trim().toLowerCase();
            if (id.isBlank()) { blError = "Trống"; screen.rebuildMenu(); return; }
            if (!id.contains(":")) { blError = "Thiếu :"; screen.rebuildMenu(); return; }
            if (screen.getState().blacklist.contains(id)) { blError = "Đã có"; screen.rebuildMenu(); return; }
            screen.getState().blacklist.add(id); blError = ""; screen.rebuildMenu();
        }));

        int listY = inputY + 22;
        int maxBl = Math.max(1, (ch - (listY - cy) - 4) / 16);
        List<String> bl = screen.getState().blacklist;
        int startBl = Math.max(0, Math.min(blScroll, bl.size() - maxBl));
        for (int i = startBl; i < Math.min(bl.size(), startBl + maxBl); i++) {
            final String bid = bl.get(i);
            int ry = listY + 2 + (i - startBl) * 16;
            screen.addUIElement(new CustomButton(cx + cw - 36, ry, 32, 14, Text.literal("XÓA"), btn -> { screen.getState().blacklist.remove(bid); screen.rebuildMenu(); }));
        }
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
        ctx.drawTextWithShadow(screen.getTextRenderer(), "CÔNG CỤ KÍCH HOẠT", cx, cy, ThemeColors.TEXT_LABEL);
        int btnW = (cw - 9) / 4;
        String[][] toolsInfo = {
                {"all", "TẤT CẢ"}, {"hand", "Tay"}, {"item", "Vật"}, {"pickaxe", "Cúp"},
                {"axe", "Rìu"}, {"shovel", "Xẻng"}, {"sword", "Kiếm"}, {"hoe", "Cuốc"}
        };
        for (int i = 0; i < 8; i++) {
            boolean on = Boolean.TRUE.equals(screen.getState().enabledTools.get(toolsInfo[i][0]));
            int col = i % 4, row = i / 4;
            int bx = cx + col * (btnW + 3), by = cy + 12 + row * 22;
            ctx.fill(bx, by, bx + btnW, by + 20, on ? ThemeColors.EMERALD_FILL : ThemeColors.BG_ROW);
            DrawHelper.drawSolidBorder(ctx, bx, by, btnW, 20, on ? ThemeColors.EMERALD_BORDER : ThemeColors.BORDER_DEFAULT);
            int tc = on ? ThemeColors.EMERALD_TEXT : ThemeColors.TEXT_LABEL;
            ctx.drawTextWithShadow(screen.getTextRenderer(), toolsInfo[i][1], bx + (btnW - screen.getTextRenderer().getWidth(toolsInfo[i][1])) / 2, by + 6, tc);
        }

        int inputY = cy + 60;
        ctx.drawTextWithShadow(screen.getTextRenderer(), "BLOCK BỊ CẤM", cx, inputY - 10, ThemeColors.TEXT_LABEL);
        if (!blError.isEmpty()) ctx.drawTextWithShadow(screen.getTextRenderer(), blError, cx + cw - screen.getTextRenderer().getWidth(blError), inputY - 10, ThemeColors.TEXT_ERROR);

        int listY = inputY + 22;
        int listH = ch - (listY - cy);
        DrawHelper.drawCard(ctx, cx, listY, cw, listH);

        List<String> bl = screen.getState().blacklist;
        if (bl.isEmpty()) {
            ctx.drawTextWithShadow(screen.getTextRenderer(), "Trống", cx + cw / 2 - 15, listY + (listH - 8) / 2, ThemeColors.TEXT_HINT);
        } else {
            int maxBl = Math.max(1, (listH - 4) / 16);
            int startBl = Math.max(0, Math.min(blScroll, bl.size() - maxBl));
            for (int i = startBl; i < Math.min(bl.size(), startBl + maxBl); i++) {
                int ry = listY + 2 + (i - startBl) * 16;
                ctx.fill(cx + 2, ry, cx + cw - 2, ry + 16, i % 2 == 0 ? ThemeColors.BG_ROW : ThemeColors.BG_ROW_HOVER);
                ctx.drawTextWithShadow(screen.getTextRenderer(), bl.get(i), cx + 6, ry + 4, ThemeColors.TEXT_LABEL);
            }
        }
    }

    @Override
    public boolean mouseScrolled(MainMenuScreen screen, double mx, double my, double h, double v) {
        blScroll = Math.max(0, blScroll - (int) v);
        return true;
    }
}