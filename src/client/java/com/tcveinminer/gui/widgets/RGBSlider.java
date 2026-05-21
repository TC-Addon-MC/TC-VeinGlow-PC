package com.tcveinminer.gui.widgets;

import com.tcveinminer.gui.screens.MenuState;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class RGBSlider extends SliderWidget {
    private final MenuState state;
    private final char ch;

    public RGBSlider(int x, int y, int w, int h, char ch, MenuState state, double init, boolean disabled) {
        super(x, y, w, h, Text.literal(String.valueOf((int) (init * 255))), init);
        this.state = state;
        this.ch = ch;
        this.active = !disabled;
    }

    @Override
    protected void updateMessage() {
        int v = (int) (value * 255);
        if (ch == 'R') state.colorR = v;
        else if (ch == 'G') state.colorG = v;
        else state.colorB = v;
        setMessage(Text.literal(String.valueOf(v)));
    }

    @Override
    protected void applyValue() {
        updateMessage();
    }
}