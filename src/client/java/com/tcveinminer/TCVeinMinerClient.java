package com.tcveinminer;

import com.tcveinminer.config.ModConfig;
import com.tcveinminer.gui.MainMenuScreen;
import com.tcveinminer.hud.SessionStats;
import com.tcveinminer.hud.VeinMinerHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class TCVeinMinerClient implements ClientModInitializer {

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
            // Phím V — toggle
            while (KEY_TOGGLE.wasPressed()) {
                boolean now = !ModConfig.get().enabled;
                ModConfig.get().enabled = now;
                ModConfig.save();
                if (now) SessionStats.onModEnabled(); else SessionStats.onModDisabled();
                if (client.player != null)
                    client.player.sendMessage(
                        Text.literal("§d[VeinMiner]§r " + (now ? "§aBẬT" : "§cTẮT")), true);
            }

            // Phím G — mở menu
            while (KEY_MENU.wasPressed()) {
                client.setScreen(new MainMenuScreen(client.currentScreen));
            }

            // Cập nhật HOLD mode flag mỗi tick
            if (client.currentScreen == null && client.getWindow() != null) {
                holdKeyDown = InputUtil.isKeyPressed(
                    client.getWindow().getHandle(), GLFW.GLFW_KEY_V);
            } else {
                holdKeyDown = false;
            }
        });
    }
}
