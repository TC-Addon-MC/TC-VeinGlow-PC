package com.tcveinminer.engine.right;

import com.tcveinminer.api.TCVeinMinerEvents;
import com.tcveinminer.api.event.SessionStartEvent;
import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.engine.AbstractActionEngine;
import com.tcveinminer.engine.action.ActionContext;
import com.tcveinminer.engine.action.ActionType;
import com.tcveinminer.engine.capability.CapabilityRegistry;
import com.tcveinminer.engine.filter.RightClickFilterPipeline;
import com.tcveinminer.engine.session.ActionSession;
import com.tcveinminer.engine.state.EngineState;
import com.tcveinminer.engine.strategy.FilterModeManager;
import com.tcveinminer.engine.strategy.MiningStrategy;
import com.tcveinminer.engine.strategy.StrategyRegistry;
import com.tcveinminer.engine.traversal.OrientationContext;
import com.tcveinminer.network.NetworkManager;
import com.tcveinminer.network.payload.MiningStateData;
import com.tcveinminer.util.SessionStats;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.*;

/**
 * Right-click engine — handles INTERACT, HARVEST, PLANT, HOE_TILL, FLUID_SCOOP actions.
 * <p>
 * NEVER touches: break block, tree cap, mining strategies.
 * <p>
 * Uses {@link CapabilityRegistry} for action resolution (no hardcoded if/else).
 * <p>
 * Entry point: {@link #onInteractTrigger(Player, ServerLevel, InteractionHand, BlockHitResult)}
 * from {@code UseBlockCallback}.
 */
public final class RightClickEngine extends AbstractActionEngine {

    @Override
    protected String getSourceId() { return "RIGHT"; }

    // ── Entry Point ───────────────────────────────────────────────────────

    /**
     * Called when a player right-clicks a block (from UseBlockCallback).
     *
     * @return true if the mod handled the interaction (cancel vanilla)
     */
    private static final ThreadLocal<Boolean> INTERNAL_PROCESSING = ThreadLocal.withInitial(() -> false);

    public static boolean isProcessingInternal() {
        return INTERNAL_PROCESSING.get();
    }

    public static void setProcessingInternal(boolean val) {
        INTERNAL_PROCESSING.set(val);
    }

    // ── Entry Point ───────────────────────────────────────────────────────

    /**
     * Called when a player right-clicks a block (from UseBlockCallback).
     *
     * @return true if the mod handled the interaction (cancel vanilla)
     */
    public boolean onInteractTrigger(Player player, ServerLevel world,
                                      InteractionHand InteractionHand, BlockHitResult hitResult) {
        // Re-entry guard
        if (session != null && session.isProcessing()) return false;
        if (isProcessingInternal()) return false;

        // Already processing a session? Don't interrupt
        if (stateMachine.is(EngineState.PROCESSING)) return false;

        if (!stateMachine.is(EngineState.IDLE) && !stateMachine.is(EngineState.PREVIEW)) return false;

        ModConfig c = ConfigManager.get();
        if (!c.enabled) return false;
        
        boolean holdingV = com.tcveinminer.engine.state.PlayerStateRegistry.isHoldingKey(player.getUUID());

        BlockPos origin = hitResult.getBlockPos();
        BlockState originState = world.getBlockState(origin);

        if (c.requireSneak && !player.isShiftKeyDown()) return false;
        if (!checkCooldown(player.getUUID(), world.getGameTime(), c)) return false;

        // Resolve action via capability system
        ItemStack heldItem = player.getItemInHand(InteractionHand);
        ActionType actionType = CapabilityRegistry.resolve(player, heldItem, originState, InteractionHand);

        // USE_ITEM and VANILLA_FALLBACK → let vanilla handle
        if (actionType == ActionType.VANILLA_FALLBACK || actionType == ActionType.USE_ITEM) {
            return false;
        }

        // Allow single block processing if not holding V, but ONLY for CROP_HARVEST
        if (!holdingV && actionType != ActionType.CROP_HARVEST) {
            return false;
        }

        // Validate
        if (!validateTrigger(player, world, origin, originState, c)) return false;

        Set<String> activeBlacklist = mergedBlacklist;
        if (activeBlacklist.contains(blockId(originState))) return false;

        // Check skill toggle
        if (!isSkillEnabled(actionType, c)) return false;

        // Select strategy (FACE for right-click)
        MiningStrategy strategy = selectStrategy(actionType);
        int maxBlocksToMine = holdingV ? (this.playerMaxBlocks - 1) : 0;

        // Bucket special: limit by available buckets
        if (actionType == ActionType.FLUID_SCOOP) {
            int availableBuckets = countBuckets(player);
            maxBlocksToMine = Math.max(0, Math.min(maxBlocksToMine, availableBuckets - 1));
            strategy = StrategyRegistry.get("FACE");
        }

        // Build filter
        FilterModeManager.FilterCache cache = new FilterModeManager.FilterCache();
        FilterModeManager.BlockFilter filter = buildFilter(actionType, strategy, maxBlocksToMine);

        if (actionType == ActionType.FLUID_SCOOP) {
            filter = FilterModeManager.Presets.FLUID_SCOOP(maxBlocksToMine);
        }

        FilterModeManager.FilterContext fCtxOrigin = new FilterModeManager.FilterContext(
                world, player, heldItem, origin, origin,
                originState, originState, Direction.UP, 0, 0, 0,
                strategy.getModeType(), cache, activeBlacklist, c.requireHarvestCapability);

        if (!filter.test(fCtxOrigin)) {
            stateMachine.force(EngineState.IDLE);
            return false;
        }

        Item initialItem = heldItem.getItem();

        // Collect blocks
        Direction hitFace = approximateHitFace(player);
        OrientationContext ctx = OrientationContext.of(hitFace, OrientationContext.facingFromYaw(player.getYRot()));

        MiningStrategy.MiningRequest req = new MiningStrategy.MiningRequest(
                world, player, heldItem,
                origin, originState, maxBlocksToMine, ctx, filter, cache, activeBlacklist,
                c.requireHarvestCapability, EngineState.PROCESSING, initialItem, c.allowHeldItemChange);

        List<BlockPos> found = strategy.collectBlocks(req);

        // Ensure origin block is in the list to be processed by the queue
        if (!found.contains(origin)) {
            found.add(0, origin);
        }

        if (found.isEmpty()) {
            stateMachine.force(EngineState.IDLE);
            return false;
        }

        // Create context
        ActionContext actionContext = createContext(actionType, player, world, origin, originState);

        // Start session
        session = new ActionSession();
        session.setActionType(actionType);
        session.setActionContext(actionContext);
        session.setInitialItem(initialItem);
        session.setInitialEnchantSig(com.tcveinminer.engine.skill.ToolManagerSkill.getSpecialEnchantSig(heldItem));
        session.getQueue().reset();
        session.getQueue().enqueue(found, world);
        session.initSnapshot(new HashSet<>(found));

        SessionStartEvent startEvent = new SessionStartEvent(player, world, origin, actionType, session.getTargetCount());
        if (TCVeinMinerEvents.SESSION_START.invoker().onSessionStart(startEvent) != net.minecraft.world.InteractionResult.PASS) {
            session = null;
            stateMachine.force(EngineState.IDLE);
            return false;
        }

        stateMachine.force(EngineState.PROCESSING);
        SessionStats.onVeinMineStart();
        if (player instanceof ServerPlayer spe) {
            NetworkManager.sendToPlayer(spe, new MiningStateData(1, 0, session.getTargetCount()));
        }

        return true;
    }

