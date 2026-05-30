package com.tcveinminer.client.gui.widgets;

import com.tcveinminer.client.gui.screens.MenuState;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public class ThicknessSlider extends AbstractSliderButton {
    private final MenuState state;

    public ThicknessSlider(int x, int y, int w, int h, MenuState state, float currentThickness) {
        super(x, y, w, h, Component.literal(String.format("%.1f", currentThickness)), (currentThickness - 1.0) / 19.0);
        this.state = state;
    }

    @Override
    protected void updateMessage() {
        float t = 1.0f + (float) (value * 19.0);
        state.outlineThickness = t;
        setMessage(Component.literal(String.format("%.1f", t)));
    }

    @Override
    protected void applyValue() {
        updateMessage();
    }
}
