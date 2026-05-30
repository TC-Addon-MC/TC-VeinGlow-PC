package com.tcveinminer.client;

import com.tcveinminer.client.config.ClientConfigManager;
import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.client.logic.BlockHighlighter;
import com.tcveinminer.client.network.ClientNetworkManager;
import com.tcveinminer.network.payload.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Common client-side engine. Loader-independent.
 * Loaders call these methods from their respective client entrypoints.
 */
public final class VeinGlowClient {

    public static net.minecraft.client.KeyMapping KEY_MINE;
    public static boolean holdKeyDown = false;
    public static boolean isMining = false;
    public static boolean isRadialMenuOpen = false;

    private static boolean lastHoldState = false;
    private static String lastShapeId = "";
    private static int lastMaxBlocks = -1;
    private static List<String> lastBlacklist = new ArrayList<>();

    private static boolean toggleActive = false;
    private static boolean lastKeyPressed = false;

    private static BlockPos lastTargetPos = null;
    private static net.minecraft.world.level.block.state.BlockState lastTargetState = null;
    private static boolean forceUpdateNextTick = false;
    private static boolean lastHoldStateForActivation = false;
    private static long lastActivationSendTime = 0;

    public static void init() {
        ClientConfigManager.load();
        BlockHighlighter.register();
    }

    public static void onJoin() {
        lastShapeId = ClientConfigManager.instance.currentShape;
        lastMaxBlocks = ClientConfigManager.instance.getEffectiveMaxBlocks();
        lastHoldState = holdKeyDown;
        lastBlacklist = new ArrayList<>(ClientConfigManager.instance.personalBlacklist);
        ClientNetworkManager.sendToServer(new HoldKeyData(holdKeyDown, lastShapeId, lastMaxBlocks,
                currentEquation(lastShapeId), lastBlacklist));
    }

    public static void onDisconnect() {
        holdKeyDown = false;
        lastHoldState = false;
        lastShapeId = "";
        lastMaxBlocks = -1;
        lastBlacklist = new ArrayList<>();
        toggleActive = false;
        lastKeyPressed = false;
        lastTargetPos = null;
        lastTargetState = null;
        forceUpdateNextTick = false;
        lastHoldStateForActivation = false;
    }

    public static void cycleShape(int direction) {
        MinecraftClient client = MinecraftClient.getInstance();
        var enabledShapes = new ArrayList<>(ClientConfigManager.instance.enabledShapes);
        if (enabledShapes.isEmpty()) return;

        int currentIndex = enabledShapes.indexOf(ClientConfigManager.instance.currentShape);
        if (currentIndex == -1) currentIndex = 0;

        int newIndex = (currentIndex + direction) % enabledShapes.size();
        if (newIndex < 0) newIndex += enabledShapes.size();

        String newShape = enabledShapes.get(newIndex);
        ClientConfigManager.instance.currentShape = newShape;
        ClientConfigManager.save();

        if (client.player != null) {
            minecraft.player.sendMessage(Component.translatable("hud.tcveinminer.cycle_notification", getShapeDisplayName(newShape)), true);
        }
    }

    public static void selectShapeByIndex(int index) {
        MinecraftClient client = MinecraftClient.getInstance();
        var enabledShapes = new ArrayList<>(ClientConfigManager.instance.enabledShapes);
        if (index >= 0 && index < enabledShapes.size()) {
            String newShape = enabledShapes.get(index);
            ClientConfigManager.instance.currentShape = newShape;
            ClientConfigManager.save();

            if (client.player != null) {
                minecraft.player.sendMessage(Component.translatable("hud.tcveinminer.cycle_notification", getShapeDisplayName(newShape)), true);
            }
        }
    }

    
    public static void handleConfigSync(ConfigSyncData data) {
        ClientConfigManager.instance.serverMaxBlocks = data.maxBlocks();
        ClientConfigManager.instance.serverBlacklist = new ArrayList<>(data.blacklistedBlocks());
    }

    public static void handleMiningState(MiningStateData data) {
        int state = data.state();
        isMining = (state == 1);
        com.tcveinminer.logic.HudNotifier.lastMined = data.broken();
        com.tcveinminer.logic.HudNotifier.lastMax = data.target();
        if (state == 2 || state == 3) {
            com.tcveinminer.logic.HudNotifier.notifyAt = System.currentTimeMillis() + 2500;
            com.tcveinminer.logic.HudNotifier.lastCancelled = (state == 3);
            forceUpdateNextTick = true;
        }
    }

    public static void handleActivationConfirm(ActivationConfirmData data) {
        BlockHighlighter.allowContinuous = data.allowContinuous();
    }

    public static void handleLookedAtBlock(LookedAtBlockData data) {
        BlockHighlighter.lookedAtBlock = data.pos().map(BlockPos::fromLong).orElse(null);
    }

    public static void handleFilterResult(FilterResultData data) {
        BlockHighlighter.allowHighlight = data.allowHighlight();
    }

    public static void handleHighlightDelta(HighlightDeltaData data) {
        BlockHighlighter.highlightBlocks.removeAll(data.removedBlocks().stream().map(BlockPos::fromLong).toList());
        BlockHighlighter.highlightBlocks.addAll(data.addedBlocks().stream().map(BlockPos::fromLong).toList());
        BlockHighlighter.highlightStyle = data.highlightStyle();
    }

