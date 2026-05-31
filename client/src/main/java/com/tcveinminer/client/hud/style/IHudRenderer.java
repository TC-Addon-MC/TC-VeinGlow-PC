package com.tcveinminer.client.hud.style;

import net.minecraft.client.gui.GuiGraphics;

public interface IHudRenderer {
    /**
     * @param ctx       GuiGraphics context
     * @param x         The root X coordinate (based on anchor + custom offset)
     * @param y         The root Y coordinate (based on anchor + custom offset)
     * @param anchor    The anchor used to position this HUD (useful for alignment like right-align)
     * @param isMining  Whether the player is actively mining a vein
     * @param holding   Whether the activation key/button is held
     * @param modeLabel The translated or raw string of the current shape mode
     * @param keyHint   The hint string explaining how to activate
     */
    void render(GuiGraphics ctx, int x, int y, HudAnchor anchor, boolean isMining, boolean holding, String modeLabel, String keyHint);
}
