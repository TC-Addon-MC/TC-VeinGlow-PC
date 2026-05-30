package com.tcveinminer.client.gui.screens;

import com.tcveinminer.client.gui.CustomButton;
import com.tcveinminer.client.util.ButtonDrawUtil;
import com.tcveinminer.client.util.DrawHelper;
import com.tcveinminer.client.util.ThemeColors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import com.tcveinminer.client.config.ClientConfigManager;

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
    private EditBox addField;
    private String addError = null;

    private EditBox searchField;

    public BlockListScreen(Screen parent) {
        super(Component.translatable("gui.tcveinminer.blocklist.title"));
        this.parent = parent;
        blocks.addAll(ClientConfigManager.instance.personalBlacklist);
    }

    @Override
    protected void init() {
        x = (width - W) / 2;
        y = (height - H) / 2;

        // Search field
        searchField = new EditBox(font, x + 10, y + HEADER_H + 8, W - 20, 16,
                Component.translatable("gui.tcveinminer.search"));
        searchField.setMaxLength(100);
        searchField.setHint(Component.translatable("gui.tcveinminer.search"));
        addRenderableWidget(searchField);

        addRenderableWidget(new CustomButton(x + 10, y + H - 60, 110, 16,
                Component.translatable("gui.tcveinminer.button.add_block"), btn -> {
                    showAddPopup = true;
                    if (addField != null)
                        addField.setValue("");
                    addError = null;
                }));

        addRenderableWidget(new CustomButton(x + 130, y + H - 60, 80, 16,
                Component.translatable("gui.tcveinminer.button.clear_all"), btn -> {
                    blocks.clear();
                    scrollOffset = 0;
                }));

        addRenderableWidget(new CustomButton(x + W / 2 - 70, y + H - 36, 140, 18,
                Component.translatable("gui.tcveinminer.button.save_back"), btn -> {
                    ClientConfigManager.instance.personalBlacklist.clear();
                    ClientConfigManager.instance.personalBlacklist.addAll(blocks);
                    ClientConfigManager.save();
                    minecraft.setScreen(parent);
                }));

        // Popup add field
        addField = new EditBox(font, x + W / 2 - 100, y + H / 2 - 10, 200, 20,
                Component.literal("minecraft:diamond_ore"));
        addField.setMaxLength(200);
        addField.setVisible(false);
        addRenderableWidget(addField);
    }

    private List<String> getFilteredBlocks() {
        String query = searchField != null ? searchField.getValue().toLowerCase() : "";
        if (query.isBlank())
            return blocks;
        return blocks.stream().filter(b -> b.contains(query)).toList();
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        DrawHelper.drawPanel(ctx, x, y, W, H);
        DrawHelper.drawHeader(ctx, x, y, W, HEADER_H);
        ctx.drawString(font, Component.translatable("gui.tcveinminer.blocklist.header"), x + 12,
                y + 8, ThemeColors.TEXT_TITLE, true);

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

            ctx.drawString(font, Component.literal(block), listX + 6, rowY + 6, ThemeColors.TEXT_LABEL, true);

            // Remove button [✕]
            boolean removeBtnHovered = mouseX >= listX + W - 26 && mouseX <= listX + W - 20
                    && mouseY >= rowY + 3 && mouseY <= rowY + 16;
            ctx.fill(listX + W - 26, rowY + 3, listX + W - 20, rowY + 16,
                    removeBtnHovered ? 0xFF6B2737 : 0xFF3A1A1A);
            ctx.drawString(font, Component.literal("X"), listX + W - 24, rowY + 5, ThemeColors.TEXT_ERROR, true);
        }
        ctx.disableScissor();

        if (filtered.isEmpty()) {
            Component empty = Component.translatable("gui.tcveinminer.blocklist.empty");
            ctx.drawString(font, empty,
                    listX + (W - 20 - font.width(empty)) / 2, listY + LIST_H / 2 - 4,
                    ThemeColors.TEXT_LABEL, true);
        }

        super.render(ctx, mouseX, mouseY, delta);

        // Popup overlay
        if (showAddPopup) {
            renderAddPopup(ctx, mouseX, mouseY);
        }
    }

    private void renderAddPopup(GuiGraphics ctx, int mouseX, int mouseY) {
        int pw = 240, ph = 80;
        int px = x + (W - pw) / 2;
        int py = y + (H - ph) / 2;

        DrawHelper.drawPanel(ctx, px, py, pw, ph);
        ctx.drawString(font, Component.translatable("gui.tcveinminer.blocklist.add_prompt"),
                px + 12, py + 10, ThemeColors.TEXT_TITLE, true);

        addField.setX(px + 10);
        addField.setY(py + 25);
        addField.setWidth(pw - 20);
        addField.setVisible(true);
        addField.render(ctx, mouseX, mouseY, 0);

        if (addError != null) {
            ctx.drawString(font, Component.literal(addError), px + 10, py + 48, ThemeColors.TEXT_ERROR, true);
        }

        // Popup buttons
        boolean addHov = mouseX >= px + 10 && mouseX <= px + 70 && mouseY >= py + 58 && mouseY <= py + 72;
        boolean canHov = mouseX >= px + pw - 70 && mouseX <= px + pw - 10 && mouseY >= py + 58 && mouseY <= py + 72;

        Component addStr = Component.translatable("gui.tcveinminer.button.add");
        Component cancelStr = Component.translatable("gui.tcveinminer.button.cancel");

        ButtonDrawUtil.drawPrimary(ctx, px + 10, py + 58, 60, 14, addHov, 0f);
        ctx.drawString(font, addStr, px + 10 + (60 - font.width(addStr)) / 2,
                py + 62, addHov ? 0xFFFFFFFF : 0xFFCDD8E8, true);

        ButtonDrawUtil.drawPrimary(ctx, px + pw - 70, py + 58, 60, 14, canHov, 0f);
        ctx.drawString(font, cancelStr, px + pw - 70 + (60 - font.width(cancelStr)) / 2,
                py + 62, canHov ? 0xFFFFFFFF : 0xFFCDD8E8, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showAddPopup) {
            int pw = 240, ph = 80;
            int px = x + (W - pw) / 2;
            int py = y + (H - ph) / 2;

            // THÊM button
            if (mouseX >= px + 10 && mouseX <= px + 70 && mouseY >= py + 58 && mouseY <= py + 72) {
                tryAddBlock(addField.getValue().trim());
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
        if (id.isBlank()) {
            addError = Component.translatable("gui.tcveinminer.error.invalid_block").getString();
            return;
        }
        try {
            ResourceLocation ident = ResourceLocation.parse(id);
            if (!BuiltInRegistries.BLOCK.containsKey(ident)) {
                addError = Component.translatable("gui.tcveinminer.error.unknown_block").getString();
                return;
            }
        } catch (Exception e) {
            addError = Component.translatable("gui.tcveinminer.error.invalid_block").getString();
            return;
        }
        if (!blocks.contains(id))
            blocks.add(id);
        showAddPopup = false;
        addField.setVisible(false);
        addError = null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int amt = (int) verticalAmount;
        if (amt == 0 && verticalAmount != 0)
            amt = verticalAmount > 0 ? 1 : -1;
        scrollOffset = Math.max(0, scrollOffset - amt);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
