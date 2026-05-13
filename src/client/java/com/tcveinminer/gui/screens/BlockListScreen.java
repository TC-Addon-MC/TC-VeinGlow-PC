package com.tcveinminer.gui.screens;

import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.gui.CustomButton;
import com.tcveinminer.util.DrawHelper;
import com.tcveinminer.util.ThemeColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;

public class BlockListScreen extends Screen {

    private static final int W = 300, H = 270;
    private static final int HEADER_H = 24;
    private static final int LIST_Y_OFFSET = 60;
    private static final int LIST_H = 130;
    private static final int ROW_H = 20;

    private final Screen parent;
    private final List<String> blocks = new ArrayList<>();
    private int scrollOffset = 0;
    private int x, y;

    // Add popup state
    private boolean showAddPopup = false;
    private TextFieldWidget addField;
    private String addError = null;

    private TextFieldWidget searchField;

    public BlockListScreen(Screen parent) {
        super(Text.literal("Block List"));
        this.parent = parent;
        blocks.addAll(ConfigManager.get().blacklistedBlocks);
    }

    @Override
    protected void init() {
        x = (width - W) / 2;
        y = (height - H) / 2;

        // Search field
        searchField = new TextFieldWidget(textRenderer, x + 10, y + HEADER_H + 8, W - 20, 16,
            Text.literal("Tìm kiếm..."));
        searchField.setMaxLength(100);
        searchField.setPlaceholder(Text.literal("Tìm kiếm..."));
        addDrawableChild(searchField);

        addDrawableChild(new CustomButton(x + 10, y + H - 60, 110, 16,
                Text.literal("+ THÊM BLOCK"), btn -> {
            showAddPopup = true;
            if (addField != null) addField.setText("");
            addError = null;
        }));

        addDrawableChild(new CustomButton(x + 130, y + H - 60, 80, 16,
                Text.literal("XÓA TẤT CẢ"), btn -> {
            blocks.clear();
            scrollOffset = 0;
        }));

        addDrawableChild(new CustomButton(x + W / 2 - 70, y + H - 36, 140, 18,
                Text.literal("LƯU & QUAY LẠI"), btn -> {
            ConfigManager.get().blacklistedBlocks.clear();
            ConfigManager.get().blacklistedBlocks.addAll(blocks);
            ConfigManager.save();
            client.setScreen(parent);
        }));

        // Popup add field
        addField = new TextFieldWidget(textRenderer, x + W/2 - 100, y + H/2 - 10, 200, 20,
            Text.literal("minecraft:diamond_ore"));
        addField.setMaxLength(200);
        addField.setVisible(false);
        addDrawableChild(addField);
    }

    private List<String> getFilteredBlocks() {
        String query = searchField != null ? searchField.getText().toLowerCase() : "";
        if (query.isBlank()) return blocks;
        return blocks.stream().filter(b -> b.contains(query)).toList();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        DrawHelper.drawPanel(ctx, x, y, W, H);
        DrawHelper.drawHeader(ctx, x, y, W, HEADER_H);
        ctx.drawTextWithShadow(textRenderer, "📋 Danh Sách Block Đen", x + 12, y + 8, ThemeColors.TEXT_TITLE);

        // List area background
        int listX = x + 10;
        int listY = y + LIST_Y_OFFSET;
        ctx.fill(listX, listY, listX + W - 20, listY + LIST_H, ThemeColors.BG_ROW_A);
        DrawHelper.drawSolidBorder(ctx, listX, listY, W - 20, LIST_H, ThemeColors.BORDER_INNER);

        // Scrollable rows
        List<String> filtered = getFilteredBlocks();
        int maxVisible = LIST_H / ROW_H;
        int startIdx = Math.max(0, Math.min(scrollOffset, Math.max(0, filtered.size() - maxVisible)));
        ctx.enableScissor(listX, listY, listX + W - 20, listY + LIST_H);

        for (int i = startIdx; i < Math.min(filtered.size(), startIdx + maxVisible); i++) {
            String block = filtered.get(i);
            int rowY = listY + (i - startIdx) * ROW_H;
            int rowBg = (i % 2 == 0) ? ThemeColors.BG_ROW_A : ThemeColors.BG_ROW_B;
            boolean hovered = mouseX >= listX && mouseX <= listX + W - 20 - 30
                           && mouseY >= rowY && mouseY <= rowY + ROW_H;
            ctx.fill(listX, rowY, listX + W - 20, rowY + ROW_H, hovered ? ThemeColors.BG_ROW_HOVER : rowBg);

            ctx.drawTextWithShadow(textRenderer, block, listX + 6, rowY + 6, ThemeColors.TEXT_LABEL);

            // Remove button [✕]
            boolean removeBtnHovered = mouseX >= listX + W - 26 && mouseX <= listX + W - 20
                                    && mouseY >= rowY + 3 && mouseY <= rowY + 16;
            ctx.fill(listX + W - 26, rowY + 3, listX + W - 20, rowY + 16,
                removeBtnHovered ? 0xFF6B2737 : 0xFF3A1A1A);
            ctx.drawTextWithShadow(textRenderer, "✕", listX + W - 24, rowY + 5, ThemeColors.TEXT_ERROR);
        }
        ctx.disableScissor();

        if (filtered.isEmpty()) {
            String empty = "Không có block nào";
            ctx.drawTextWithShadow(textRenderer, empty,
                listX + (W - 20 - textRenderer.getWidth(empty)) / 2, listY + LIST_H / 2 - 4,
                ThemeColors.TEXT_LABEL);
        }

        super.render(ctx, mouseX, mouseY, delta);

        // Popup overlay
        if (showAddPopup) {
            renderAddPopup(ctx, mouseX, mouseY);
        }
    }

