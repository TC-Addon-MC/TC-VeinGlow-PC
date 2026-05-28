package com.tcveinminer.engine.left;

import com.tcveinminer.TCVeinMinerMod;
import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.engine.AbstractActionEngine;
import com.tcveinminer.engine.action.ActionContext;
import com.tcveinminer.engine.action.ActionType;
import com.tcveinminer.engine.filter.LeftClickFilterPipeline;
import com.tcveinminer.engine.queue.BlockActionQueue;
import com.tcveinminer.engine.session.ActionSession;
import com.tcveinminer.engine.state.EngineState;
import com.tcveinminer.engine.strategy.FilterModeManager;
import com.tcveinminer.engine.strategy.MiningStrategy;
import com.tcveinminer.engine.strategy.StrategyRegistry;
import com.tcveinminer.engine.traversal.OrientationContext;
import com.tcveinminer.engine.skill.TreeCapitatorSkill;
import com.tcveinminer.network.HighlightBlockListPayload;
import com.tcveinminer.network.MiningStatePayload;
import com.tcveinminer.util.SessionStats;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;

/**
 * Left-click engine — handles BREAK and TREE_CAP actions.
 * <p>
 * NEVER touches: interact, crop, bucket, hoe, planting.
 * <p>
 * Entry point:
 * {@link #onBreakTrigger(PlayerEntity, ServerWorld, BlockPos, BlockState)}
 * from {@code PlayerBlockBreakEvents.AFTER}.
 */
public final class LeftClickEngine extends AbstractActionEngine {

    @Override
    protected String getSourceId() {
        return "LEFT";
    }

    // ── Entry Point ───────────────────────────────────────────────────────

    /**
     * Called when a player breaks a block (from PlayerBlockBreakEvents.AFTER).
     */
    public void onBreakTrigger(PlayerEntity player, ServerWorld world,
            BlockPos origin, BlockState originState) {
        // Re-entry guard: if we are already processing, the AFTER event
        // from our own tryBreakBlock() must be ignored
        if (session != null && session.isProcessing())
            return;

        // If already in PROCESSING, track block in snapshot
        if (stateMachine.is(EngineState.PROCESSING)) {
            if (session != null && session.getLockedSnapshot().contains(origin)) {
                session.removeFromSnapshot(origin);
                session.updateRenderSnapshot();
                if (player instanceof ServerPlayerEntity spe) {
                    ServerPlayNetworking.send(spe, new HighlightBlockListPayload(
                            new ArrayList<>(session.getRenderSnapshot()), playerShape, getSourceId()));
                }
                if (session.getQueue().isEmpty() && session.getLockedSnapshot().isEmpty()) {
                    finalizeMining(player, world);
                }
            }
            return;
        }

        if (!stateMachine.is(EngineState.IDLE) && !stateMachine.is(EngineState.PREVIEW))
            return;

        ModConfig c = ConfigManager.get();
        if (!c.enabled)
            return;
        if (!TCVeinMinerMod.playersHoldingV.contains(player.getUuid()))
            return;

        if (c.requireSneak && !player.isSneaking())
            return;
        if (!checkCooldown(player.getUuid(), world.getTime(), c))
            return;

        if (!validateTrigger(player, world, origin, originState, c))
            return;

        Set<String> activeBlacklist = mergedBlacklist;
        if (activeBlacklist.contains(blockId(originState)))
            return;

        if (stateMachine.is(EngineState.PREVIEW)) {
            // Keep going, transition from PREVIEW → PROCESSING
        } else if (!stateMachine.is(EngineState.IDLE)) {
            return;
        }

        // Resolve action
        ActionType actionType = resolveActionType(player, world, origin, originState);
        MiningStrategy strategy = selectStrategy(actionType);

        int maxBlocksToMine = this.playerMaxBlocks - 1;

        // Build filter
        FilterModeManager.FilterCache cache = new FilterModeManager.FilterCache();
        FilterModeManager.BlockFilter filter = buildFilter(actionType, strategy, maxBlocksToMine);

        FilterModeManager.FilterContext fCtxOrigin = new FilterModeManager.FilterContext(
                world, player, player.getMainHandStack(), origin, origin,
                originState, originState, Direction.UP, 0, 0, 0,
                strategy.getModeType(), cache, activeBlacklist, c.requireHarvestCapability);

        if (!filter.test(fCtxOrigin)) {
            stateMachine.force(EngineState.IDLE);
            return;
        }

        Item initialItem = player.getMainHandStack().getItem();

        Direction hitFace = approximateHitFace(player);
        OrientationContext ctx = OrientationContext.of(hitFace, OrientationContext.facingFromYaw(player.getYaw()));

        MiningStrategy.MiningRequest req = new MiningStrategy.MiningRequest(
                world, player, player.getMainHandStack(),
                origin, originState, maxBlocksToMine, ctx, filter, cache, activeBlacklist,
                c.requireHarvestCapability, EngineState.PROCESSING, initialItem, c.allowHeldItemChange);

        List<BlockPos> found = strategy.collectBlocks(req);
        if (found.isEmpty()) {
            stateMachine.force(EngineState.IDLE);
            return;
        }

        // Create action context
        ActionContext actionContext = createContext(actionType, player, world, origin, originState);

        // Detect sapling for TreeCap
        if (actionType == ActionType.TREE_CAP && actionContext instanceof ActionContext.TreeCapContext tc) {
            Map<Block, Integer> leafCounts = new HashMap<>();
            for (BlockPos pos : found) {
                BlockState state = world.getBlockState(pos);
                if (state.isIn(BlockTags.LEAVES)) {
                    leafCounts.merge(state.getBlock(), 1, Integer::sum);
                }
            }
            if (!leafCounts.isEmpty()) {
                Block mostCommonLeaf = Collections.max(leafCounts.entrySet(), Map.Entry.comparingByValue()).getKey();
                tc.setTreeSaplingType(TreeCapitatorSkill.getSaplingForBlock(mostCommonLeaf));
            }
        }

        // Start session
        session = new ActionSession();
        session.setActionType(actionType);
        session.setActionContext(actionContext);
        session.setInitialItem(initialItem);
        session.setInitialEnchantSig(com.tcveinminer.engine.skill.ToolManagerSkill.getSpecialEnchantSig(player.getMainHandStack()));
        session.getQueue().reset();
        session.getQueue().enqueue(found, world);
        session.initSnapshot(new HashSet<>(found));

        stateMachine.force(EngineState.PROCESSING);
        SessionStats.onVeinMineStart();
        if (player instanceof ServerPlayerEntity spe) {
            ServerPlayNetworking.send(spe, new MiningStatePayload(1, 0, session.getTargetCount()));
        }
    }

