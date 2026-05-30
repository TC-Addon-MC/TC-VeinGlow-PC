package com.tcveinminer.client.gui.screens.tabs;

import com.tcveinminer.client.config.ClientConfig;
import com.tcveinminer.client.config.ClientConfigManager;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.client.gui.CustomButton;
import com.tcveinminer.client.gui.screens.CustomShapeDesignerScreen;
import com.tcveinminer.client.gui.screens.MainMenuScreen;
import com.tcveinminer.client.util.DrawHelper;
import com.tcveinminer.client.util.ThemeColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ShapesTab implements MenuTab {
    private int shapeScroll = 0;
    private boolean isDraggingScroll = false;
    private int lastCx, lastCy, lastCw, lastCh;

    // Kiểm tra server có chặn custom shapes không (dùng hàm mẫu tạm thời)
    private static boolean isCustomBlockedByServer() {
        return ClientConfigManager.instance.serverDisabledShapes.contains("custom");
    }

    @Override
    public void init(MainMenuScreen screen, int cx, int cy, int cw, int ch) {
        int btnW = (cw - 6) / 2;

        screen.addUIElement(new CustomButton(cx, cy + 2, btnW, 20,
                Text.translatable("gui.tcveinminer.shapes.add_custom"), btn -> {
            MinecraftClient.getInstance().setScreen(
                    new CustomShapeDesignerScreen(screen, null));
        }));

        screen.addUIElement(new CustomButton(cx + btnW + 6, cy + 2, btnW, 20,
                Text.translatable("gui.tcveinminer.button.reset_defaults"), btn -> {
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
                        Text.translatable(on ? "gui.tcveinminer.button.on" : "gui.tcveinminer.button.off"), btn -> {
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
                        Text.translatable(on ? "gui.tcveinminer.button.on" : "gui.tcveinminer.button.off"), btn -> {
                    toggleShape(screen, customs.get(finalCi).strategyId);
                }));

                screen.addUIElement(new CustomButton(cx + cw - 40, ry + 2, 36, 18,
                        Text.translatable("gui.tcveinminer.button.delete"), btn -> {
                    String deletedId = customs.get(finalCi).strategyId;
                    screen.getState().customShapes.remove(finalCi);
                    screen.getState().enabledShapes.remove(deletedId);
                    if (deletedId.equals(screen.getState().selectedShapeId)) {
                        screen.getState().selectedShapeId = "FACE";
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
                    Text.translatable("gui.tcveinminer.error.server_blocked_custom").getString(),
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

                ctx.fill(cx, ry, cx + cw, ry + 22, DrawHelper.BG_CARD);
                DrawHelper.drawSolidBorder(ctx, cx, ry, cw, 22, DrawHelper.BORDER_MODERN);

                ctx.drawTextWithShadow(screen.getTextRenderer(), Text.translatable("tc_veinminer.mode." + s.name()).getString(),
                        cx + 8, ry + 7, 0xFFA0AEC0);
            }
            rowIndex++;
        }

        // --- Render custom shapes ---
        for (ClientConfig.CustomShapeEntry entry : customs) {
            if (rowIndex >= start && rowIndex < start + maxV) {
                int ry = cy + 26 + (rowIndex - start) * 24;
                boolean blocked = isCustomBlockedByServer();

                // Màu tím cho custom, xám nếu bị server chặn
                int fillColor  = blocked ? 0x22888888 : DrawHelper.BG_CARD;
                int borderColor = blocked ? DrawHelper.BORDER_MODERN : ThemeColors.PURPLE_BORDER_DIM;
                int textColor  = blocked ? ThemeColors.TEXT_DIM : 0xFFB39DDB;

                ctx.fill(cx, ry, cx + cw, ry + 22, fillColor);
                DrawHelper.drawSolidBorder(ctx, cx, ry, cw, 22, borderColor);

                String label = entry.name;
                ctx.drawTextWithShadow(screen.getTextRenderer(), label, cx + 8, ry + 7, textColor);
            }
            rowIndex++;
        }

        if (totalRows > maxV) {
            int barX = cx + cw - 3;
            int barH = ch - 26;
            int trackY = cy + 26;
            ctx.fill(barX, trackY, barX + 3, trackY + barH, 0x33FFFFFF);

            float thumbRatio = (float) maxV / totalRows;
            float thumbOffset = (float) start / totalRows;
            int thumbH = Math.max(12, (int)(barH * thumbRatio));
            int thumbY = trackY + (int)((barH - thumbH) * thumbOffset / Math.max(1.0f, 1.0f - thumbRatio));
            ctx.fill(barX, thumbY, barX + 3, thumbY + thumbH, 0xAAA0AEC0);
        }
    }

    @Override
    public boolean mouseClicked(MainMenuScreen screen, double mx, double my, int btn) {
        int cx = lastCx, cy = lastCy, cw = lastCw, ch = lastCh;
        
        // Detect scrollbar click
        if (mx >= cx + cw - 10 && mx <= cx + cw && my >= cy + 26 && my <= cy + ch) {
            isDraggingScroll = true;
            return true;
        }
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
    public boolean mouseDragged(MainMenuScreen screen, double mx, double my, int btn, double dx, double dy) {
        if (isDraggingScroll) {
            int barH = lastCh - 26;
            if (barH <= 0) return false;

            int totalRows = ModConfig.MiningShape.values().length + screen.getState().customShapes.size();
            int maxV = barH / 24;

            if (totalRows > maxV) {
                float relativeY = (float) (my - (lastCy + 26));
                float thumbRatio = (float) maxV / totalRows;
                int thumbH = Math.max(12, (int) (barH * thumbRatio));
                
                // Trừ đi nửa chiều cao thumb để tâm thumb bám theo chuột
                float scrollPos = (relativeY - thumbH / 2.0f) / (barH - thumbH);
                shapeScroll = (int) (scrollPos * (totalRows - maxV));
                shapeScroll = Math.max(0, Math.min(shapeScroll, totalRows - maxV));
                screen.rebuildMenu();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MainMenuScreen screen, double mx, double my, int btn) {
        isDraggingScroll = false;
        return false;
    }

    @Override
    public boolean mouseScrolled(MainMenuScreen screen, double mx, double my, double h, double v) {
        shapeScroll = Math.max(0, shapeScroll - (int) v);
        screen.rebuildMenu();
        return true;
    }
}

