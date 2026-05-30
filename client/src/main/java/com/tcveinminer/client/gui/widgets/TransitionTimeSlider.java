package com.tcveinminer.client.gui.widgets;

import com.tcveinminer.client.gui.screens.MenuState;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class TransitionTimeSlider extends SliderWidget {
    private final MenuState state;

    public TransitionTimeSlider(int x, int y, int w, int h, MenuState state, float currentTime) {
        super(x, y, w, h, Text.literal(String.format("%.1fs", currentTime)), (currentTime - 0.1) / 9.9);
        this.state = state;
    }

    @Override
    protected void updateMessage() {
        float t = 0.1f + (float) (value * 9.9);
        state.colorTransitionTime = t;
        setMessage(Text.literal(String.format("%.1fs", t)));
    }

    @Override
    protected void applyValue() {
        updateMessage();
    }
}
