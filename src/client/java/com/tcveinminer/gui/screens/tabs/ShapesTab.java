package com.tcveinminer.gui.screens.tabs;

import com.tcveinminer.config.ClientConfig;
import com.tcveinminer.config.ClientConfigManager;
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
import java.util.List;
import java.util.Set;

public class ShapesTab implements MenuTab {
    private int shapeScroll = 0;
    private int lastCx, lastCy, lastCw, lastCh;

    // Kiểm tra server có chặn custom shapes không (dùng hàm mẫu tạm thời)
    private static boolean isCustomBlockedByServer() {
        return ClientConfigManager.instance.serverDisabledShapes.contains("custom");
    }

    @Override
    public void init(MainMenuScreen screen, int cx, int cy, int cw, int ch) {
        int btnW = (cw - 6) / 2;

        screen.addUIElement(new CustomButton(cx, cy + 2, btnW, 20,
                Text.literal("+ TỰ THIẾT KẾ MỚI"), btn -> {
            MinecraftClient.getInstance().setScreen(
                    new CustomShapeDesignerScreen(screen, null));
        }));

        screen.addUIElement(new CustomButton(cx + btnW + 6, cy + 2, btnW, 20,
                Text.literal("Khôi phục mặc định"), btn -> {
            screen.getState().enabledShapes = new LinkedHashSet<>(
                    Set.of("FACE", "EDGES", "CORNERS", "TUNNEL_1x2", "AREA_3x3", "TREE_CAP"));
            screen.syncShapeStateToClientConfig();
            screen.rebuildMenu();
        }));

        // --- Tính tổng số hàng để phân bổ scroll ---
        ModConfig.MiningShape[] builtins = ModConfig.MiningShape.values();
        List<ClientConfig.CustomShapeEntry> customs = screen.getState().customShapes;
        int totalRows = builtins.length + customs.size();
        int maxV = (ch - 26) / 24;
        int start = Math.max(0, Math.min(shapeScroll, totalRows - maxV));

        int rowIndex = 0;

        // --- Hàng builtin ---
        for (ModConfig.MiningShape s : builtins) {
            if (rowIndex >= start && rowIndex < start + maxV) {
                int visualRow = rowIndex - start;
                boolean on = screen.getState().enabledShapes.contains(s.name());
                int ry = cy + 26 + visualRow * 24;
                screen.addUIElement(new CustomButton(cx + cw - 40, ry + 2, 36, 18,
                        Text.literal(on ? "BẬT" : "TẮT"), btn -> {
                    toggleShape(screen, s.name());
                }));
            }
            rowIndex++;
        }

        // --- Hàng custom ---
        for (int ci = 0; ci < customs.size(); ci++) {
            if (rowIndex >= start && rowIndex < start + maxV) {
                int visualRow = rowIndex - start;
                int ry = cy + 26 + visualRow * 24;
                final int finalCi = ci;
                ClientConfig.CustomShapeEntry entry = customs.get(ci);
                boolean on = screen.getState().enabledShapes.contains(entry.strategyId);

                screen.addUIElement(new CustomButton(cx + cw - 84, ry + 2, 40, 18,
                        Text.literal(on ? "BẬT" : "TẮT"), btn -> {
                    toggleShape(screen, customs.get(finalCi).strategyId);
                }));

                screen.addUIElement(new CustomButton(cx + cw - 40, ry + 2, 36, 18,
                        Text.literal("XÓA"), btn -> {
                    String deletedId = customs.get(finalCi).strategyId;
                    screen.getState().customShapes.remove(finalCi);
                    screen.getState().enabledShapes.remove(deletedId);
                    if (deletedId.equals(screen.getState().hoveredShapeId)) {
                        screen.getState().hoveredShapeId = "FACE";
                    }
                    screen.syncShapeStateToClientConfig();
                    screen.rebuildMenu();
                }));
            }
            rowIndex++;
        }
    }

