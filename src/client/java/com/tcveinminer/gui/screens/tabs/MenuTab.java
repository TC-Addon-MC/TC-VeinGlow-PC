package com.tcveinminer.gui.screens.tabs;

import com.tcveinminer.gui.screens.MainMenuScreen;
import net.minecraft.client.gui.DrawContext;

public interface MenuTab {
    void init(MainMenuScreen screen, int cx, int cy, int cw, int ch);
    void render(DrawContext ctx, MainMenuScreen screen, int cx, int cy, int cw, int ch, int mouseX, int mouseY, float delta);
    default boolean mouseClicked(MainMenuScreen screen, double mx, double my, int btn) { return false; }
    default boolean mouseScrolled(MainMenuScreen screen, double mx, double my, double h, double v) { return false; }
}