package com.tcveinminer;

import com.tcveinminer.config.ModConfig;
import com.tcveinminer.gui.screens.MainMenuScreen;
import com.tcveinminer.hud.SessionStats;
import com.tcveinminer.hud.VeinMinerHud;
import com.tcveinminer.logic.BlockHighlighter;
import com.tcveinminer.network.HoldKeyPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class TCVeinMinerClient implements ClientModInitializer {
    private static boolean lastHoldState = false;
    public static KeyBinding KEY_TOGGLE;
    public static KeyBinding KEY_MENU;

    /** True khi player đang giữ phím V (dùng cho HOLD mode). */
    public static boolean holdKeyDown = false;

    @Override
    public void onInitializeClient() {
        KEY_TOGGLE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.tc_veinminer.toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.tc_veinminer"
        ));
        KEY_MENU = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.tc_veinminer.menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.tc_veinminer"
        ));

        HudRenderCallback.EVENT.register(new VeinMinerHud());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (KEY_MENU.wasPressed()) {
                client.setScreen(new MainMenuScreen(client.currentScreen));
            }

            if (client.currentScreen == null && client.getWindow() != null) {
                holdKeyDown = InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_V);
            } else {
                holdKeyDown = false;
            }

            // Gửi packet nếu trạng thái thay đổi
            if (holdKeyDown != lastHoldState && client.getNetworkHandler() != null) {
                lastHoldState = holdKeyDown;
                ClientPlayNetworking.send(new HoldKeyPayload(holdKeyDown));
            }
        });
        BlockHighlighter.register();
    }
}