    @Override
    public void render(DrawContext ctx, MainMenuScreen screen, int cx, int cy, int cw, int ch,
                       int mouseX, int mouseY, float delta) {
        this.lastCx = cx; this.lastCy = cy; this.lastCw = cw; this.lastCh = ch;

        // Cảnh báo server chặn custom
        if (isCustomBlockedByServer()) {
            ctx.drawTextWithShadow(screen.getTextRenderer(),
                    "⚠ Server đã tắt chế độ tùy chỉnh!",
                    cx, cy - 10, ThemeColors.TEXT_ERROR);
        }

        ModConfig.MiningShape[] builtins = ModConfig.MiningShape.values();
        List<ClientConfig.CustomShapeEntry> customs = screen.getState().customShapes;
        int totalRows = builtins.length + customs.size();
        int maxV = (ch - 26) / 24;
        int start = Math.max(0, Math.min(shapeScroll, totalRows - maxV));

        int rowIndex = 0;

        // --- Render builtin shapes ---
        for (ModConfig.MiningShape s : builtins) {
            if (rowIndex >= start && rowIndex < start + maxV) {
                int ry = cy + 26 + (rowIndex - start) * 24;
                boolean sel = s.name().equals(screen.getState().hoveredShapeId);

                ctx.fill(cx, ry, cx + cw, ry + 22, sel ? 0x33F59E0B : DrawHelper.BG_CARD);
                DrawHelper.drawSolidBorder(ctx, cx, ry, cw, 22,
                        sel ? ThemeColors.GOLD : DrawHelper.BORDER_MODERN);
                if (sel) ctx.fill(cx, ry + 4, cx + 2, ry + 18, ThemeColors.GOLD);

                ctx.drawTextWithShadow(screen.getTextRenderer(), s.icon + " " + s.label,
                        cx + 8, ry + 7, sel ? ThemeColors.GOLD : 0xFFA0AEC0);
            }
            rowIndex++;
        }

        // --- Render custom shapes ---
        for (ClientConfig.CustomShapeEntry entry : customs) {
            if (rowIndex >= start && rowIndex < start + maxV) {
                int ry = cy + 26 + (rowIndex - start) * 24;
                boolean sel = entry.strategyId.equals(screen.getState().hoveredShapeId);
                boolean blocked = isCustomBlockedByServer();

                // Màu tím cho custom, xám nếu bị server chặn
                int fillColor  = blocked ? 0x22888888 : (sel ? 0x338B5CF6 : DrawHelper.BG_CARD);
                int borderColor = blocked ? DrawHelper.BORDER_MODERN
                        : (sel ? ThemeColors.PURPLE : ThemeColors.PURPLE_BORDER_DIM);
                int textColor  = blocked ? ThemeColors.TEXT_DIM
                        : (sel ? ThemeColors.PURPLE_TEXT : 0xFFB39DDB);

                ctx.fill(cx, ry, cx + cw, ry + 22, fillColor);
                DrawHelper.drawSolidBorder(ctx, cx, ry, cw, 22, borderColor);
                if (sel && !blocked) ctx.fill(cx, ry + 4, cx + 2, ry + 18, ThemeColors.PURPLE);

                String label = "✦ " + entry.name;
                ctx.drawTextWithShadow(screen.getTextRenderer(), label, cx + 8, ry + 7, textColor);
            }
            rowIndex++;
        }
    }

    @Override
    public boolean mouseClicked(MainMenuScreen screen, double mx, double my, int btn) {
        int cx = lastCx, cy = lastCy, cw = lastCw, ch = lastCh;
        if (mx < cx || mx > cx + cw || my < cy || my > cy + ch) return false;

        ModConfig.MiningShape[] builtins = ModConfig.MiningShape.values();
        List<ClientConfig.CustomShapeEntry> customs = screen.getState().customShapes;
        int totalRows = builtins.length + customs.size();
        int maxV = (ch - 26) / 24;
        int start = Math.max(0, Math.min(shapeScroll, totalRows - maxV));

        int rowIndex = 0;

        // Click builtin
        for (ModConfig.MiningShape s : builtins) {
            if (rowIndex >= start && rowIndex < start + maxV) {
                int ry = cy + 26 + (rowIndex - start) * 24;
                if (mx >= cx && mx <= cx + cw - 40 && my >= ry && my <= ry + 22) {
                    toggleShape(screen, s.name());
                    return true;
                }
            }
            rowIndex++;
        }

        // Click custom
        for (ClientConfig.CustomShapeEntry entry : customs) {
            if (rowIndex >= start && rowIndex < start + maxV) {
                int ry = cy + 26 + (rowIndex - start) * 24;
                if (mx >= cx && mx <= cx + cw - 86 && my >= ry && my <= ry + 22) {
                    if (isCustomBlockedByServer()) return true; // Bị chặn, không cho chọn
                    toggleShape(screen, entry.strategyId);
                    return true;
                }
            }
            rowIndex++;
        }

        return false;
    }

    private void toggleShape(MainMenuScreen screen, String shapeId) {
        if (screen.getState().enabledShapes.contains(shapeId)) {
            if (screen.getState().enabledShapes.size() > 1) {
                screen.getState().enabledShapes.remove(shapeId);
            }
        } else {
            screen.getState().enabledShapes.add(shapeId);
        }
        screen.syncShapeStateToClientConfig();
        screen.rebuildMenu();
    }

    @Override
    public boolean mouseScrolled(MainMenuScreen screen, double mx, double my, double h, double v) {
        shapeScroll = Math.max(0, shapeScroll - (int) v);
        return true;
    }
}
