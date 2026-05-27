package com.tcveinminer.gui.widgets;

import com.tcveinminer.gui.screens.MenuState;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class ThicknessSlider extends SliderWidget {
    private final MenuState state;

    public ThicknessSlider(int x, int y, int w, int h, MenuState state, float currentThickness) {
        super(x, y, w, h, Text.literal(String.format("%.1f", currentThickness)), (currentThickness - 1.0) / 19.0);
        this.state = state;
    }

    @Override
    protected void updateMessage() {
        float t = 1.0f + (float) (value * 19.0);
        state.outlineThickness = t;
        setMessage(Text.literal(String.format("%.1f", t)));
    }

    @Override
    protected void applyValue() {
        updateMessage();
    }
}
