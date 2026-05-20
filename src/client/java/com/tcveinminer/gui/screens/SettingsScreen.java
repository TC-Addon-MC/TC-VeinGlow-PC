package com.tcveinminer.gui.screens;

import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.gui.CustomButton;
import com.tcveinminer.util.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.LinkedHashSet;
import java.util.Set;

public class SettingsScreen extends Screen {

    private static final int W = LayoutUtil.SCREEN_W_MD;
    private static final int H = 320;
    private static final int TAB_GENERAL = 0, TAB_SHAPES = 1;
    private int activeTab = TAB_GENERAL;

    private final Screen parent;
    private int x, y;

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
        x = (width  - W) / 2;
        y = (height - H) / 2;
        rebuildWidgets();
    }

    private void rebuildWidgets() {
        clearChildren();

        int tabY = y + LayoutUtil.HEADER_H + 2;
        addDrawableChild(new CustomButton(x + LayoutUtil.TAB_PAD_X, tabY, 140, LayoutUtil.TAB_H,
                Text.literal("⚙ Cài đặt chung"),
                btn -> { activeTab = TAB_GENERAL; rebuildWidgets(); }));
        addDrawableChild(new CustomButton(x + 160, tabY, 140, LayoutUtil.TAB_H,
                Text.literal("⛏ Chế độ đào"),
                btn -> { activeTab = TAB_SHAPES; rebuildWidgets(); }));

        int rowY    = LayoutUtil.contentY(y, true);
        int rowStep = LayoutUtil.ROW_STEP;

        if (activeTab == TAB_GENERAL) {
            addDrawableChild(new MaxBlockSlider(x + 180, rowY, 130, LayoutUtil.ROW_H));
            rowY += rowStep;
            addToggleRow(rowY, () -> requireSneak,       v -> requireSneak       = v); rowY += rowStep;
            addToggleRow(rowY, () -> requireCorrectTool, v -> requireCorrectTool = v); rowY += rowStep;
            addToggleRow(rowY, () -> consumeDurability,  v -> consumeDurability  = v); rowY += rowStep;
            addToggleRow(rowY, () -> diagonalMining,     v -> diagonalMining     = v); rowY += rowStep;
            addToggleRow(rowY, () -> showHud,            v -> showHud            = v); rowY += rowStep;
            addDrawableChild(new CooldownSlider(x + 180, rowY, 130, LayoutUtil.ROW_H));
        } else {
            int shapeY = rowY + LayoutUtil.ROW_STEP_SM;
            for (ModConfig.MiningShape shape : ModConfig.MiningShape.values()) {
                final ModConfig.MiningShape s = shape;
                boolean on = enabledShapes.contains(shape.name());
                addDrawableChild(new CustomButton(x + W - 80, shapeY, 64, LayoutUtil.ROW_H,
                        Text.literal(on ? "✓ BẬT" : "✗ TẮT"), btn -> {
                            if (enabledShapes.contains(s.name())) {
                                if (enabledShapes.size() > 1) enabledShapes.remove(s.name());
                            } else {
                                enabledShapes.add(s.name());
                            }
                            rebuildWidgets();
                        }));
                shapeY += 22;
                if (shapeY > y + H - 50) break;
            }
        }

        int footerY = LayoutUtil.footerY(y, H);
        addDrawableChild(new CustomButton(x + LayoutUtil.TAB_PAD_X, footerY,
                LayoutUtil.BTN_W_HALF, LayoutUtil.BTN_H,
                Text.literal("💾 LƯU & QUAY LẠI"),
                btn -> { save(); client.setScreen(parent); }));
        addDrawableChild(new CustomButton(x + W - LayoutUtil.BTN_W_HALF - LayoutUtil.TAB_PAD_X, footerY,
                LayoutUtil.BTN_W_HALF, LayoutUtil.BTN_H,
                Text.literal("✕ HỦY"),
                btn -> client.setScreen(parent)));
    }

    private void addToggleRow(int rowY, BoolGet get, BoolSet set) {
        addDrawableChild(new CustomButton(x + W - 80, rowY, 64, LayoutUtil.ROW_H,
                Text.literal(get.get() ? "✓ BẬT" : "✗ TẮT"), btn -> {
                    set.set(!get.get());
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
        PanelDrawUtil.panel(ctx, x, y, W, H);
        DrawHelper.drawHeader(ctx, x, y, W, LayoutUtil.HEADER_H);

        ctx.drawTextWithShadow(textRenderer, "⚙ Cài Đặt TC-VeinMiner",
                x + LayoutUtil.HEADER_PAD_X, y + LayoutUtil.HEADER_PAD_Y,
                ThemeColors.TEXT_TITLE);

        // Tab underline active indicator
        int tabY = y + LayoutUtil.HEADER_H + 2;
        if (activeTab == TAB_GENERAL)
            ctx.fill(x + LayoutUtil.TAB_PAD_X, tabY + LayoutUtil.TAB_H - 2, x + 150, tabY + LayoutUtil.TAB_H, ThemeColors.GOLD);
        else
            ctx.fill(x + 160, tabY + LayoutUtil.TAB_H - 2, x + 300, tabY + LayoutUtil.TAB_H, ThemeColors.GOLD);

        // Section card
        int cardY = y + LayoutUtil.HEADER_H + LayoutUtil.TAB_H + 6;
        PanelDrawUtil.card(ctx, x + 8, cardY, W - 16, H - LayoutUtil.HEADER_H - LayoutUtil.TAB_H - 50);

        int rowY    = LayoutUtil.contentY(y, true);
        int rowStep = LayoutUtil.ROW_STEP;

        if (activeTab == TAB_GENERAL) {
            drawRow(ctx, "Số block tối đa:",        String.valueOf(maxBlocks),     rowY); rowY += rowStep;
            drawRowBool(ctx, "Yêu cầu Sneak:",         requireSneak,       rowY); rowY += rowStep;
            drawRowBool(ctx, "Yêu cầu đúng công cụ:",  requireCorrectTool, rowY); rowY += rowStep;
            drawRowBool(ctx, "Hao mòn công cụ:",        consumeDurability,  rowY); rowY += rowStep;
            drawRowBool(ctx, "Diagonal Mining:",         diagonalMining,     rowY); rowY += rowStep;
            drawRowBool(ctx, "Hiện HUD:",                showHud,            rowY); rowY += rowStep;
            drawRow(ctx, "Cooldown (ticks):",        String.valueOf(cooldownTicks), rowY);
        } else {
            ctx.drawTextWithShadow(textRenderer, "Chọn chế độ hiện trong Radial Menu:",
                    x + LayoutUtil.CONTENT_PAD_X, rowY, ThemeColors.TEXT_LABEL);
            rowY += LayoutUtil.ROW_STEP_SM;
            for (ModConfig.MiningShape shape : ModConfig.MiningShape.values()) {
                boolean on = enabledShapes.contains(shape.name());
                ctx.drawTextWithShadow(textRenderer, shape.icon + "  " + shape.label,
                        x + LayoutUtil.CONTENT_PAD_X, rowY + 4,
                        on ? ThemeColors.EMERALD_TEXT : ThemeColors.TEXT_LABEL);
                rowY += 22;
                if (rowY > y + H - 50) break;
            }
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawRow(DrawContext ctx, String label, String value, int rowY) {
        ctx.drawTextWithShadow(textRenderer, label,
                x + LayoutUtil.CONTENT_PAD_X, rowY + 4, ThemeColors.TEXT_LABEL);
        ctx.drawTextWithShadow(textRenderer, value,
                x + W - 88 - textRenderer.getWidth(value), rowY + 4, ThemeColors.TEXT_VALUE);
    }

    private void drawRowBool(DrawContext ctx, String label, boolean value, int rowY) {
        ctx.drawTextWithShadow(textRenderer, label,
                x + LayoutUtil.CONTENT_PAD_X, rowY + 4, ThemeColors.TEXT_LABEL);
        // Toggle indicator vẽ kèm widget từ ToggleDrawUtil
        ToggleDrawUtil.draw(ctx, x + W - 80, rowY, 64, LayoutUtil.ROW_H, value);
    }

    // ── Slider widgets ────────────────────────────────────────────────────

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
