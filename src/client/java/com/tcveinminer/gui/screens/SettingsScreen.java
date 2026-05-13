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

public class SettingsScreen extends Screen {

    private static final int W = 310, H = 280;
    private static final int HEADER_H = 24;

    private final Screen parent;
    private int x, y;

    // Working copy
    private int maxBlocks;
    private boolean requireSneak;
    private boolean requireCorrectTool;
    private boolean consumeDurability;
    private boolean diagonalMining;
    private int cooldownTicks;
    private ModConfig.MiningShape miningShape;

    private MaxBlockSlider maxBlockSlider;
    private CooldownSlider cooldownSlider;

    public SettingsScreen(Screen parent) {
        super(Text.literal("Settings"));
        this.parent = parent;
        ModConfig cfg = ConfigManager.get();
        maxBlocks         = cfg.maxBlocks;
        requireSneak      = cfg.requireSneak;
        requireCorrectTool= cfg.requireCorrectTool;
        consumeDurability = cfg.consumeDurability;
        diagonalMining    = cfg.diagonalMining;
        cooldownTicks     = cfg.cooldownTicks;
        miningShape       = cfg.miningShape;
    }

    @Override
    protected void init() {
        x = (width - W) / 2;
        y = (height - H) / 2;

        int rowY = y + HEADER_H + 12;
        int rowStep = 26;

        // Max blocks slider
        maxBlockSlider = new MaxBlockSlider(x + 160, rowY, 130, 16);
        addDrawableChild(maxBlockSlider);
        rowY += rowStep;

        // Toggles
        addToggle("Yêu cầu Sneak:", rowY, () -> requireSneak, v -> requireSneak = v); rowY += rowStep;
        addToggle("Yêu cầu đúng công cụ:", rowY, () -> requireCorrectTool, v -> requireCorrectTool = v); rowY += rowStep;
        addToggle("Hao mòn công cụ:", rowY, () -> consumeDurability, v -> consumeDurability = v); rowY += rowStep;
        addToggle("Diagonal Mining:", rowY, () -> diagonalMining, v -> diagonalMining = v); rowY += rowStep;

        // Cooldown slider
        cooldownSlider = new CooldownSlider(x + 160, rowY, 130, 16);
        addDrawableChild(cooldownSlider);
        rowY += rowStep;

        // Shape cycle button
        final int shapeRowY = rowY;
        addDrawableChild(new CustomButton(x + 160, shapeRowY, 130, 16,
                Text.literal(shapeLabel()), btn -> {
            miningShape = switch (miningShape) {
                case SAME_BLOCK -> ModConfig.MiningShape.SAME_TAG;
                case SAME_TAG   -> ModConfig.MiningShape.ALL;
                case ALL        -> ModConfig.MiningShape.SAME_BLOCK;
            };
            btn.setMessage(Text.literal(shapeLabel()));
        }));
        rowY += rowStep;

        // Save / Cancel
        addDrawableChild(new CustomButton(x + 10, y + H - 28, 140, 18,
                Text.literal("LƯU & QUAY LẠI"), btn -> {
            save();
            client.setScreen(parent);
        }));

        addDrawableChild(new CustomButton(x + W - 150, y + H - 28, 140, 18,
                Text.literal("HỦY"), btn -> client.setScreen(parent)));
    }

    private interface BoolGet { boolean get(); }
    private interface BoolSet { void set(boolean v); }

    private void addToggle(String label, int rowY, BoolGet get, BoolSet set) {
        addDrawableChild(new CustomButton(x + 160, rowY, 60, 16,
                Text.literal(get.get() ? "BẬT" : "TẮT"), btn -> {
            set.set(!get.get());
            btn.setMessage(Text.literal(get.get() ? "BẬT" : "TẮT"));
        }));
    }

    private String shapeLabel() {
        return switch (miningShape) {
            case SAME_BLOCK -> "Chỉ cùng loại";
            case SAME_TAG   -> "Cùng tag ore";
            case ALL        -> "Tất cả block";
        };
    }

    private void save() {
        ModConfig cfg = ConfigManager.get();
        cfg.maxBlocks          = maxBlocks;
        cfg.requireSneak       = requireSneak;
        cfg.requireCorrectTool = requireCorrectTool;
        cfg.consumeDurability  = consumeDurability;
        cfg.diagonalMining     = diagonalMining;
        cfg.cooldownTicks      = cooldownTicks;
        cfg.miningShape        = miningShape;
        ConfigManager.save();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        DrawHelper.drawPanel(ctx, x, y, W, H);
        DrawHelper.drawHeader(ctx, x, y, W, HEADER_H);
        ctx.drawTextWithShadow(textRenderer, "⚙ Cài Đặt", x + 12, y + 8, ThemeColors.TEXT_TITLE);

        int rowY = y + HEADER_H + 16;
        int rowStep = 26;

        drawLabel(ctx, "Số block tối đa:", rowY);
        ctx.drawTextWithShadow(textRenderer, String.valueOf(maxBlocks),
            x + 300, rowY, ThemeColors.TEXT_VALUE);
        rowY += rowStep;

        drawLabel(ctx, "Yêu cầu Sneak (Shift):", rowY);      rowY += rowStep;
        drawLabel(ctx, "Yêu cầu đúng công cụ:", rowY);       rowY += rowStep;
        drawLabel(ctx, "Hao mòn công cụ:", rowY);             rowY += rowStep;
        drawLabel(ctx, "Diagonal Mining:", rowY);              rowY += rowStep;

        drawLabel(ctx, "Cooldown (ticks):", rowY);
        ctx.drawTextWithShadow(textRenderer, String.valueOf(cooldownTicks),
            x + 300, rowY, ThemeColors.TEXT_VALUE);
        rowY += rowStep;

        drawLabel(ctx, "Hình dạng đào:", rowY);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawLabel(DrawContext ctx, String label, int rowY) {
        ctx.drawTextWithShadow(textRenderer, label, x + 14, rowY + 4, ThemeColors.TEXT_LABEL);
    }

    // Custom slider widgets
    private class MaxBlockSlider extends SliderWidget {
        MaxBlockSlider(int x, int y, int w, int h) {
            super(x, y, w, h, Text.literal(String.valueOf(maxBlocks)),
                (maxBlocks - 1) / 255.0);
        }
        @Override protected void updateMessage() {
            maxBlocks = (int)(value * 255) + 1;
            setMessage(Text.literal(String.valueOf(maxBlocks)));
        }
        @Override protected void applyValue() { updateMessage(); }
    }

    private class CooldownSlider extends SliderWidget {
        CooldownSlider(int x, int y, int w, int h) {
            super(x, y, w, h, Text.literal(String.valueOf(cooldownTicks)),
                cooldownTicks / 100.0);
        }
        @Override protected void updateMessage() {
            cooldownTicks = (int)(value * 20) * 5; // step 5
            setMessage(Text.literal(String.valueOf(cooldownTicks)));
        }
        @Override protected void applyValue() { updateMessage(); }
    }

    @Override
    public boolean shouldPause() { return false; }
}
