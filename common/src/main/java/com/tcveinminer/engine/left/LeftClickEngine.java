package com.tcveinminer.engine.left;

import com.tcveinminer.api.TCVeinMinerEvents;
import com.tcveinminer.api.event.SessionStartEvent;
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
import com.tcveinminer.network.NetworkManager;
import com.tcveinminer.network.payload.HighlightBlockListData;
import com.tcveinminer.network.payload.MiningStateData;
import com.tcveinminer.util.SessionStats;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.*;

/**
 * Left-click engine — handles BREAK and TREE_CAP actions.
 * <p>
 * NEVER touches: interact, crop, bucket, hoe, planting.
 * <p>
 * Entry point:
 * {@link #onBreakTrigger(Player, ServerLevel, BlockPos, BlockState)}
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
    public void onBreakTrigger(Player player, ServerLevel world,
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
                if (player instanceof ServerPlayer spe) {
                    NetworkManager.sendToPlayer(spe, new HighlightBlockListData(
                            session.getRenderSnapshot().stream().map(BlockPos::asLong).toList(), playerShape, getSourceId()));
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
        if (!com.tcveinminer.engine.state.PlayerStateRegistry.isHoldingKey(player.getUUID()))
            return;

        if (c.requireSneak && !player.isShiftKeyDown())
            return;
        if (!checkCooldown(player.getUUID(), world.getGameTime(), c))
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
                world, player, player.getMainHandItem(), origin, origin,
                originState, originState, Direction.UP, 0, 0, 0,
                strategy.getModeType(), cache, activeBlacklist, c.requireHarvestCapability);

        if (!filter.test(fCtxOrigin)) {
            stateMachine.force(EngineState.IDLE);
            return;
        }

        Item initialItem = player.getMainHandItem().getItem();

        Direction hitFace = approximateHitFace(player);
        OrientationContext ctx = OrientationContext.of(hitFace, OrientationContext.facingFromYaw(player.getYRot()));

        MiningStrategy.MiningRequest req = new MiningStrategy.MiningRequest(
                world, player, player.getMainHandItem(),
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
                if (state.is(BlockTags.LEAVES)) {
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
        session.setInitialEnchantSig(com.tcveinminer.engine.skill.ToolManagerSkill.getSpecialEnchantSig(player.getMainHandItem()));
        session.getQueue().reset();
        session.getQueue().enqueue(found, world);
        session.initSnapshot(new HashSet<>(found));

        SessionStartEvent startEvent = new SessionStartEvent(player, world, origin, actionType, session.getTargetCount());
        if (TCVeinMinerEvents.SESSION_START.invoker().onSessionStart(startEvent) != net.minecraft.world.InteractionResult.PASS) {
            session = null;
            stateMachine.force(EngineState.IDLE);
            return;
        }

        stateMachine.force(EngineState.PROCESSING);
        SessionStats.onVeinMineStart();
        if (player instanceof ServerPlayer spe) {
            NetworkManager.sendToPlayer(spe, new MiningStateData(1, 0, session.getTargetCount()));
        }
    }

    // ── Template Method Implementations ───────────────────────────────────

    @Override
    protected ActionType resolveActionType(Player player, ServerLevel world,
            BlockPos origin, BlockState state) {
        if ("TREE_CAP".equals(playerShape) && state.is(BlockTags.LOGS)) {
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
    protected ActionContext createContext(ActionType type, Player player,
            ServerLevel world, BlockPos origin, BlockState state) {
        Item initialItem = player.getMainHandItem().getItem();
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
    protected boolean validateTrigger(Player player, ServerLevel world,
            BlockPos origin, BlockState state, ModConfig config) {
        // TREE_CAP shape + non-log = reject
        if ("TREE_CAP".equals(playerShape) && !state.is(BlockTags.LOGS))
            return false;
        if (!config.enableBreakSkill)
            return false;
        if (config.consumeHunger && !player.isCreative() && player.getFoodData().getFoodLevel() <= 0)
            return false;
        return true;
    }

    @Override
    protected void onSessionFinalized(Player player, ServerLevel world) {
        // Auto-replant for TreeCap
        if (session != null && session.getActionContext() instanceof ActionContext.TreeCapContext tc) {
            if (ConfigManager.get().enableTreeCapitatorSkill) {
                TreeCapitatorSkill.autoReplant((ServerLevel) player.level(), player,
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
            return currentState.is(BlockTags.LOGS) || currentState.is(BlockTags.LEAVES);
        }

        return currentState.getBlock() == original.getBlock();
    }

    // ── Utility ───────────────────────────────────────────────────────────

    private static Direction approximateHitFace(Player player) {
        float pitch = player.getXRot();
        if (pitch > 60f)
            return Direction.UP;
        if (pitch < -60f)
            return Direction.DOWN;
        return OrientationContext.facingFromYaw(player.getYRot()).getOpposite();
    }
}
