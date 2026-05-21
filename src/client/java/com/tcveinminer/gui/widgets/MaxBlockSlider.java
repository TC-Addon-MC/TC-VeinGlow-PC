package com.tcveinminer.gui.widgets;

import com.tcveinminer.gui.screens.MenuState;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class MaxBlockSlider extends SliderWidget {
    private final MenuState state;

    public MaxBlockSlider(int x, int y, int w, int h, MenuState state) {
        super(x, y, w, h, Text.literal(String.valueOf(state.maxBlocks)), (state.maxBlocks - 1) / 127.0);
        this.state = state;
    }

    @Override
    protected void updateMessage() {
        state.maxBlocks = (int) (value * 127) + 1;
        setMessage(Text.literal(String.valueOf(state.maxBlocks)));
    }

    @Override
    protected void applyValue() {
        updateMessage();
    }
}