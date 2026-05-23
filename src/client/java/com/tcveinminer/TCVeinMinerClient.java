package com.tcveinminer;

import com.tcveinminer.config.ClientConfigManager;
import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.gui.screens.RadialMenuScreen;
import com.tcveinminer.hud.VeinMinerHudOverlay;
import com.tcveinminer.logic.BlockHighlighter;
import com.tcveinminer.network.HoldKeyPayload;
import com.tcveinminer.network.ConfigSyncPayload;
import com.tcveinminer.network.MiningStatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class TCVeinMinerClient implements ClientModInitializer {

    public static KeyBinding KEY_MINE;
    public static KeyBinding KEY_MENU;

    /** Trạng thái "đang kích hoạt" gửi lên server (kết quả sau khi xử lý activation mode). */
    public static boolean holdKeyDown = false;
    public static boolean isMining = false;

    private static boolean lastHoldState  = false;
    private static String  lastShapeId    = "";
    private static int     lastMaxBlocks  = -1;

    private static List<String> lastBlacklist = new ArrayList<>();

    /** Dùng cho TOGGLE/TOGGLE_SNEAK: trạng thái toggle hiện tại. */
    private static boolean toggleActive   = false;
    /** Để phát hiện edge "vừa nhấn" V (tránh lặp nhiều tick). */
    private static boolean lastKeyPressed = false;

    @Override
    public void onInitializeClient() {
        // Load config client ngay khi khởi động
        ClientConfigManager.load();

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

        HudRenderCallback.EVENT.register(new VeinMinerHudOverlay());

        // Đồng bộ trạng thái ngay khi join server
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            lastShapeId   = ClientConfigManager.instance.currentShape;
            lastMaxBlocks = ClientConfigManager.instance.getEffectiveMaxBlocks();
            lastHoldState = holdKeyDown;
            lastBlacklist = new ArrayList<>(ClientConfigManager.instance.personalBlacklist);
            ClientPlayNetworking.send(new HoldKeyPayload(holdKeyDown, lastShapeId, lastMaxBlocks, currentEquation(lastShapeId), lastBlacklist));
        });

        // Reset khi ngắt kết nối
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            holdKeyDown    = false;
            lastHoldState  = false;
            lastShapeId    = "";
            lastMaxBlocks  = -1;
            lastBlacklist  = new ArrayList<>();
            toggleActive   = false;
            lastKeyPressed = false;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Mở menu radial
            while (KEY_MENU.wasPressed()) {
                client.setScreen(new RadialMenuScreen(client.currentScreen));
            }

            // Xác định holdKeyDown theo activation mode
            if (client.currentScreen == null && client.getWindow() != null && client.player != null) {
                int boundKey = KeyBindingHelper.getBoundKeyOf(KEY_MINE).getCode();
                boolean physicallyHeld = InputUtil.isKeyPressed(client.getWindow().getHandle(), boundKey);
                boolean sneaking = client.player.isSneaking();
                // Edge "vừa nhấn xuống" (rising edge)
                boolean justPressed = physicallyHeld && !lastKeyPressed;
                lastKeyPressed = physicallyHeld;

                int mode = ClientConfigManager.instance.activationMode;
                switch (mode) {
                    case 1 -> // HOLD_KEY: giữ V
                        holdKeyDown = physicallyHeld;
                    case 2 -> // HOLD_SNEAK: giữ V + sneak
                        holdKeyDown = physicallyHeld && sneaking;
                    case 3 -> { // TOGGLE: nhấn V một lần bật/tắt
                        if (justPressed) toggleActive = !toggleActive;
                        holdKeyDown = toggleActive;
                    }
                    case 4 -> { // TOGGLE_SNEAK: sneak + nhấn V bật/tắt
                        if (justPressed && sneaking) toggleActive = !toggleActive;
                        holdKeyDown = toggleActive;
                    }
                    default -> holdKeyDown = physicallyHeld;
                }
            } else {
                holdKeyDown = false;
                // Toggle mode không tắt khi mở màn hình, chỉ HOLD reset
                int mode = ClientConfigManager.instance.activationMode;
                if (mode == 1 || mode == 2) {
                    lastKeyPressed = false;
                }
            }

            // Kiểm tra và gửi packet nếu có thay đổi
            String currentShapeId = ClientConfigManager.instance.currentShape;
            int currentMaxBlocks  = ClientConfigManager.instance.getEffectiveMaxBlocks();

            List<String> currentBlacklist = new ArrayList<>(ClientConfigManager.instance.personalBlacklist);
            boolean stateChanged = (holdKeyDown != lastHoldState)
                    || (!currentShapeId.equals(lastShapeId))
                    || (currentMaxBlocks != lastMaxBlocks) || (!currentBlacklist.equals(lastBlacklist));

            if (stateChanged && client.player != null && ClientPlayNetworking.canSend(HoldKeyPayload.ID)) {
                lastHoldState = holdKeyDown;
                lastShapeId   = currentShapeId;
                lastMaxBlocks = currentMaxBlocks;

                lastBlacklist = new ArrayList<>(currentBlacklist);
                ClientPlayNetworking.send(
                        new HoldKeyPayload(holdKeyDown, currentShapeId, currentMaxBlocks, currentEquation(currentShapeId), currentBlacklist)
                );
            }
        });

                ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                var ccfg = ClientConfigManager.instance;
                ccfg.serverMaxBlocks = payload.maxBlocks();
                ccfg.serverBlacklist = new ArrayList<>(payload.blacklistedBlocks());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(MiningStatePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                isMining = payload.isMining();
                if (!isMining) {
                    // When mining stops, we can allow highlight recalculation again if needed.
                    // The BlockHighlighter will handle the reset of its internal state.
                }
            });
        });

        BlockHighlighter.register();
    }

    private static String currentEquation(String shapeId) {
        if (shapeId == null || !shapeId.startsWith("custom:")) return "";
        return ClientConfigManager.instance.customShapes.stream()
                .filter(entry -> shapeId.equals(entry.strategyId))
                .map(entry -> entry.equation)
                .findFirst()
                .orElse("");
    }
}
