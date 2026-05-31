package com.tcveinminer.client.gui.screens.tabs;

import com.tcveinminer.client.config.ClientConfigManager;
import com.tcveinminer.client.gui.screens.MainMenuScreen;
import com.tcveinminer.client.gui.screens.MenuState;
import com.tcveinminer.client.gui.widgets.AmberButton;
import com.tcveinminer.client.hud.style.HudAnchor;
import com.tcveinminer.client.hud.style.HudStyle;
import com.tcveinminer.client.util.DrawHelper;
import com.tcveinminer.client.util.ThemeColors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class HudTab implements MenuTab {

    private AmberButton toggleHudBtn;
    private AmberButton styleBtn;
    private AmberButton anchorBtn;
    private AmberButton customXUp, customXDown, customYUp, customYDown;

    @Override
    public void init(MainMenuScreen parent, int cx, int cy, int cw, int ch) {
        MenuState state = parent.getState();

        toggleHudBtn = parent.addUIElement(new AmberButton(cx + 10, cy + 10, 150, 20, 
            Component.literal("Show HUD: " + (state.showHud ? "ON" : "OFF")), btn -> {
            state.showHud = !state.showHud;
            btn.setMessage(Component.literal("Show HUD: " + (state.showHud ? "ON" : "OFF")));
        }));

        styleBtn = parent.addUIElement(new AmberButton(cx + 10, cy + 35, 150, 20, 
            Component.literal("Style: " + state.hudStyle.name()), btn -> {
            HudStyle[] styles = HudStyle.values();
            int nextIdx = (state.hudStyle.ordinal() + 1) % styles.length;
            state.hudStyle = styles[nextIdx];
            btn.setMessage(Component.literal("Style: " + state.hudStyle.name()));
        }));

        anchorBtn = parent.addUIElement(new AmberButton(cx + 10, cy + 60, 150, 20, 
            Component.literal("Anchor: " + state.hudAnchor.name()), btn -> {
            HudAnchor[] anchors = HudAnchor.values();
            int nextIdx = (state.hudAnchor.ordinal() + 1) % anchors.length;
            state.hudAnchor = anchors[nextIdx];
            btn.setMessage(Component.literal("Anchor: " + state.hudAnchor.name()));
        }));

        // Custom X and Y adjustments
        customXDown = parent.addUIElement(new AmberButton(cx + 10, cy + 95, 20, 20, Component.literal("-"), btn -> {
            ClientConfigManager.instance.hudPositionX -= 5;
        }));
        customXUp = parent.addUIElement(new AmberButton(cx + 60, cy + 95, 20, 20, Component.literal("+"), btn -> {
            ClientConfigManager.instance.hudPositionX += 5;
        }));

        customYDown = parent.addUIElement(new AmberButton(cx + 90, cy + 95, 20, 20, Component.literal("-"), btn -> {
            ClientConfigManager.instance.hudPositionY -= 5;
        }));
        customYUp = parent.addUIElement(new AmberButton(cx + 140, cy + 95, 20, 20, Component.literal("+"), btn -> {
            ClientConfigManager.instance.hudPositionY += 5;
        }));
    }

    @Override
    public void render(GuiGraphics ctx, MainMenuScreen parent, int cx, int cy, int cw, int ch, int mouseX, int mouseY, float delta) {
        MenuState state = parent.getState();

        ctx.drawString(parent.getTextRenderer(), "Custom X", cx + 34, cy + 85, ThemeColors.TEXT_DIM, true);
        ctx.drawString(parent.getTextRenderer(), "Custom Y", cx + 114, cy + 85, ThemeColors.TEXT_DIM, true);
        
        ctx.drawString(parent.getTextRenderer(), String.valueOf(ClientConfigManager.instance.hudPositionX), cx + 38, cy + 101, ThemeColors.TEXT_VALUE, true);
        ctx.drawString(parent.getTextRenderer(), String.valueOf(ClientConfigManager.instance.hudPositionY), cx + 118, cy + 101, ThemeColors.TEXT_VALUE, true);

        // Render preview box
        int previewW = 120;
        int previewH = 80;
        int px = cx + cw - previewW - 10;
        int py = cy + 10;

        DrawHelper.drawSolidBorder(ctx, px, py, previewW, previewH, ThemeColors.BORDER_DEFAULT);
        ctx.fill(px + 1, py + 1, px + previewW - 1, py + previewH - 1, ThemeColors.BG_PANEL_INSET);

        ctx.drawString(parent.getTextRenderer(), "Screen Preview", px + 5, py + previewH + 5, ThemeColors.TEXT_DIM, true);

        int dotX = px, dotY = py;
        switch (state.hudAnchor) {
            case TOP_LEFT -> { dotX += 10; dotY += 10; }
            case TOP_CENTER -> { dotX += previewW/2; dotY += 10; }
            case TOP_RIGHT -> { dotX += previewW-10; dotY += 10; }
            case MIDDLE_LEFT -> { dotX += 10; dotY += previewH/2; }
            case MIDDLE_RIGHT -> { dotX += previewW-10; dotY += previewH/2; }
            case BOTTOM_LEFT -> { dotX += 10; dotY += previewH-10; }
            case BOTTOM_RIGHT -> { dotX += previewW-10; dotY += previewH-10; }
            case CUSTOM -> { dotX += previewW/2; dotY += previewH/2; }
        }

        ctx.fill(dotX - 3, dotY - 3, dotX + 3, dotY + 3, ThemeColors.EMERALD_BORDER);
        ctx.fill(dotX - 1, dotY - 1, dotX + 1, dotY + 1, ThemeColors.GOLD);
    }
}
