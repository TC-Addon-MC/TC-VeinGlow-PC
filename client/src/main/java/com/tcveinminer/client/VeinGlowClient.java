package com.tcveinminer.client;

import com.tcveinminer.client.config.ClientConfigManager;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.client.logic.BlockHighlighter;
import com.tcveinminer.client.network.ClientNetworkManager;
import com.tcveinminer.network.payload.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Common client-side engine. Loader-independent.
 * Loaders call these methods from their respective client entrypoints.
 */
public final class VeinGlowClient {

    public static KeyMapping KEY_MINE;
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
        Minecraft client = Minecraft.getInstance();
        var enabledShapes = new ArrayList<>(ClientConfigManager.instance.enabledShapes);
        if (enabledShapes.isEmpty())
            return;

        int currentIndex = enabledShapes.indexOf(ClientConfigManager.instance.currentShape);
        if (currentIndex == -1)
            currentIndex = 0;

        int newIndex = (currentIndex + direction) % enabledShapes.size();
        if (newIndex < 0)
            newIndex += enabledShapes.size();

        String newShape = enabledShapes.get(newIndex);
        ClientConfigManager.instance.currentShape = newShape;
        ClientConfigManager.save();

        if (client.player != null) {
            client.player.displayClientMessage(
                    Component.translatable("hud.tcveinminer.cycle_notification", getShapeDisplayName(newShape)), true);
        }
    }

    public static void selectShapeByIndex(int index) {
        Minecraft client = Minecraft.getInstance();
        var enabledShapes = new ArrayList<>(ClientConfigManager.instance.enabledShapes);
        if (index >= 0 && index < enabledShapes.size()) {
            String newShape = enabledShapes.get(index);
            ClientConfigManager.instance.currentShape = newShape;
            ClientConfigManager.save();

            if (client.player != null) {
                client.player.displayClientMessage(
                        Component.translatable("hud.tcveinminer.cycle_notification", getShapeDisplayName(newShape)),
                        true);
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
        BlockHighlighter.lookedAtBlock = data.pos().map(BlockPos::of).orElse(null);
    }

    public static void handleFilterResult(FilterResultData data) {
        BlockHighlighter.allowHighlight = data.allowHighlight();
    }

    public static void handleHighlightDelta(HighlightDeltaData data) {
        BlockHighlighter.highlightBlocks.removeAll(data.removedBlocks().stream().map(BlockPos::of).toList());
        BlockHighlighter.highlightBlocks.addAll(data.addedBlocks().stream().map(BlockPos::of).toList());
        BlockHighlighter.highlightStyle = data.highlightStyle();
    }

    public static void handleHighlightBlockList(HighlightBlockListData data) {
        BlockHighlighter.highlightBlocks.clear();
        BlockHighlighter.highlightBlocks.addAll(data.blocks().stream().map(BlockPos::of).toList());
        BlockHighlighter.highlightStyle = data.highlightStyle();
    }

    public static void onClientTick(boolean keyMinePressed, boolean menuKeyPressed) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null)
            return;

        isRadialMenuOpen = menuKeyPressed;

        if (client.screen == null && client.getWindow() != null) {
            boolean sneaking = client.player.isCrouching();
            boolean justPressed = keyMinePressed && !lastKeyPressed;
            lastKeyPressed = keyMinePressed;

            int mode = ClientConfigManager.instance.activationMode;
            switch (mode) {
                case 1 -> holdKeyDown = keyMinePressed;
                case 2 -> holdKeyDown = keyMinePressed && sneaking;
                case 3 -> {
                    if (justPressed)
                        toggleActive = !toggleActive;
                    holdKeyDown = toggleActive;
                }
                case 4 -> {
                    if (justPressed && sneaking)
                        toggleActive = !toggleActive;
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
            HitResult hit = client.hitResult;

            if (client.player.getMainHandItem().getItem() == net.minecraft.world.item.Items.BUCKET) {
                HitResult fluidHit = client.level.clip(new net.minecraft.world.level.ClipContext(
                        client.player.getEyePosition(1.0F),
                        client.player.getEyePosition(1.0F).add(client.player.getViewVector(1.0F).scale(5.0)),
                        net.minecraft.world.level.ClipContext.Block.OUTLINE,
                        net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY,
                        client.player));
                if (fluidHit != null && fluidHit.getType() == HitResult.Type.BLOCK) {
                    net.minecraft.world.level.block.state.BlockState fluidState = client.level
                            .getBlockState(((BlockHitResult) fluidHit).getBlockPos());
                    if (fluidState.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock
                            && fluidState.getFluidState().isSource()) {
                        hit = fluidHit;
                    }
                }
            }

            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                currentTarget = ((BlockHitResult) hit).getBlockPos();
                currentState = client.level.getBlockState(currentTarget);
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
                    ClientNetworkManager.sendToServer(new ActivationRequestData(holdKeyDown,
                            java.util.Optional.ofNullable(currentTarget).map(BlockPos::asLong)));
                }
            }
        }
    }

    public static Component getShapeDisplayName(String shapeId) {
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
        if (shapeId == null || !shapeId.startsWith("custom:"))
            return "";
        return ClientConfigManager.instance.customShapes.stream()
                .filter(entry -> shapeId.equals(entry.strategyId))
                .map(entry -> entry.equation)
                .findFirst()
                .orElse("");
    }

    private VeinGlowClient() {
    }
}
