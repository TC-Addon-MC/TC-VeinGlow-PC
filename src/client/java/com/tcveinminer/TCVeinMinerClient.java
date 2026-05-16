package com.tcveinminer;

import com.tcveinminer.gui.screens.RadialMenuScreen;
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
import org.lwjgl.glfw.GLFW;

public class TCVeinMinerClient implements ClientModInitializer {
    private static boolean lastHoldState = false;

    // Đổi tên từ KEY_TOGGLE thành KEY_MINE để phản ánh đúng chức năng "đè để đào"
    public static KeyBinding KEY_MINE;
    public static KeyBinding KEY_MENU;

    /** Trạng thái thực tế: True khi player đang giữ phím chức năng. */
    public static boolean holdKeyDown = false;

    @Override
    public void onInitializeClient() {
        // Đăng ký phím bấm với ID mới rõ ràng hơn
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

        HudRenderCallback.EVENT.register(new VeinMinerHud());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // 1. Xử lý Menu (vẫn dùng wasPressed vì menu chỉ cần bấm một lần để mở)
            while (KEY_MENU.wasPressed()) {
                client.setScreen(new RadialMenuScreen(client.currentScreen));
            }

            // 2. Xử lý Logic Đè Phím (SỬA LỖI HARDCODE)
            if (client.currentScreen == null && client.getWindow() != null) {
                // FIX: Lấy mã phím thực tế mà người dùng đã gán trong Options
                int boundKey = KeyBindingHelper.getBoundKeyOf(KEY_MINE).getCode();
                holdKeyDown = InputUtil.isKeyPressed(client.getWindow().getHandle(), boundKey);
            } else {
                // Khi đang mở GUI (Chest, Menu...), tự động coi như nhả phím để an toàn
                holdKeyDown = false;
            }

            // 3. Gửi Packet đồng bộ lên Server (chỉ gửi khi trạng thái thay đổi)
            if (holdKeyDown != lastHoldState && client.getNetworkHandler() != null) {
                lastHoldState = holdKeyDown;
                com.tcveinminer.config.ModConfig cfg = com.tcveinminer.config.ConfigManager.get();
                ClientPlayNetworking.send(new HoldKeyPayload(holdKeyDown, cfg.miningShape.name(), cfg.maxBlocks));
            }
        });

        BlockHighlighter.register();
    }
}