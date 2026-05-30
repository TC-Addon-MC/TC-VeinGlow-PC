package com.tcveinminer.client.gui.widgets;

import com.tcveinminer.client.config.ClientConfigManager;
import com.tcveinminer.client.gui.screens.MenuState;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public class MaxBlockSlider extends SliderWidget {
    private final MenuState state;

    public MaxBlockSlider(int x, int y, int w, int h, MenuState state) {
        super(x, y, w, h, Component.literal(String.valueOf(state.maxBlocks)), ClientConfigManager.instance.serverMaxBlocks <= 1 ? 0 : (state.maxBlocks - 1) / (double)(ClientConfigManager.instance.serverMaxBlocks - 1));
        this.state = state;
    }

    @Override
    protected void updateMessage() {
        state.maxBlocks = ClientConfigManager.instance.serverMaxBlocks <= 1 ? 1 : (int) (value * (ClientConfigManager.instance.serverMaxBlocks - 1)) + 1;
        setMessage(Component.literal(String.valueOf(state.maxBlocks)));
    }

    @Override
    protected void applyValue() {
        updateMessage();
    }
}
