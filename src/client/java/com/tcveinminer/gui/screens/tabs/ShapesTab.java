package com.tcveinminer.gui.screens.tabs;

import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.gui.CustomButton;
import com.tcveinminer.gui.screens.CustomShapeDesignerScreen;
import com.tcveinminer.gui.screens.MainMenuScreen;
import com.tcveinminer.util.DrawHelper;
import com.tcveinminer.util.ThemeColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import java.util.LinkedHashSet;
import java.util.Set;

public class ShapesTab implements MenuTab {
    private int shapeScroll = 0;

    // Biến lưu trữ tọa độ vùng làm việc (được cập nhật mỗi lần render)
    private int lastCx, lastCy, lastCw, lastCh;

    @Override
    public void init(MainMenuScreen screen, int cx, int cy, int cw, int ch) {
        int btnW = (cw - 6) / 2;

        // Nút "+ TỰ THIẾT KẾ"
        screen.addUIElement(new CustomButton(cx, cy + 2, btnW, 20, Text.literal("+ TỰ THIẾT KẾ MỚI (CUSTOM)"), btn -> {
            MinecraftClient.getInstance().setScreen(new CustomShapeDesignerScreen(screen));
        }));

        // Nút "Khôi phục"
        screen.addUIElement(new CustomButton(cx + btnW + 6, cy + 2, btnW, 20, Text.literal("Khôi phục mặc định"), btn -> {
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

            // Nút Bật/Tắt bên phải cùng
            screen.addUIElement(new CustomButton(cx + cw - 40, ry + 2, 36, 18, Text.literal(on ? "BẬT" : "TẮT"), btn -> {
                if (on) {
                    if (screen.getState().enabledShapes.size() > 1) screen.getState().enabledShapes.remove(s.name());
                } else screen.getState().enabledShapes.add(s.name());
                screen.rebuildMenu();
            }));
        }
    }

    @Override
    public void render(DrawContext ctx, MainMenuScreen screen, int cx, int cy, int cw, int ch, int mouseX, int mouseY, float delta) {
        // LƯU LẠI TỌA ĐỘ CHÍNH XÁC NHẤT
        this.lastCx = cx;
        this.lastCy = cy;
        this.lastCw = cw;
        this.lastCh = ch;

        ModConfig.MiningShape[] shapes = ModConfig.MiningShape.values();
        int maxV = (ch - 26) / 24;
        int start = Math.max(0, Math.min(shapeScroll, shapes.length - maxV));

        for (int i = start; i < Math.min(shapes.length, start + maxV); i++) {
            ModConfig.MiningShape s = shapes[i];
            int ry = cy + 26 + (i - start) * 24;
            boolean sel = s.name().equals(screen.getState().hoveredShapeId);

            // Vẽ nền hàng
            ctx.fill(cx, ry, cx + cw, ry + 22, sel ? 0x33F59E0B : DrawHelper.BG_CARD);
            DrawHelper.drawSolidBorder(ctx, cx, ry, cw, 22, sel ? ThemeColors.GOLD : DrawHelper.BORDER_MODERN);

            if (sel) ctx.fill(cx, ry + 4, cx + 2, ry + 18, ThemeColors.GOLD);

            ctx.drawTextWithShadow(screen.getTextRenderer(), s.label, cx + 8, ry + 7, sel ? ThemeColors.GOLD : 0xFFA0AEC0);
        }
    }

    @Override
    public boolean mouseClicked(MainMenuScreen screen, double mx, double my, int btn) {
        // SỬ DỤNG TỌA ĐỘ ĐÃ LƯU TỪ RENDER, KHÔNG TÍNH TOÁN LẠI NỮA
        int cx = lastCx;
        int cy = lastCy;
        int cw = lastCw;
        int ch = lastCh;

        // Đảm bảo không click ngoài vùng tab
        if (mx < cx || mx > cx + cw || my < cy || my > cy + ch) return false;

        ModConfig.MiningShape[] shapes = ModConfig.MiningShape.values();
        int maxV = (ch - 26) / 24;
        int start = Math.max(0, Math.min(shapeScroll, shapes.length - maxV));

        for (int i = start; i < Math.min(shapes.length, start + maxV); i++) {
            int ry = cy + 26 + (i - start) * 24;
            // Trừ đi 40px chiều rộng bên phải để không click nhầm vào nút BẬT/TẮT
            if (mx >= cx && mx <= cx + cw - 40 && my >= ry && my <= ry + 22) {
                screen.getState().hoveredShapeId = shapes[i].name();
                ConfigManager.get().miningShape = shapes[i];
                ConfigManager.save();
                return true; // Ngăn chặn click xuyên qua
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