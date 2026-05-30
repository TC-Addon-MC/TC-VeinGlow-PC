package com.tcveinminer.engine;

import com.tcveinminer.api.TCVeinMinerEvents;
import com.tcveinminer.api.event.BlockBreakEvent;
import com.tcveinminer.api.event.SessionEndEvent;
import com.tcveinminer.TCVeinMinerMod;
import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.engine.action.ActionContext;
import com.tcveinminer.engine.action.ActionExecutorRegistry;
import com.tcveinminer.engine.action.ActionType;
import com.tcveinminer.engine.action.BlockAction;
import com.tcveinminer.engine.queue.BlockActionQueue;
import com.tcveinminer.engine.session.ActionSession;
import com.tcveinminer.engine.state.EngineState;
import com.tcveinminer.engine.state.EngineStateMachine;
import com.tcveinminer.engine.strategy.CustomEquationStrategy;
import com.tcveinminer.engine.strategy.FilterModeManager;
import com.tcveinminer.engine.strategy.MiningStrategy;
import com.tcveinminer.engine.strategy.StrategyRegistry;
import com.tcveinminer.logic.HudNotifier;
import com.tcveinminer.network.NetworkManager;
import com.tcveinminer.network.payload.HighlightDeltaData;
import com.tcveinminer.network.payload.MiningStateData;
import com.tcveinminer.util.ExpressionEvaluator;
import com.tcveinminer.util.SessionStats;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base class for action engines (left-click and right-click).
 * <p>
 * Contains ALL shared lifecycle logic:
 * <ul>
 * <li>Queue ticking ({@link #onServerTick})</li>
 * <li>Snapshot sync</li>
 * <li>Render/highlight updates</li>
 * <li>Session lifecycle (stop, finalize, cancel)</li>
 * <li>Cooldown checking</li>
 * <li>Packet sending (MiningStatePayload, HighlightBlockListPayload)</li>
 * <li>HUD notification</li>
 * <li>Config management</li>
 * </ul>
 * <p>
 * Subclasses (LeftClickEngine, RightClickEngine) only override:
 * <ul>
 * <li>Action resolution</li>
 * <li>Filter pipeline composition</li>
 * <li>Strategy selection</li>
 * <li>ActionContext creation</li>
 * <li>Trigger validation</li>
 * <li>Post-finalization hooks</li>
 * </ul>
 */
public abstract class AbstractActionEngine {

    protected final EngineStateMachine stateMachine = new EngineStateMachine();
    protected ActionSession session;

    // ── Shared config ─────────────────────────────────────────────────────
    protected String playerShape = "FACE";
    protected int playerMaxBlocks = 64;
    protected MiningStrategy customStrategy = null;
    protected Set<String> playerBlacklist = new HashSet<>();
    protected Set<String> mergedBlacklist = Collections.emptySet();

    // ── Cooldown (shared static map, keyed by UUID + source) ──────────────
    private static final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    // ══════════════════════════════════════════════════════════════════════
    // TEMPLATE METHODS — subclasses override these
    // ══════════════════════════════════════════════════════════════════════

    /** Source identifier for highlight packets ("LEFT" or "RIGHT") */
    protected abstract String getSourceId();

    /** Resolve which ActionType to use for the given context */
    protected abstract ActionType resolveActionType(PlayerEntity player, ServerWorld world,
            BlockPos origin, BlockState state);

    /** Build the filter pipeline for the resolved action */
    protected abstract FilterModeManager.BlockFilter buildFilter(ActionType type,
            MiningStrategy strategy, int maxBlocks);

    /** Create the appropriate ActionContext for the resolved action */
    protected abstract ActionContext createContext(ActionType type, PlayerEntity player,
            ServerWorld world, BlockPos origin, BlockState state);

    /** Select mining strategy for block collection */
    protected abstract MiningStrategy selectStrategy(ActionType type);

    /** Engine-specific validation before starting a session */
    protected abstract boolean validateTrigger(PlayerEntity player, ServerWorld world,
            BlockPos origin, BlockState state, ModConfig config);

    /**
     * Called after session completes successfully (e.g., tree replant). Override
     * for hooks.
     */
    protected void onSessionFinalized(PlayerEntity player, ServerWorld world) {
    }

    /**
     * Check if a block is still valid for processing during tick.
     * Default: same block type as expected. Override for action-specific logic.
     */
    protected boolean isBlockStillValid(BlockState currentState, BlockActionQueue.ActionEntry entry,
            ActionSession session) {
        if (session.getActionContext() == null)
            return false;
        BlockState original = session.getActionContext().getOriginalState();
        return original != null && currentState.getBlock() == original.getBlock();
    }

    // ══════════════════════════════════════════════════════════════════════
    // SHARED IMPLEMENTATION — never duplicated in subclasses
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Main tick processor — drain queue, execute actions, update snapshot.
     * Called once per server tick per engine.
     */
    public void onServerTick(PlayerEntity player, ServerWorld world) {
        // Cleanup terminal states
        if (stateMachine.is(EngineState.FINISHED) || stateMachine.is(EngineState.CANCELLED)) {
            stateMachine.force(EngineState.IDLE);
            return;
        }

        if (!stateMachine.is(EngineState.PROCESSING))
            return;
        if (session == null)
            return;

        // Item lock constraint
        ModConfig c = ConfigManager.get();
        // Nếu enableToolSwapSkill bật, bỏ qua khi tay trống (tool vừa vỡ)
        // để handleToolState xử lý việc tìm & đổi tool thay thế.
        // Nếu không bỏ qua, stop() sẽ được gọi trước khi handleToolState kịp chạy.
        if (!c.allowHeldItemChange && session.getInitialItem() != null
                && player.getMainHandStack().getItem() != session.getInitialItem()) {
            boolean toolJustBroke = player.getMainHandStack().isEmpty();
            boolean swapWillHandle = c.enableToolSwapSkill && c.requireHarvestCapability;
            if (!(toolJustBroke && swapWillHandle)) {
                stop(player);
                return;
            }
        }

        // Key hold check
        if (!TCVeinMinerMod.playersHoldingV.contains(player.getUuid())) {
            stop(player);
            return;
        }

        // Hunger constraint
        if (c.consumeHunger && !player.isCreative() && player.getHungerManager().getFoodLevel() <= 0) {
            stop(player);
            return;
        }

        // Queue exhausted
        if (session.getQueue().isEmpty()) {
            if (session.getLockedSnapshot().isEmpty()) {
                finalizeMining(player, world);
            }
            return;
        }

        // Adaptive Mining Speed Logic (Square Root Curve)
        int totalBlocks = session.getTargetCount();
        int blocksPerTick;

        if (c.miningSpeed <= 0) {
            // Instant mining (0s)
            blocksPerTick = totalBlocks;
        } else {
            int speed = Math.min(10, c.miningSpeed);
            // Speed=1 -> factor=0.75 (fastest). Speed=10 -> factor=7.5 (slowest).
            double factor = speed * 0.75;
            double targetTicks = Math.sqrt(totalBlocks) * factor;
            targetTicks = Math.max(1.0, targetTicks);

            int dynamicBlocksPerTick = (int) Math.ceil(totalBlocks / targetTicks);

            // Safety bounds
            int minBlocksPerTick = c.tickSliceSize > 0 ? c.tickSliceSize : 1;
            blocksPerTick = Math.max(minBlocksPerTick, Math.min(100, dynamicBlocksPerTick));
        }

        List<BlockActionQueue.ActionEntry> batch = session.getQueue().drainForTick(world, blocksPerTick);

        List<BlockPos> removed = new ArrayList<>();

        // Set re-entry guard BEFORE executing actions
        session.beginProcessing();
        if (session.getActionType().isRightClick()) {
            com.tcveinminer.engine.right.RightClickEngine.setProcessingInternal(true);
        }

        if (player instanceof ServerPlayerEntity spe) {
            NetworkManager.sendToPlayer(spe,
                    new MiningStateData(1, session.getProcessedCount(), session.getTargetCount()));
        }

        try {
            BlockAction executor = ActionExecutorRegistry.get(session.getActionType());
            if (executor == null) {
                stop(player);
                return;
            }

            for (BlockActionQueue.ActionEntry e : batch) {
                if (!(player instanceof ServerPlayerEntity spe))
                    break;

                BlockState currentState = world.getBlockState(e.pos());

                // Block identity validation
                if (!isBlockStillValid(currentState, e, session)) {
                    session.removeFromSnapshot(e.pos());
                    removed.add(e.pos());
                    continue;
                }

                BlockBreakEvent preEvent = new BlockBreakEvent(player, world, e.pos(), currentState,
                        session.getActionType());
                if (TCVeinMinerEvents.BLOCK_BREAK_PRE.invoker()
                        .onBlockBreakPre(preEvent) != net.minecraft.util.ActionResult.PASS) {
                    session.removeFromSnapshot(e.pos());
                    removed.add(e.pos());
                    continue;
                }

                boolean success = executor.execute(spe, world, e.pos(), session.getActionContext());

                session.removeFromSnapshot(e.pos());
                removed.add(e.pos());

                if (!success)
                    continue;

                TCVeinMinerEvents.BLOCK_BREAK_POST.invoker().onBlockBreakPost(preEvent);

                String blockId = Registries.BLOCK.getId(currentState.getBlock()).toString();
                SessionStats.onBlockBroken(blockId);
                session.incrementProcessed();

                // Tool Management Skill
                if (c.requireHarvestCapability && session.getInitialItem() != Items.AIR) {
                    com.tcveinminer.engine.skill.ToolManagerSkill.ToolAction toolAction = com.tcveinminer.engine.skill.ToolManagerSkill
                            .handleToolState(spe, c, session.getInitialItem(), session);
                    if (toolAction == com.tcveinminer.engine.skill.ToolManagerSkill.ToolAction.STOP) {
                        stop(player);
                        return;
                    }
                }

                if (c.consumeDurability) {
                    SessionStats.onDurabilityUsed(1);
                }

                if (c.consumeHunger && c.blocksPerHunger > 0) {
                    player.addExhaustion(4.0F / c.blocksPerHunger);
                }
            }
        } finally {
            session.endProcessing();
            if (session.getActionType().isRightClick()) {
                com.tcveinminer.engine.right.RightClickEngine.setProcessingInternal(false);
            }
        }

        if (!removed.isEmpty()) {
            session.updateRenderSnapshot();
            sendHighlightUpdate(player, removed);
        }

        if (session.getQueue().isEmpty() && session.getLockedSnapshot().isEmpty()) {
            finalizeMining(player, world);
        }
    }

    /**
     * Stop/cancel the current session.
     */
    protected void stop(PlayerEntity player) {
        List<BlockPos> removed = session != null ? new ArrayList<>(session.getRenderSnapshot())
                : Collections.emptyList();

        if (session != null) {
            TCVeinMinerEvents.SESSION_END.invoker().onSessionEnd(new SessionEndEvent(
                    player, player.getWorld(), session.getActionType(),
                    session.getProcessedCount(), session.getTargetCount(), true));
            session.clear();
        }
        stateMachine.force(EngineState.CANCELLED);
        if (player instanceof ServerPlayerEntity spe) {
            NetworkManager.sendToPlayer(spe, new MiningStateData(3,
                    session != null ? session.getProcessedCount() : 0,
                    session != null ? session.getTargetCount() : 0));
            NetworkManager.sendToPlayer(spe, new HighlightDeltaData(
                    Collections.emptyList(), removed.stream().map(BlockPos::asLong).toList(), "FACE", getSourceId()));
        }
    }

    /**
     * Finalize a completed session — HUD, stats, cleanup, hooks.
     */
    protected void finalizeMining(PlayerEntity player, ServerWorld world) {
        int processed = session != null ? session.getProcessedCount() : 0;
        int target = session != null ? session.getTargetCount() : 0;

        HudNotifier.lastMined = processed;
        HudNotifier.lastMax = target;
        HudNotifier.notifyAt = System.currentTimeMillis() + 2500;

        stateMachine.force(EngineState.FINISHED);

        if (player instanceof ServerPlayerEntity spe) {
            NetworkManager.sendToPlayer(spe, new MiningStateData(2, processed, target));
            List<BlockPos> removed = session != null ? new ArrayList<>(session.getRenderSnapshot())
                    : Collections.emptyList();
            NetworkManager.sendToPlayer(spe, new HighlightDeltaData(
                    Collections.emptyList(), removed.stream().map(BlockPos::asLong).toList(), "FACE", getSourceId()));
        }

        // Hook for subclasses (e.g., tree replant)
        onSessionFinalized(player, world);

        if (session != null) {
            TCVeinMinerEvents.SESSION_END.invoker().onSessionEnd(new SessionEndEvent(
                    player, world, session.getActionType(), processed, target, false));
            session.clear();
        }
    }

    /**
     * Send highlight update packet to the client.
     */
    protected void sendHighlightUpdate(PlayerEntity player, List<BlockPos> removedBlocks) {
        if (session != null && player instanceof ServerPlayerEntity spe) {
            NetworkManager.sendToPlayer(spe, new HighlightDeltaData(
                    Collections.emptyList(), removedBlocks.stream().map(BlockPos::asLong).toList(), playerShape,
                    getSourceId()));
        }
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * @return true if this engine has an active processing session
     */
    public boolean isProcessing() {
        return session != null && session.isProcessing();
    }

    /**
     * @return true if this engine is in PROCESSING state (session active, may or
     *         may not be in tick)
     */
    public boolean isActive() {
        return stateMachine.is(EngineState.PROCESSING);
    }

    /**
     * @return current engine state
     */
    public EngineState getState() {
        return stateMachine.get();
    }

    /**
     * @return the current session's render snapshot for preview
     */
    public Set<BlockPos> getRenderSnapshot() {
        return session != null ? session.getRenderSnapshot() : Collections.emptySet();
    }

    /** Force the engine into a specific state (e.g., IDLE, PREVIEW). */
    public void forceState(EngineState state) {
        stateMachine.force(state);
    }

    /** @return processed block count for the current session, or 0 if none */
    public int getSessionProcessedCount() {
        return session != null ? session.getProcessedCount() : 0;
    }

    /** @return target block count for the current session, or 0 if none */
    public int getSessionTargetCount() {
        return session != null ? session.getTargetCount() : 0;
    }

    /** @return action type for the current session, or null if none */
    public ActionType getSessionActionType() {
        return session != null ? session.getActionType() : null;
    }

    // ── Config ────────────────────────────────────────────────────────────

    /**
     * Update player config (shape, max blocks, equation, blacklist).
     */
    public void updateConfig(String shapeId, int maxBlocks, String equation, List<String> blacklist) {
        this.playerMaxBlocks = maxBlocks;
        this.playerBlacklist = FilterModeManager.normalizeBlacklist(
                blacklist == null ? Collections.emptySet() : new HashSet<>(blacklist));

        ModConfig c = ConfigManager.get();
        this.mergedBlacklist = mergeBlacklists(
                c.blacklistedBlocks != null ? new HashSet<>(c.blacklistedBlocks) : Collections.emptySet(),
                this.playerBlacklist);
        if (shapeId != null && shapeId.startsWith("custom:") && equation != null && !equation.isBlank()) {
            ExpressionEvaluator evaluator = new ExpressionEvaluator(equation);
            this.customStrategy = evaluator.isValid() ? new CustomEquationStrategy(shapeId, evaluator) : null;
            this.playerShape = (this.customStrategy != null) ? shapeId : "FACE";
        } else {
            this.customStrategy = null;
            this.playerShape = shapeId;
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────

    /**
     * Check and update cooldown for a player+source combination.
     */
    protected boolean checkCooldown(UUID uuid, long tick, ModConfig c) {
        if (c.cooldownTicks <= 0)
            return true;
        String key = uuid.toString() + "_" + getSourceId();
        long avail = cooldowns.getOrDefault(key, 0L);
        if (tick < avail)
            return false;
        cooldowns.put(key, tick + c.cooldownTicks);
        return true;
    }

    /**
     * Clear cooldown for a player (called on disconnect).
     */
    public static void clearCooldown(UUID uuid) {
        String prefix = uuid.toString() + "_";
        cooldowns.keySet().removeIf(k -> k.startsWith(prefix));
    }

    protected static String blockId(BlockState state) {
        return Registries.BLOCK.getId(state.getBlock()).toString();
    }

    protected static Set<String> mergeBlacklists(Set<String> configBlacklist, Set<String> playerBlacklist) {
        Set<String> result = new HashSet<>(FilterModeManager.normalizeBlacklist(configBlacklist));
        result.addAll(playerBlacklist);
        return result;
    }
}