    public static void handleHighlightBlockList(HighlightBlockListData data) {
        BlockHighlighter.highlightBlocks.clear();
        BlockHighlighter.highlightBlocks.addAll(data.blocks().stream().map(BlockPos::fromLong).toList());
        BlockHighlighter.highlightStyle = data.highlightStyle();
    }

    public static void onClientTick(boolean keyMinePressed, boolean menuKeyPressed) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        isRadialMenuOpen = menuKeyPressed;

        if (client.screen == null && minecraft.getWindow() != null) {
            boolean sneaking = minecraft.player.isShiftKeyDown();
            boolean justPressed = keyMinePressed && !lastKeyPressed;
            lastKeyPressed = keyMinePressed;

            int mode = ClientConfigManager.instance.activationMode;
            switch (mode) {
                case 1 -> holdKeyDown = keyMinePressed;
                case 2 -> holdKeyDown = keyMinePressed && sneaking;
                case 3 -> {
                    if (justPressed) toggleActive = !toggleActive;
                    holdKeyDown = toggleActive;
                }
                case 4 -> {
                    if (justPressed && sneaking) toggleActive = !toggleActive;
                    holdKeyDown = toggleActive;
                }
                default -> holdKeyDown = keyMinePressed;
            }
        } else {
            holdKeyDown = false;
            int mode = ClientConfigManager.instance.activationMode;
            if (mode == 1 || mode == 2) {
                lastKeyPressed = false;
            }
        }

        String currentShapeId = ClientConfigManager.instance.currentShape;
        int currentMaxBlocks = ClientConfigManager.instance.getEffectiveMaxBlocks();
        List<String> currentBlacklist = new ArrayList<>(ClientConfigManager.instance.personalBlacklist);
        
        boolean stateChanged = (holdKeyDown != lastHoldState)
                || (!currentShapeId.equals(lastShapeId))
                || (currentMaxBlocks != lastMaxBlocks) 
                || (!currentBlacklist.equals(lastBlacklist));

        if (stateChanged && ClientNetworkManager.canSend(HoldKeyData.class)) {
            lastHoldState = holdKeyDown;
            lastShapeId = currentShapeId;
            lastMaxBlocks = currentMaxBlocks;
            lastBlacklist = new ArrayList<>(currentBlacklist);
            
            ClientNetworkManager.sendToServer(new HoldKeyData(holdKeyDown, currentShapeId, currentMaxBlocks,
                    currentEquation(currentShapeId), currentBlacklist));
        }

        if (client.level != null) {
            BlockPos currentTarget = null;
            net.minecraft.world.level.block.state.BlockState currentState = null;
            HitResult hit = minecraft.hitResult;

            if (client.player.getMainHandItem().getItem() == net.minecraft.world.item.Items.BUCKET) {
                HitResult fluidHit = minecraft.level.clip(new net.minecraft.world.level.ClipContext(
                        minecraft.player.getCameraPosVec(1.0F),
                        minecraft.player.getCameraPosVec(1.0F).add(client.player.getRotationVec(1.0F).multiply(5.0)),
                        net.minecraft.world.level.ClipContext.Block.OUTLINE,
                        net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY,
                        minecraft.player));
                if (fluidHit != null && fluidHit.getType() == HitResult.Type.BLOCK) {
                    net.minecraft.world.level.block.state.BlockState fluidState = minecraft.level.getBlockState(((BlockHitResult) fluidHit).getBlockPos());
                    if (fluidState.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock && fluidState.getFluidState().isSource()) {
                        hit = fluidHit;
                    }
                }
            }

            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                currentTarget = ((BlockHitResult) hit).getBlockPos();
                currentState = minecraft.level.getBlockState(currentTarget);
            }

            boolean targetPosChanged = (currentTarget == null && lastTargetPos != null)
                    || (currentTarget != null && !currentTarget.equals(lastTargetPos));
            boolean targetStateChanged = (currentState == null && lastTargetState != null)
                    || (currentState != null && currentState != lastTargetState);
            
            boolean targetChanged = targetPosChanged || targetStateChanged;

            long now = System.currentTimeMillis();
            boolean forceSend = (holdKeyDown != lastHoldStateForActivation) || forceUpdateNextTick;
            
            if (forceSend || (targetChanged && now - lastActivationSendTime > 100)) {
                forceUpdateNextTick = false;
                lastTargetPos = currentTarget;
                lastTargetState = currentState;
                lastHoldStateForActivation = holdKeyDown;
                lastActivationSendTime = now;

                if (ClientNetworkManager.canSend(ActivationRequestData.class)) {
                    ClientNetworkManager.sendToServer(new ActivationRequestData(holdKeyDown, java.util.Optional.ofNullable(currentTarget).map(BlockPos::asLong)));
                }
            }
        }
    }

    public static Text getShapeDisplayName(String shapeId) {
        try {
            ModConfig.MiningShape shape = ModConfig.MiningShape.valueOf(shapeId);
            return Component.translatable("tc_veinminer.mode." + shape.name());
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return ClientConfigManager.instance.customShapes.stream()
                    .filter(entry -> shapeId.equals(entry.strategyId))
                    .map(entry -> Component.literal(entry.name))
                    .findFirst()
                    .orElse(Component.literal(shapeId));
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

    private VeinGlowClient() {}
}
