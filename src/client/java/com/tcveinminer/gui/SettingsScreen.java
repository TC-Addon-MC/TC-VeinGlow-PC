package com.tcveinminer.gui;

import com.tcveinminer.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;

public class SettingsScreen extends BaseScreen {

    private int     maxBlocks;
    private boolean requireSneak, requireCorrectTool, consumeDurability, diagonalMining;
    private int     cooldownTicks;
    // SỬA: Đổi kiểu String thành ModConfig.MiningShape
    private ModConfig.MiningShape miningShape;

    private SliderWidget sliderMax, sliderCooldown;

    public SettingsScreen(Screen parent) {
        super(parent, "Settings", 310, 275);
        ModConfig c = ModConfig.get();
        maxBlocks = c.maxBlocks;
        requireSneak = c.requireSneak;
        requireCorrectTool = c.requireCorrectTool;
        consumeDurability = c.consumeDurability;
        diagonalMining = c.diagonalMining;
        cooldownTicks = c.cooldownTicks;
        miningShape = c.miningShape; // Bây giờ cả hai đều là kiểu Enum
    }

    @Override
    protected void initWidgets() {
        int bx = x + 200, by = y + HEADER_H + 2, step = 26;

        sliderMax = new SliderWidget(bx, by + 4, 100, 16, Text.literal(String.valueOf(maxBlocks)), (maxBlocks - 1) / 255.0) {
            @Override protected void updateMessage() { maxBlocks = (int)(value * 255) + 1; setMessage(Text.literal(String.valueOf(maxBlocks))); }
            @Override protected void applyValue()    { updateMessage(); }
        };
        addDrawableChild(sliderMax);

        addDrawableChild(toggleBtn(bx, by + step + 4,   "Sneak"));
        addDrawableChild(toggleBtn(bx, by + step * 2 + 4, "Tool"));
        addDrawableChild(toggleBtn(bx, by + step * 3 + 4, "Dura"));
        addDrawableChild(toggleBtn(bx, by + step * 4 + 4, "Diag"));

        sliderCooldown = new SliderWidget(bx, by + step * 5 + 4, 100, 16, Text.literal(String.valueOf(cooldownTicks)), cooldownTicks / 100.0) {
            @Override protected void updateMessage() { cooldownTicks = ((int)(value * 20)) * 5; setMessage(Text.literal(String.valueOf(cooldownTicks))); }
            @Override protected void applyValue()    { updateMessage(); }
        };
        addDrawableChild(sliderCooldown);

        // SỬA: Logic xoay vòng Enum MiningShape
        addDrawableChild(ButtonWidget.builder(Text.literal(shapeLabel()), b -> {
            miningShape = switch(miningShape) {
                case SAME_BLOCK -> ModConfig.MiningShape.SAME_TAG;
                case SAME_TAG   -> ModConfig.MiningShape.ALL;
                default         -> ModConfig.MiningShape.SAME_BLOCK;
            };
            b.setMessage(Text.literal(shapeLabel()));
        }).dimensions(bx, by + step * 6 + 4, 100, 16).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("LƯU & QUAY LẠI"), b -> {
            ModConfig c = ModConfig.get();
            c.maxBlocks = maxBlocks;
            c.requireSneak = requireSneak;
            c.requireCorrectTool = requireCorrectTool;
            c.consumeDurability = consumeDurability;
            c.diagonalMining = diagonalMining;
            c.cooldownTicks = cooldownTicks;
            c.miningShape = miningShape; // Lưu giá trị Enum
            ModConfig.save();
            client.setScreen(parent);
        }).dimensions(x + 10, y + H - 28, 140, 18).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("HỦY"), b -> client.setScreen(parent))
                .dimensions(x + W - 150, y + H - 28, 140, 18).build());
    }

    private ButtonWidget toggleBtn(int bx, int by, String field) {
        return ButtonWidget.builder(Text.literal(getF(field) ? "BẬT" : "TẮT"), b -> {
            setF(field, !getF(field));
            b.setMessage(Text.literal(getF(field) ? "BẬT" : "TẮT"));
        }).dimensions(bx, by, 60, 16).build();
    }

    private boolean getF(String f) {
        return switch(f) {
            case "Sneak" -> requireSneak;
            case "Tool"  -> requireCorrectTool;
            case "Dura"  -> consumeDurability;
            case "Diag"  -> diagonalMining;
            default      -> false;
        };
    }

    private void setF(String f, boolean v) {
        switch(f) {
            case "Sneak" -> requireSneak = v;
            case "Tool"  -> requireCorrectTool = v;
            case "Dura"  -> consumeDurability = v;
            case "Diag"  -> diagonalMining = v;
        }
    }

    // SỬA: Kiểm tra theo hằng số Enum thay vì chuỗi String
    private String shapeLabel() {
        return switch(miningShape) {
            case SAME_TAG   -> "Cùng tag ore";
            case ALL        -> "Tất cả block";
            default         -> "Chỉ cùng loại";
        };
    }

    @Override
    protected void renderContent(DrawContext ctx, int mx, int my, float delta) {
        drawTitle(ctx, "⚙ Cài Đặt");
        int lx = x + 14, ly = y + HEADER_H + 10, step = 26;
        String[] labels = {"Số block tối đa:", "Yêu cầu Sneak (Shift):", "Yêu cầu đúng công cụ:", "Hao mòn công cụ:", "Diagonal Mining:", "Cooldown (ticks):", "Hình dạng đào:"};
        for (int i = 0; i < labels.length; i++) label(ctx, labels[i], lx, ly + i * step + 8);
    }
}