    private void renderAddPopup(DrawContext ctx, int mouseX, int mouseY) {
        int pw = 240, ph = 80;
        int px = x + (W - pw) / 2;
        int py = y + (H - ph) / 2;

        DrawHelper.drawPanel(ctx, px, py, pw, ph);
        ctx.drawTextWithShadow(textRenderer, "Nhập Block ID:", px + 12, py + 10, ThemeColors.TEXT_TITLE);

        addField.setX(px + 10);
        addField.setY(py + 25);
        addField.setWidth(pw - 20);
        addField.setVisible(true);
        addField.render(ctx, mouseX, mouseY, 0);

        if (addError != null) {
            ctx.drawTextWithShadow(textRenderer, addError, px + 10, py + 48, ThemeColors.TEXT_ERROR);
        }

        // Popup buttons
        boolean addHov = mouseX >= px + 10 && mouseX <= px + 70 && mouseY >= py + 58 && mouseY <= py + 72;
        boolean canHov = mouseX >= px + pw - 70 && mouseX <= px + pw - 10 && mouseY >= py + 58 && mouseY <= py + 72;

        DrawHelper.drawButton(ctx, px + 10, py + 58, 60, 14, addHov, false);
        ctx.drawTextWithShadow(textRenderer, "THÊM", px + 10 + (60 - textRenderer.getWidth("THÊM")) / 2,
            py + 62, ThemeColors.BTN_TEXT);

        DrawHelper.drawButton(ctx, px + pw - 70, py + 58, 60, 14, canHov, false);
        ctx.drawTextWithShadow(textRenderer, "HỦY", px + pw - 70 + (60 - textRenderer.getWidth("HỦY")) / 2,
            py + 62, ThemeColors.BTN_TEXT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showAddPopup) {
            int pw = 240, ph = 80;
            int px = x + (W - pw) / 2;
            int py = y + (H - ph) / 2;

            // THÊM button
            if (mouseX >= px + 10 && mouseX <= px + 70 && mouseY >= py + 58 && mouseY <= py + 72) {
                tryAddBlock(addField.getText().trim());
                return true;
            }
            // HỦY button
            if (mouseX >= px + pw - 70 && mouseX <= px + pw - 10 && mouseY >= py + 58 && mouseY <= py + 72) {
                showAddPopup = false;
                addField.setVisible(false);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        // Remove block clicks
        List<String> filtered = getFilteredBlocks();
        int maxVisible = LIST_H / ROW_H;
        int listX = x + 10;
        int listY = y + LIST_Y_OFFSET;
        int startIdx = Math.max(0, Math.min(scrollOffset, Math.max(0, filtered.size() - maxVisible)));

        for (int i = startIdx; i < Math.min(filtered.size(), startIdx + maxVisible); i++) {
            int rowY = listY + (i - startIdx) * ROW_H;
            if (mouseX >= listX + W - 26 && mouseX <= listX + W - 20
             && mouseY >= rowY + 3 && mouseY <= rowY + 16) {
                blocks.remove(filtered.get(i));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void tryAddBlock(String id) {
        if (id.isBlank()) { addError = "Block không hợp lệ"; return; }
        try {
            Identifier ident = Identifier.of(id);
            if (!Registries.BLOCK.containsId(ident)) {
                addError = "Block không tồn tại trong game";
                return;
            }
        } catch (Exception e) {
            addError = "Block không hợp lệ";
            return;
        }
        if (!blocks.contains(id)) blocks.add(id);
        showAddPopup = false;
        addField.setVisible(false);
        addError = null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset = Math.max(0, scrollOffset - (int) verticalAmount);
        return true;
    }

    @Override
    public boolean shouldPause() { return false; }
}