    // ── Template Method Implementations ───────────────────────────────────

    @Override
    protected ActionType resolveActionType(PlayerEntity player, ServerWorld world,
            BlockPos origin, BlockState state) {
        if ("TREE_CAP".equals(playerShape) && state.isIn(BlockTags.LOGS)) {
            return ActionType.TREE_CAP;
        }
        return ActionType.BREAK;
    }

    @Override
    protected FilterModeManager.BlockFilter buildFilter(ActionType type,
            MiningStrategy strategy, int maxBlocks) {
        return LeftClickFilterPipeline.forAction(type, strategy.getModeType(), maxBlocks);
    }

    @Override
    protected ActionContext createContext(ActionType type, PlayerEntity player,
            ServerWorld world, BlockPos origin, BlockState state) {
        Item initialItem = player.getMainHandStack().getItem();
        if (type == ActionType.TREE_CAP) {
            return new ActionContext.TreeCapContext(state, initialItem, origin, null);
        }
        return new ActionContext(state, initialItem);
    }

    @Override
    protected MiningStrategy selectStrategy(ActionType type) {
        return (customStrategy != null) ? customStrategy : StrategyRegistry.get(playerShape);
    }

    @Override
    protected boolean validateTrigger(PlayerEntity player, ServerWorld world,
            BlockPos origin, BlockState state, ModConfig config) {
        // TREE_CAP shape + non-log = reject
        if ("TREE_CAP".equals(playerShape) && !state.isIn(BlockTags.LOGS))
            return false;
        if (!config.enableBreakSkill)
            return false;
        if (config.consumeHunger && !player.isCreative() && player.getHungerManager().getFoodLevel() <= 0)
            return false;
        return true;
    }

    @Override
    protected void onSessionFinalized(PlayerEntity player, ServerWorld world) {
        // Auto-replant for TreeCap
        if (session != null && session.getActionContext() instanceof ActionContext.TreeCapContext tc) {
            if (ConfigManager.get().enableTreeCapitatorSkill) {
                TreeCapitatorSkill.autoReplant((ServerWorld) player.getWorld(), player,
                        tc.getTreeOriginPos(), tc.getTreeSaplingType());
            }
        }
    }

    @Override
    protected boolean isBlockStillValid(BlockState currentState, BlockActionQueue.ActionEntry entry,
            ActionSession session) {
        if (session.getActionContext() == null)
            return false;
        BlockState original = session.getActionContext().getOriginalState();
        if (original == null)
            return false;

        // TreeCap: allow any log or leaf type
        if (session.getActionType() == ActionType.TREE_CAP) {
            return currentState.isIn(BlockTags.LOGS) || currentState.isIn(BlockTags.LEAVES);
        }

        return currentState.getBlock() == original.getBlock();
    }

    // ── Utility ───────────────────────────────────────────────────────────

    private static Direction approximateHitFace(PlayerEntity player) {
        float pitch = player.getPitch();
        if (pitch > 60f)
            return Direction.UP;
        if (pitch < -60f)
            return Direction.DOWN;
        return OrientationContext.facingFromYaw(player.getYaw()).getOpposite();
    }
}