    // ── Template Method Implementations ───────────────────────────────────

    @Override
    protected ActionType resolveActionType(Player player, ServerLevel world,
                                            BlockPos origin, BlockState state) {
        return CapabilityRegistry.resolve(player, player.getMainHandItem(), state, InteractionHand.MAIN_HAND);
    }

    @Override
    protected FilterModeManager.BlockFilter buildFilter(ActionType type,
                                                         MiningStrategy strategy, int maxBlocks) {
        return RightClickFilterPipeline.forAction(type, maxBlocks);
    }

    @Override
    protected ActionContext createContext(ActionType type, Player player,
                                          ServerLevel world, BlockPos origin, BlockState state) {
        Item initialItem = player.getMainHandItem().getItem();
        return switch (type) {
            case FLUID_SCOOP -> new ActionContext.BucketContext(state, initialItem,
                    state.getBlock(), countBuckets(player));
            case PLANT -> new ActionContext.PlantContext(state, initialItem,
                    InteractionHand.MAIN_HAND, createDummyHitResult(origin));
            case HOE_TILL, INTERACT_BLOCK -> new ActionContext.InteractContext(state, initialItem,
                    InteractionHand.MAIN_HAND, createDummyHitResult(origin));
            default -> new ActionContext(state, initialItem);
        };
    }

    @Override
    protected MiningStrategy selectStrategy(ActionType type) {
        // Right-click uses player shape by default, unless overriden later (e.g. Bucket in onInteractTrigger)
        return (customStrategy != null) ? customStrategy : StrategyRegistry.get(playerShape);
    }

    @Override
    protected boolean validateTrigger(Player player, ServerLevel world,
                                       BlockPos origin, BlockState state, ModConfig config) {
        // Block TREE_CAP shape from vein-stripping logs
        if ("TREE_CAP".equals(playerShape) && state.is(BlockTags.LOGS)) return false;
        if (config.consumeHunger && !player.isCreative() && player.getFoodData().getFoodLevel() <= 0)
            return false;
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private boolean isSkillEnabled(ActionType type, ModConfig c) {
        return switch (type) {
            case CROP_HARVEST -> c.enableCropHarvestSkill;
            case INTERACT_BLOCK, HOE_TILL -> c.enableInteractSkill;
            case FLUID_SCOOP -> c.enableBucketSkill;
            case PLANT -> c.enableInteractSkill; // Uses interact mechanism
            default -> true;
        };
    }

    private static int countBuckets(Player player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == Items.BUCKET) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static BlockHitResult createDummyHitResult(BlockPos pos) {
        return new BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(pos),
                Direction.UP, pos, false);
    }

    private static Direction approximateHitFace(Player player) {
        float pitch = player.getXRot();
        if (pitch > 60f) return Direction.UP;
        if (pitch < -60f) return Direction.DOWN;
        return OrientationContext.facingFromYaw(player.getYRot()).getOpposite();
    }
}
