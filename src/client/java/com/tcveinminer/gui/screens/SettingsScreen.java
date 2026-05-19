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

    private static final int W = 320, H = 320;
    private static final int HEADER_H = 24;
    private static final int TAB_H = 20;
    private static final int TAB_GENERAL = 0, TAB_SHAPES = 1;
    private int activeTab = TAB_GENERAL;

    private final Screen parent;
    private int x, y;

    // Working copies
    private int     maxBlocks;
    private boolean requireSneak;
    private boolean requireCorrectTool;
    private boolean consumeDurability;
    private boolean diagonalMining;
    private boolean showHud;
    private int     cooldownTicks;
    private Set<String> enabledShapes;

    public SettingsScreen(Screen parent) {
        super(Text.literal("Cài Đặt"));
        this.parent = parent;
        ModConfig cfg = ConfigManager.get();
        maxBlocks          = cfg.maxBlocks;
        requireSneak       = cfg.requireSneak;
        requireCorrectTool = cfg.requireCorrectTool;
        consumeDurability  = cfg.consumeDurability;
        diagonalMining     = cfg.diagonalMining;
        showHud            = cfg.showHud;
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
        int tabY = y + HEADER_H + 2;

        addDrawableChild(new CustomButton(x + 10, tabY, 140, TAB_H,
            Text.literal("⚙ Cài đặt chung"), btn -> { activeTab = TAB_GENERAL; rebuildWidgets(); }));
        addDrawableChild(new CustomButton(x + 160, tabY, 140, TAB_H,
            Text.literal("⛏ Chế độ đào"), btn -> { activeTab = TAB_SHAPES; rebuildWidgets(); }));

        int contentY = y + HEADER_H + TAB_H + 12;
        int rowStep  = 26;

        if (activeTab == TAB_GENERAL) {
            int rowY = contentY;

            // Max blocks slider
            addDrawableChild(new MaxBlockSlider(x + 180, rowY, 130, 16));
            rowY += rowStep;

            // Toggle buttons — giữ reference để cập nhật text đúng
            addToggleRow(rowY, "requireSneak",       () -> requireSneak,       v -> requireSneak       = v); rowY += rowStep;
            addToggleRow(rowY, "requireCorrectTool", () -> requireCorrectTool, v -> requireCorrectTool = v); rowY += rowStep;
            addToggleRow(rowY, "consumeDurability",  () -> consumeDurability,  v -> consumeDurability  = v); rowY += rowStep;
            addToggleRow(rowY, "diagonalMining",     () -> diagonalMining,     v -> diagonalMining     = v); rowY += rowStep;
            addToggleRow(rowY, "showHud",            () -> showHud,            v -> showHud            = v); rowY += rowStep;

            // Cooldown slider
            addDrawableChild(new CooldownSlider(x + 180, rowY, 130, 16));

        } else {
            // Shapes tab
            int rowY = contentY;
            for (ModConfig.MiningShape shape : ModConfig.MiningShape.values()) {
                final ModConfig.MiningShape s = shape;
                boolean on = enabledShapes.contains(shape.name());
                addDrawableChild(new CustomButton(x + W - 80, rowY, 64, 16,
                    Text.literal(on ? "✓ BẬT" : "✗ TẮT"), btn -> {
                        if (enabledShapes.contains(s.name())) {
                            if (enabledShapes.size() > 1) enabledShapes.remove(s.name());
                        } else {
                            enabledShapes.add(s.name());
                        }
                        rebuildWidgets();
                    }));
                rowY += 22;
                if (rowY > y + H - 50) break; // overflow guard
            }
        }

        addDrawableChild(new CustomButton(x + 10, y + H - 28, 145, 18,
            Text.literal("💾 LƯU & QUAY LẠI"), btn -> { save(); client.setScreen(parent); }));
        addDrawableChild(new CustomButton(x + W - 155, y + H - 28, 145, 18,
            Text.literal("✕ HỦY"), btn -> client.setScreen(parent)));
    }

    private void addToggleRow(int rowY, String key, BoolGet get, BoolSet set) {
        addDrawableChild(new CustomButton(x + W - 80, rowY, 64, 16,
            Text.literal(get.get() ? "✓ BẬT" : "✗ TẮT"), btn -> {
                set.set(!get.get());
                // Rebuild để cập nhật text (đơn giản hơn giữ reference)
                rebuildWidgets();
            }));
    }

    private interface BoolGet { boolean get(); }
    private interface BoolSet { void set(boolean v); }

    private void save() {
        ModConfig cfg = ConfigManager.get();
        cfg.maxBlocks          = maxBlocks;
        cfg.requireSneak       = requireSneak;
        cfg.requireCorrectTool = requireCorrectTool;
        cfg.consumeDurability  = consumeDurability;
        cfg.diagonalMining     = diagonalMining;
        cfg.showHud            = showHud;
        cfg.cooldownTicks      = cooldownTicks;
        cfg.enabledShapes      = enabledShapes;
        ConfigManager.save();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        DrawHelper.drawPanel(ctx, x, y, W, H);
        DrawHelper.drawHeader(ctx, x, y, W, HEADER_H);
        ctx.drawTextWithShadow(textRenderer, "⚙ Cài Đặt TC-VeinGlow", x + 12, y + 8, ThemeColors.TEXT_TITLE);

        // Tab underline
        int tabY = y + HEADER_H + 2;
        if (activeTab == TAB_GENERAL)
            ctx.fill(x + 10, tabY + TAB_H - 2, x + 150, tabY + TAB_H, ThemeColors.TOGGLE_ON_BORDER);
        else
            ctx.fill(x + 160, tabY + TAB_H - 2, x + 300, tabY + TAB_H, ThemeColors.TOGGLE_ON_BORDER);

        int rowY = y + HEADER_H + TAB_H + 16;
        int rowStep = 26;

        if (activeTab == TAB_GENERAL) {
            drawLabelValue(ctx, "Số block tối đa:", String.valueOf(maxBlocks), rowY); rowY += rowStep;
            drawLabelBool (ctx, "Yêu cầu Sneak:",         requireSneak,       rowY); rowY += rowStep;
            drawLabelBool (ctx, "Yêu cầu đúng công cụ:",  requireCorrectTool, rowY); rowY += rowStep;
            drawLabelBool (ctx, "Hao mòn công cụ:",        consumeDurability,  rowY); rowY += rowStep;
            drawLabelBool (ctx, "Diagonal Mining:",         diagonalMining,     rowY); rowY += rowStep;
            drawLabelBool (ctx, "Hiện HUD:",                showHud,            rowY); rowY += rowStep;
            drawLabelValue(ctx, "Cooldown (ticks):", String.valueOf(cooldownTicks), rowY);
        } else {
            ctx.drawTextWithShadow(textRenderer, "Chọn chế độ hiện trong Radial Menu:",
                x + 14, rowY - 6, ThemeColors.TEXT_LABEL);
            for (ModConfig.MiningShape shape : ModConfig.MiningShape.values()) {
                boolean on = enabledShapes.contains(shape.name());
                ctx.drawTextWithShadow(textRenderer,
                    shape.icon + "  " + shape.label,
                    x + 14, rowY + 4,
                    on ? ThemeColors.TOGGLE_ON_TEXT : ThemeColors.TEXT_LABEL);
                rowY += 22;
                if (rowY > y + H - 50) break;
            }
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawLabelValue(DrawContext ctx, String label, String value, int rowY) {
        ctx.drawTextWithShadow(textRenderer, label, x + 14, rowY + 4, ThemeColors.TEXT_LABEL);
        ctx.drawTextWithShadow(textRenderer, value,
            x + W - 88 - textRenderer.getWidth(value), rowY + 4, ThemeColors.TEXT_VALUE);
    }

    private void drawLabelBool(DrawContext ctx, String label, boolean value, int rowY) {
        ctx.drawTextWithShadow(textRenderer, label, x + 14, rowY + 4, ThemeColors.TEXT_LABEL);
    }

    // Slider nội bộ
    private class MaxBlockSlider extends SliderWidget {
        MaxBlockSlider(int x, int y, int w, int h) {
            super(x, y, w, h, Text.literal(String.valueOf(maxBlocks)), (maxBlocks - 1) / 255.0);
        }
        @Override protected void updateMessage() {
            maxBlocks = (int)(value * 255) + 1;
            setMessage(Text.literal(String.valueOf(maxBlocks)));
        }
        @Override protected void applyValue() { updateMessage(); }
    }

    private class CooldownSlider extends SliderWidget {
        CooldownSlider(int x, int y, int w, int h) {
            super(x, y, w, h, Text.literal(String.valueOf(cooldownTicks)), cooldownTicks / 100.0);
        }
        @Override protected void updateMessage() {
            cooldownTicks = (int)(value * 100);
            setMessage(Text.literal(String.valueOf(cooldownTicks)));
        }
        @Override protected void applyValue() { updateMessage(); }
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext ctx, int mx, int my, float delta) {}
}
