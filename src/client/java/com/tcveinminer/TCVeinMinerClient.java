package com.tcveinminer;

import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.gui.screens.RadialMenuScreen;
import com.tcveinminer.hud.VeinMinerHudOverlay;
import com.tcveinminer.logic.BlockHighlighter;
import com.tcveinminer.network.HoldKeyPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class TCVeinMinerClient implements ClientModInitializer {

    public static KeyBinding KEY_MINE;
    public static KeyBinding KEY_MENU;

    /** True khi player đang giữ phím đào. */
    public static boolean holdKeyDown = false;
    private static boolean lastHoldState = false;
    private static String lastShapeId = "";

    @Override
    public void onInitializeClient() {
        KEY_MINE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tc_veinminer.mine",
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

        // Đăng ký HUD overlay duy nhất
        HudRenderCallback.EVENT.register(new VeinMinerHudOverlay());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Mở menu radial
            while (KEY_MENU.wasPressed()) {
                client.setScreen(new RadialMenuScreen(client.currentScreen));
            }

            // Xác định trạng thái giữ phím
            if (client.currentScreen == null && client.getWindow() != null) {
                int boundKey = KeyBindingHelper.getBoundKeyOf(KEY_MINE).getCode();
                boolean physicallyHeld = InputUtil.isKeyPressed(client.getWindow().getHandle(), boundKey);

                // Nếu requireSneak = true, cần giữ Shift đồng thời
                if (ConfigManager.get().requireSneak && client.player != null) {
                    holdKeyDown = physicallyHeld && client.player.isSneaking();
                } else {
                    holdKeyDown = physicallyHeld;
                }
            } else {
                holdKeyDown = false;
            }

            // Gửi packet khi trạng thái thay đổi (hold state hoặc shape)
            var cfg = ConfigManager.get();
            String currentShapeId = cfg.miningShape.strategyId;
            if ((holdKeyDown != lastHoldState || !currentShapeId.equals(lastShapeId))
                    && client.getNetworkHandler() != null) {
                lastHoldState = holdKeyDown;
                lastShapeId   = currentShapeId;
                ClientPlayNetworking.send(
                        new HoldKeyPayload(holdKeyDown, currentShapeId, cfg.maxBlocks)
                );
            }
        });

        BlockHighlighter.register();
    }
}