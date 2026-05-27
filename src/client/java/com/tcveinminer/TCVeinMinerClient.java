package com.tcveinminer;

import com.tcveinminer.config.ClientConfigManager;
import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.hud.VeinMinerHudOverlay;
import com.tcveinminer.logic.BlockHighlighter;
import com.tcveinminer.network.HoldKeyPayload;
import com.tcveinminer.network.ConfigSyncPayload;
import com.tcveinminer.network.MiningStatePayload;
import com.tcveinminer.network.ActivationRequestPayload;
import com.tcveinminer.network.ActivationConfirmPayload;
import com.tcveinminer.network.LookedAtBlockPayload;
import com.tcveinminer.network.FilterResultPayload;
import com.tcveinminer.network.HighlightBlockListPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public class TCVeinMinerClient implements ClientModInitializer {

    public static KeyBinding KEY_MINE;
    public static KeyBinding KEY_MENU;
    public static KeyBinding KEY_NEXT_SHAPE;
    public static KeyBinding KEY_PREV_SHAPE;
    public static final KeyBinding[] KEY_QUICK_SELECT = new KeyBinding[9];

    /** Trạng thái "đang kích hoạt" gửi lên server (kết quả sau khi xử lý activation mode). */
    public static boolean holdKeyDown = false;
    public static boolean isMining = false;
    public static boolean isRadialMenuOpen = false;

    private static boolean lastHoldState  = false;
    private static String  lastShapeId    = "";
    private static int     lastMaxBlocks  = -1;

    private static List<String> lastBlacklist = new ArrayList<>();

    /** Dùng cho TOGGLE/TOGGLE_SNEAK: trạng thái toggle hiện tại. */
    private static boolean toggleActive   = false;
    /** Để phát hiện edge "vừa nhấn" V (tránh lặp nhiều tick). */
    private static boolean lastKeyPressed = false;
    private static final java.util.Set<Integer> pressedKeys = new java.util.HashSet<>();

    private static BlockPos lastTargetPos = null;
    private static boolean lastHoldStateForActivation = false;
    private static long lastActivationSendTime = 0;

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

        KEY_NEXT_SHAPE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tc_veinminer.next_shape",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT,
                "key.categories.tc_veinminer"
        ));

        KEY_PREV_SHAPE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tc_veinminer.prev_shape",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT,
                "key.categories.tc_veinminer"
        ));

        for (int i = 0; i < 9; i++) {
            KEY_QUICK_SELECT[i] = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.tc_veinminer.quick_select_" + (i + 1),
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_1 + i,
                    "key.categories.tc_veinminer"
            ));
        }

        HudRenderCallback.EVENT.register((ctx, tick) -> {
            new VeinMinerHudOverlay().onHudRender(ctx, tick);
        });

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
            lastTargetPos  = null;
            lastHoldStateForActivation = false;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.currentScreen == null) {
                while (KEY_NEXT_SHAPE.wasPressed()) {
                    cycleShape(1);
                }
                while (KEY_PREV_SHAPE.wasPressed()) {
                    cycleShape(-1);
                }
                for (int i = 0; i < 9; i++) {
                    while (KEY_QUICK_SELECT[i].wasPressed()) {
                        selectShapeByIndex(i);
                    }
                }
            }
            
            // Mở menu radial khi bấm phím MENU
            int menuKey = KeyBindingHelper.getBoundKeyOf(KEY_MENU).getCode();
            boolean menuKeyPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), menuKey);
            if (menuKeyPressed && client.currentScreen == null && client.player != null) {
                client.setScreen(new com.tcveinminer.gui.screens.RadialMenuScreen(null));
            }
            isRadialMenuOpen = menuKeyPressed;

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

            // Phase 1: Check target block changes and send ActivationRequestPayload
            if (client.player != null && client.world != null) {
                BlockPos currentTarget = null;
                HitResult hit = client.crosshairTarget;
                if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                    currentTarget = ((BlockHitResult) hit).getBlockPos();
                }
                
                boolean targetChanged = (currentTarget == null && lastTargetPos != null) || (currentTarget != null && !currentTarget.equals(lastTargetPos));
                long now = System.currentTimeMillis();
                boolean forceSend = (holdKeyDown != lastHoldStateForActivation);
                if (forceSend || (targetChanged && now - lastActivationSendTime > 100)) {
                    lastTargetPos = currentTarget;
                    lastHoldStateForActivation = holdKeyDown;
                    lastActivationSendTime = now;
                    
                    if (ClientPlayNetworking.canSend(ActivationRequestPayload.ID)) {
                        ClientPlayNetworking.send(new ActivationRequestPayload(holdKeyDown, java.util.Optional.ofNullable(currentTarget)));
                    }
                }
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
                int state = payload.state();
                isMining = (state == 1);
                
                com.tcveinminer.logic.HudNotifier.lastMined = payload.broken();
                com.tcveinminer.logic.HudNotifier.lastMax = payload.target();
                
                if (state == 2 || state == 3) {
                    com.tcveinminer.logic.HudNotifier.notifyAt = System.currentTimeMillis() + 2500;
                    com.tcveinminer.logic.HudNotifier.lastCancelled = (state == 3);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ActivationConfirmPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                BlockHighlighter.allowContinuous = payload.allowContinuous();
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(LookedAtBlockPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                BlockHighlighter.lookedAtBlock = payload.pos();
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(FilterResultPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                BlockHighlighter.allowHighlight = payload.allowHighlight();
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(HighlightBlockListPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                BlockHighlighter.highlightBlocks = new java.util.HashSet<>(payload.blocks());
                BlockHighlighter.highlightStyle = payload.highlightStyle();
            });
        });

        BlockHighlighter.register();
    }



    private static void cycleShape(int direction) {
        var enabledShapes = new ArrayList<>(ClientConfigManager.instance.enabledShapes);
        if (enabledShapes.isEmpty()) return;
        
        int currentIndex = enabledShapes.indexOf(ClientConfigManager.instance.currentShape);
        if (currentIndex == -1) currentIndex = 0;
        
        int newIndex = (currentIndex + direction) % enabledShapes.size();
        if (newIndex < 0) newIndex += enabledShapes.size();
        
        ClientConfigManager.instance.currentShape = enabledShapes.get(newIndex);
        ClientConfigManager.save();
    }

    private static void selectShapeByIndex(int index) {
        var enabledShapes = new ArrayList<>(ClientConfigManager.instance.enabledShapes);
        if (index >= 0 && index < enabledShapes.size()) {
            ClientConfigManager.instance.currentShape = enabledShapes.get(index);
            ClientConfigManager.save();
        }
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
