package com.tcveinminer.gui.screens;

import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.gui.CustomButton;
import com.tcveinminer.util.DrawHelper;
import com.tcveinminer.util.ThemeColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.LinkedHashSet;
import java.util.Set;

public class SettingsScreen extends Screen {

    private static final int W = 320, H = 310;
    private static final int HEADER_H = 24;
    private static final int TAB_H = 20;

    // Tabs
    private static final int TAB_GENERAL = 0;
    private static final int TAB_SHAPES  = 1;
    private int activeTab = TAB_GENERAL;

    private final Screen parent;
    private int x, y;

    // Working copy — General
    private int maxBlocks;
    private boolean requireSneak;
    private boolean requireCorrectTool;
    private boolean consumeDurability;
    private boolean diagonalMining;
    private int cooldownTicks;

    // Working copy — Shapes (enabled in radial menu)
    private Set<String> enabledShapes;

    private MaxBlockSlider maxBlockSlider;
    private CooldownSlider cooldownSlider;

    public SettingsScreen(Screen parent) {
        super(Text.literal("Cài Đặt"));
        this.parent = parent;
        ModConfig cfg = ConfigManager.get();
        maxBlocks          = cfg.maxBlocks;
        requireSneak       = cfg.requireSneak;
        requireCorrectTool = cfg.requireCorrectTool;
        consumeDurability  = cfg.consumeDurability;
        diagonalMining     = cfg.diagonalMining;
        cooldownTicks      = cfg.cooldownTicks;
        enabledShapes      = new LinkedHashSet<>(cfg.enabledShapes);
    }

    @Override
    protected void init() {
        x = (width - W) / 2;
        y = (height - H) / 2;
        rebuildWidgets();
    }

    private void rebuildWidgets() {
        clearChildren();

        // Tab buttons
        addDrawableChild(new CustomButton(x + 10, y + HEADER_H + 2, 140, TAB_H,
                Text.literal("Cài đặt chung"), btn -> { activeTab = TAB_GENERAL; rebuildWidgets(); }));
        addDrawableChild(new CustomButton(x + 160, y + HEADER_H + 2, 140, TAB_H,
                Text.literal("Chế độ đào"), btn -> { activeTab = TAB_SHAPES; rebuildWidgets(); }));

        int contentY = y + HEADER_H + TAB_H + 10;
        int rowStep = 26;

        if (activeTab == TAB_GENERAL) {
            int rowY = contentY;

            maxBlockSlider = new MaxBlockSlider(x + 180, rowY, 120, 16);
            addDrawableChild(maxBlockSlider);
            rowY += rowStep;

            addToggle(rowY, () -> requireSneak,       v -> requireSneak = v);       rowY += rowStep;
            addToggle(rowY, () -> requireCorrectTool, v -> requireCorrectTool = v); rowY += rowStep;
            addToggle(rowY, () -> consumeDurability,  v -> consumeDurability = v);  rowY += rowStep;
            addToggle(rowY, () -> diagonalMining,     v -> diagonalMining = v);     rowY += rowStep;

            cooldownSlider = new CooldownSlider(x + 180, rowY, 120, 16);
            addDrawableChild(cooldownSlider);

        } else {
            // Shapes tab — toggle từng shape
            int rowY = contentY;
            for (ModConfig.MiningShape shape : ModConfig.MiningShape.values()) {
                final ModConfig.MiningShape s = shape;
                boolean on = enabledShapes.contains(shape.name());
                addDrawableChild(new CustomButton(x + W - 80, rowY, 60, 16,
                        Text.literal(on ? "BẬT" : "TẮT"), btn -> {
                    if (enabledShapes.contains(s.name())) {
                        // Đừng tắt nếu chỉ còn 1
                        if (enabledShapes.size() > 1) enabledShapes.remove(s.name());
                    } else {
                        enabledShapes.add(s.name());
                    }
                    rebuildWidgets();
                }));
                rowY += 22;
            }
        }

        // Save / Cancel
        addDrawableChild(new CustomButton(x + 10, y + H - 28, 145, 18,
                Text.literal("LƯU & QUAY LẠI"), btn -> { save(); client.setScreen(parent); }));
        addDrawableChild(new CustomButton(x + W - 155, y + H - 28, 145, 18,
                Text.literal("HỦY"), btn -> client.setScreen(parent)));
    }

    private interface BoolGet { boolean get(); }
    private interface BoolSet { void set(boolean v); }

