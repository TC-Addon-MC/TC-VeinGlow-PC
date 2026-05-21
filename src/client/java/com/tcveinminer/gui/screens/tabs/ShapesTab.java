package com.tcveinminer.gui.screens.tabs;

import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.gui.CustomButton;
import com.tcveinminer.gui.screens.MainMenuScreen;
import com.tcveinminer.util.DrawHelper;
import com.tcveinminer.util.ThemeColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import java.util.LinkedHashSet;
import java.util.Set;

public class ShapesTab implements MenuTab {
    private int shapeScroll = 0;

    @Override
    public void init(MainMenuScreen screen, int cx, int cy, int cw, int ch) {
        int btnW = (cw - 6) / 2;
        screen.addUIElement(new CustomButton(cx, cy + 2, btnW, 20, Text.literal("+ TỰ THIẾT KẾ"), btn -> {}));
        screen.addUIElement(new CustomButton(cx + btnW + 6, cy + 2, btnW, 20, Text.literal("Khôi phục"), btn -> {
            screen.getState().enabledShapes = new LinkedHashSet<>(Set.of("FACE", "EDGES", "CORNERS", "TUNNEL_1x2", "AREA_3x3", "TREE_CAP"));
            screen.rebuildMenu();
        }));

        ModConfig.MiningShape[] shapes = ModConfig.MiningShape.values();
        int maxV = (ch - 26) / 24;
        int start = Math.max(0, Math.min(shapeScroll, shapes.length - maxV));
        for (int i = start; i < Math.min(shapes.length, start + maxV); i++) {
            ModConfig.MiningShape s = shapes[i];
            boolean on = screen.getState().enabledShapes.contains(s.name());
            int ry = cy + 26 + (i - start) * 24;
            screen.addUIElement(new CustomButton(cx + cw - 36, ry + 2, 32, 18, Text.literal(on ? "BẬT" : "TẮT"), btn -> {
                if (on) {
                    if (screen.getState().enabledShapes.size() > 1) screen.getState().enabledShapes.remove(s.name());
                } else screen.getState().enabledShapes.add(s.name());
                screen.rebuildMenu();
            }));
        }
    }

    @Override
    public void render(DrawContext ctx, MainMenuScreen screen, int cx, int cy, int cw, int ch, int mouseX, int mouseY, float delta) {
        ModConfig.MiningShape[] shapes = ModConfig.MiningShape.values();
        int maxV = (ch - 26) / 24;
        int start = Math.max(0, Math.min(shapeScroll, shapes.length - maxV));
        for (int i = start; i < Math.min(shapes.length, start + maxV); i++) {
            ModConfig.MiningShape s = shapes[i];
            int ry = cy + 26 + (i - start) * 24;
            boolean sel = s.name().equals(screen.getState().hoveredShapeId);

            ctx.fill(cx, ry, cx + cw, ry + 22, sel ? ThemeColors.BG_ROW_SELECTED : ThemeColors.BG_ROW);
            DrawHelper.drawSolidBorder(ctx, cx, ry, cw, 22, sel ? ThemeColors.GOLD_BORDER : ThemeColors.BORDER_DEFAULT);
            if (sel) ctx.fill(cx, ry + 4, cx + 3, ry + 18, ThemeColors.GOLD);

            ctx.drawTextWithShadow(screen.getTextRenderer(), s.label, cx + 8, ry + 7, sel ? ThemeColors.GOLD : ThemeColors.TEXT_WHITE);
        }
    }

    @Override
    public boolean mouseClicked(MainMenuScreen screen, double mx, double my, int btn) {
        int cx = screen.getPx() + 10;
        int cy = screen.getPy() + 48; // HDR + TAB + PAD
        int cw = screen.getW() - 20;
        int ch = screen.getH() - 66; // 20 + 20 + 26

        ModConfig.MiningShape[] shapes = ModConfig.MiningShape.values();
        int maxV = (ch - 26) / 24;
        int start = Math.max(0, Math.min(shapeScroll, shapes.length - maxV));
        for (int i = start; i < Math.min(shapes.length, start + maxV); i++) {
            int ry = cy + 26 + (i - start) * 24;
            if (mx >= cx && mx <= cx + cw - 40 && my >= ry && my <= ry + 22) {
                screen.getState().hoveredShapeId = shapes[i].name();
                ConfigManager.get().miningShape = shapes[i];
                ConfigManager.save();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(MainMenuScreen screen, double mx, double my, double h, double v) {
        shapeScroll = Math.max(0, shapeScroll - (int) v);
        return true;
    }
}