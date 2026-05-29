package com.tcveinminer.client.gui.widgets;

import com.tcveinminer.client.gui.screens.MenuState;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class ProtectThresholdSlider extends SliderWidget {
    private final MenuState state;

    public ProtectThresholdSlider(int x, int y, int w, int h, MenuState state) {
        super(x, y, w, h, Text.literal(String.valueOf(state.toolProtectThreshold)), state.toolProtectThreshold / 100.0);
        this.state = state;
    }

    @Override
    protected void updateMessage() {
        state.toolProtectThreshold = (int) (value * 100);
        if (state.toolProtectThreshold < 1) state.toolProtectThreshold = 1;
        setMessage(Text.literal(String.valueOf(state.toolProtectThreshold)));
    }

    @Override
    protected void applyValue() {
        updateMessage();
    }
}