    private void addToggle(int rowY, BoolGet get, BoolSet set) {
        addDrawableChild(new CustomButton(x + W - 80, rowY, 60, 16,
                Text.literal(get.get() ? "BẬT" : "TẮT"), btn -> {
            set.set(!get.get());
            btn.setMessage(Text.literal(get.get() ? "BẬT" : "TẮT"));
        }));
    }

    private void save() {
        ModConfig cfg = ConfigManager.get();
        cfg.maxBlocks          = maxBlocks;
        cfg.requireSneak       = requireSneak;
        cfg.requireCorrectTool = requireCorrectTool;
        cfg.consumeDurability  = consumeDurability;
        cfg.diagonalMining     = diagonalMining;
        cfg.cooldownTicks      = cooldownTicks;
        cfg.enabledShapes      = enabledShapes;
        ConfigManager.save();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        DrawHelper.drawPanel(ctx, x, y, W, H);
        DrawHelper.drawHeader(ctx, x, y, W, HEADER_H);
        ctx.drawTextWithShadow(textRenderer, "⚙ Cài Đặt", x + 12, y + 8, ThemeColors.TEXT_TITLE);

        // Tab highlight
        int tabGenX = x + 10, tabShpX = x + 160;
        int tabY = y + HEADER_H + 2;
        if (activeTab == TAB_GENERAL) ctx.fill(tabGenX, tabY + TAB_H - 2, tabGenX + 140, tabY + TAB_H, ThemeColors.TOGGLE_ON_BORDER);
        else                          ctx.fill(tabShpX, tabY + TAB_H - 2, tabShpX + 140, tabY + TAB_H, ThemeColors.TOGGLE_ON_BORDER);

        int rowY = y + HEADER_H + TAB_H + 14;
        int rowStep = 26;

        if (activeTab == TAB_GENERAL) {
            drawLabel(ctx, "Số block tối đa:", rowY);
            ctx.drawTextWithShadow(textRenderer, String.valueOf(maxBlocks), x + W - 14 - textRenderer.getWidth(String.valueOf(maxBlocks)), rowY + 4, ThemeColors.TEXT_VALUE);
            rowY += rowStep;
            drawLabel(ctx, "Yêu cầu Sneak (Shift):", rowY);    rowY += rowStep;
            drawLabel(ctx, "Yêu cầu đúng công cụ:", rowY);     rowY += rowStep;
            drawLabel(ctx, "Hao mòn công cụ:", rowY);           rowY += rowStep;
            drawLabel(ctx, "Diagonal Mining:", rowY);            rowY += rowStep;
            drawLabel(ctx, "Cooldown (ticks):", rowY);
            ctx.drawTextWithShadow(textRenderer, String.valueOf(cooldownTicks), x + W - 14 - textRenderer.getWidth(String.valueOf(cooldownTicks)), rowY + 4, ThemeColors.TEXT_VALUE);
        } else {
            ctx.drawTextWithShadow(textRenderer, "Chọn chế độ nào hiện trong radial menu:",
                    x + 14, rowY - 8, ThemeColors.TEXT_LABEL);
            for (ModConfig.MiningShape shape : ModConfig.MiningShape.values()) {
                boolean on = enabledShapes.contains(shape.name());
                ctx.drawTextWithShadow(textRenderer, shape.icon + " " + shape.label,
                        x + 14, rowY + 4,
                        on ? ThemeColors.TOGGLE_ON_TEXT : ThemeColors.TEXT_LABEL);
                rowY += 22;
            }
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawLabel(DrawContext ctx, String label, int rowY) {
        ctx.drawTextWithShadow(textRenderer, label, x + 14, rowY + 4, ThemeColors.TEXT_LABEL);
    }

    private class MaxBlockSlider extends SliderWidget {
        MaxBlockSlider(int x, int y, int w, int h) {
            super(x, y, w, h, Text.literal(String.valueOf(maxBlocks)), (maxBlocks - 1) / 255.0);
        }
        @Override protected void updateMessage() { maxBlocks = (int)(value * 255) + 1; setMessage(Text.literal(String.valueOf(maxBlocks))); }
        @Override protected void applyValue() { updateMessage(); }
    }

    private class CooldownSlider extends SliderWidget {
        CooldownSlider(int x, int y, int w, int h) {
            super(x, y, w, h, Text.literal(String.valueOf(cooldownTicks)), cooldownTicks / 100.0);
        }
        @Override protected void updateMessage() { cooldownTicks = (int)(value * 20) * 5; setMessage(Text.literal(String.valueOf(cooldownTicks))); }
        @Override protected void applyValue() { updateMessage(); }
    }

    @Override
    public boolean shouldPause() { return false; }
}