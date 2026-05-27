package com.tcveinminer.engine;

import com.tcveinminer.TCVeinMinerMod;
import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.engine.queue.MiningQueue;
import com.tcveinminer.engine.queue.MiningQueue.Entry;
import com.tcveinminer.engine.state.MiningStateMachine;
import com.tcveinminer.engine.state.MiningStateMachine.State;
import com.tcveinminer.engine.strategy.FilterModeManager;
import com.tcveinminer.engine.strategy.CustomEquationStrategy;
import com.tcveinminer.engine.strategy.MiningStrategy;
import com.tcveinminer.engine.strategy.StrategyRegistry;
import com.tcveinminer.engine.traversal.OrientationContext;
import com.tcveinminer.logic.HudNotifier;
import com.tcveinminer.util.ExpressionEvaluator;
import com.tcveinminer.util.SessionStats;
import com.tcveinminer.network.MiningStatePayload;
import com.tcveinminer.network.ActivationConfirmPayload;
import com.tcveinminer.network.FilterResultPayload;
import com.tcveinminer.network.LookedAtBlockPayload;
import com.tcveinminer.network.HighlightBlockListPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MiningEngine: per-player orchestrator.
 *
 * Fixed issues:
 * [CRITICAL] Re-entry guard: isMining flag prevents AFTER break event from
 * re-triggering onBreakTrigger while engine is executing.
 * [CRITICAL] Enchantment/XP/exhaustion: uses
 * ServerPlayerEntity.interactionManager
 * .tryBreakBlock() which runs full vanilla break pipeline.
 * [CRITICAL] Tool check every tick: toolOk() called in onServerTick, not just
 * trigger.
 * [CRITICAL] Tool break: full vanilla pipeline handles it correctly.
 * [CRITICAL] Cooldowns/playersHoldingV never cleaned → moved to disconnect
 * handler
 * in TCVeinMinerMod.
 * [SECURITY] maxBlocks clamped server-side in TCVeinMinerMod packet handler.
 * [FEATURE] Đã tích hợp FilterModeManager và MiningRequest vào quy trình BFS.
 */
public final class MiningEngine {

    private static final Map<UUID, MiningEngine> ENGINES = new ConcurrentHashMap<>();

    public static MiningEngine forPlayer(UUID uuid) {
        return ENGINES.computeIfAbsent(uuid, id -> new MiningEngine());
    }

    public static void removePlayer(UUID uuid) {
        MiningEngine engine = ENGINES.remove(uuid);
        if (engine != null) {
            // Clear cooldown too
            cooldowns.remove(uuid);
        }
    }

    // ── Instance state ──────────────────────────────────────────────────────

    private final MiningStateMachine stateMachine = new MiningStateMachine();
    private final MiningQueue queue = new MiningQueue();
    private Set<BlockPos> lockedSnapshot = new HashSet<>();
    private volatile Set<BlockPos> renderSnapshot = Collections.emptySet();

    /**
     * RE-ENTRY GUARD: set to true while engine is breaking blocks.
     * onBreakTrigger checks this flag and returns immediately if true,
     * preventing the AFTER break event from recursing back into the engine.
     */
    private boolean isMining = false;

    private int brokenCount = 0;
    private int targetCount = 0;
    private String playerShape = "FACE";
    private int playerMaxBlocks = 64;
    private MiningStrategy customStrategy = null;
    private Set<String> playerBlacklist = new HashSet<>();
    private BlockState originalState = null;
    private Item initialItem = null;

    private MiningEngine() {
    }

    public void updatePlayerConfig(String shapeId, int maxBlocks) {
        updatePlayerConfig(shapeId, maxBlocks, "", Collections.emptyList());
    }

    public void updatePlayerConfig(String shapeId, int maxBlocks, String equation, List<String> blacklist) {
        this.playerMaxBlocks = maxBlocks;
        this.playerBlacklist = FilterModeManager.normalizeBlacklist(
                blacklist == null ? Collections.emptySet() : new HashSet<>(blacklist));
        if (shapeId != null && shapeId.startsWith("custom:") && equation != null && !equation.isBlank()) {
            this.customStrategy = buildCustomStrategy(shapeId, equation);
            this.playerShape = (this.customStrategy != null) ? shapeId : "FACE";
        } else {
            this.customStrategy = null;
            this.playerShape = shapeId;
        }
    }

    private static MiningStrategy buildCustomStrategy(String id, String equation) {
        ExpressionEvaluator evaluator = new ExpressionEvaluator(equation);
        if (!evaluator.isValid())
            return null;
        return new CustomEquationStrategy(id, evaluator);
    }

    // ── Public API ───────────────────────────────────────────────────────────

    public void handleActivationRequest(ServerPlayerEntity spe, boolean active, BlockPos targetPos) {
        if (!active || targetPos == null) {
            if (stateMachine.is(State.PREVIEW)) {
                stateMachine.force(State.IDLE);
                renderSnapshot = Collections.emptySet();
                lockedSnapshot.clear();
            }
            ServerPlayNetworking.send(spe, new ActivationConfirmPayload(false));
            ServerPlayNetworking.send(spe, new FilterResultPayload(false));
            ServerPlayNetworking.send(spe, new HighlightBlockListPayload(Collections.emptyList(), "FACE"));
            return;
        }

        // Ignore dynamic updates if already locked/finished
        if (stateMachine.is(State.LOCKED_MINING) || stateMachine.is(State.FINISHED) || stateMachine.is(State.CANCELLED)) {
            return;
        }

        ModConfig c = ConfigManager.get();
        if (!c.enabled) return;

        stateMachine.force(State.PREVIEW);

        ServerWorld world = spe.getServerWorld();
        if (!world.isChunkLoaded(targetPos.getX() >> 4, targetPos.getZ() >> 4)) return;

        BlockState targetState = world.getBlockState(targetPos);
        if (targetState.isAir()) {
            ServerPlayNetworking.send(spe, new FilterResultPayload(false));
            return;
        }

        Set<String> activeBlacklist = mergeBlacklists(c.blacklistedBlocks, playerBlacklist);
        boolean isBlacklisted = activeBlacklist.contains(blockId(targetState));

        if (isBlacklisted) {
            ServerPlayNetworking.send(spe, new ActivationConfirmPayload(false));
            ServerPlayNetworking.send(spe, new LookedAtBlockPayload(targetPos));
            ServerPlayNetworking.send(spe, new FilterResultPayload(false));
            return;
        }

        Direction hitFace = approximateHitFace(spe);
        OrientationContext ctx = OrientationContext.of(
                hitFace,
                OrientationContext.facingFromYaw(spe.getYaw())
        );

        MiningStrategy strategy = (customStrategy != null)
                ? customStrategy
                : StrategyRegistry.get(this.playerShape);
        int maxBlocksToMine = this.playerMaxBlocks - 1;

        FilterModeManager.FilterCache cache = new FilterModeManager.FilterCache();
        FilterModeManager.BlockFilter filter = FilterModeManager.resolveFilter(strategy.getModeType(), maxBlocksToMine);

        FilterModeManager.FilterContext fCtxTarget = new FilterModeManager.FilterContext(
                world, spe, spe.getMainHandStack(), targetPos, targetPos,
                targetState, targetState, Direction.UP, 0, 0, 0, strategy.getModeType(), cache,
                activeBlacklist,
                c.requireCorrectTool
        );

        if (!filter.test(fCtxTarget)) {
            ServerPlayNetworking.send(spe, new ActivationConfirmPayload(false));
            ServerPlayNetworking.send(spe, new LookedAtBlockPayload(targetPos));
            ServerPlayNetworking.send(spe, new FilterResultPayload(false));
            return;
        }

        ServerPlayNetworking.send(spe, new ActivationConfirmPayload(true));
        ServerPlayNetworking.send(spe, new LookedAtBlockPayload(targetPos));
        ServerPlayNetworking.send(spe, new FilterResultPayload(true));

        MiningStrategy.MiningRequest req = new MiningStrategy.MiningRequest(
                world, spe, spe.getMainHandStack(),
                targetPos, targetState, maxBlocksToMine, ctx, filter, cache, activeBlacklist,
                c.requireCorrectTool, stateMachine.get(), spe.getMainHandStack().getItem(), c.allowHeldItemChange
        );

        List<BlockPos> found = strategy.collectBlocks(req);
        if (!found.contains(targetPos)) {
            found.add(targetPos);
        }

        ServerPlayNetworking.send(spe, new HighlightBlockListPayload(found, this.playerShape));
    }

    public void onBreakTrigger(PlayerEntity player, ServerWorld world,
            BlockPos origin, BlockState originState) {
        // [CRITICAL] Re-entry guard: if we are already breaking blocks, the AFTER event
        // fired by our own world.interactionManager.tryBreakBlock() must be ignored.
        if (isMining)
            return;

        if (stateMachine.is(State.LOCKED_MINING)) {
            if (lockedSnapshot.contains(origin)) {
                lockedSnapshot.remove(origin);
                renderSnapshot = new HashSet<>(lockedSnapshot);
                if (player instanceof ServerPlayerEntity spe) {
                    ServerPlayNetworking.send(spe, new HighlightBlockListPayload(new ArrayList<>(renderSnapshot), this.playerShape));
                }
                if (queue.isEmpty() && lockedSnapshot.isEmpty()) {
                    finalizeMining(player);
                }
            }
            return;
        }

        if (!stateMachine.is(State.IDLE) && !stateMachine.is(State.PREVIEW))
            return;

        ModConfig c = ConfigManager.get();
        if (!c.enabled)
            return;
        if (!TCVeinMinerMod.playersHoldingV.contains(player.getUuid()))
            return;
        Set<String> activeBlacklist = mergeBlacklists(c.blacklistedBlocks, playerBlacklist);
        if (activeBlacklist.contains(blockId(originState)))
            return;
        if (c.requireSneak && !player.isSneaking())
            return;
        if (!checkCooldown(player.getUuid(), world.getTime(), c))
            return;

        if (stateMachine.is(State.PREVIEW)) {
            // Keep going, transition from PREVIEW -> LOCKED_MINING later
        } else if (!stateMachine.is(State.IDLE)) {
            queue.interrupt();
            stateMachine.force(State.IDLE);
        }

        Direction hitFace = approximateHitFace(player);
        OrientationContext ctx = OrientationContext.of(
                hitFace,
                OrientationContext.facingFromYaw(player.getYaw()));

        MiningStrategy strategy = (customStrategy != null)
                ? customStrategy
                : StrategyRegistry.get(this.playerShape);
        int maxBlocksToMine = this.playerMaxBlocks - 1;

        // 1. Khởi tạo Cache và lựa chọn Pipeline Filter cho tác vụ nội bộ Server
        FilterModeManager.FilterCache cache = new FilterModeManager.FilterCache();
        FilterModeManager.BlockFilter filter = FilterModeManager.resolveFilter(strategy.getModeType(), maxBlocksToMine);

        // Track initial item
        this.initialItem = player.getMainHandStack().getItem();

        // 2. Nạp toàn bộ dữ liệu vào MiningRequest để Strategy xử lý (Contextual
        // Injection)
        MiningStrategy.MiningRequest req = new MiningStrategy.MiningRequest(
                world, player, player.getMainHandStack(),
                origin, originState, maxBlocksToMine, ctx, filter, cache, activeBlacklist,
                c.requireCorrectTool, State.LOCKED_MINING, initialItem, c.allowHeldItemChange);

        // 3. Tiến hành thu thập khối theo Filter Mode mới
        List<BlockPos> found = strategy.collectBlocks(req);

        if (found.isEmpty()) {
            stateMachine.force(State.IDLE);
            return;
        }

        this.originalState = originState;

        queue.reset();
        queue.enqueue(found, world);
        
        lockedSnapshot = new HashSet<>(found);
        renderSnapshot = new HashSet<>(lockedSnapshot);
        targetCount = lockedSnapshot.size();
        brokenCount = 0;

        stateMachine.force(State.LOCKED_MINING);
        SessionStats.onVeinMineStart();
        if (player instanceof ServerPlayerEntity spe)
            ServerPlayNetworking.send(spe, new MiningStatePayload(true));
    }

    public void onServerTick(PlayerEntity player, ServerWorld world) {
        if (stateMachine.is(State.FINISHED) || stateMachine.is(State.CANCELLED)) {
            stateMachine.force(State.IDLE);
            return;
        }

        if (!stateMachine.is(State.LOCKED_MINING))
            return;

        // [CRITICAL] Item Lock Constraint (Phase 4/5)
        ModConfig c = ConfigManager.get();
        if (!c.allowHeldItemChange && initialItem != null && player.getMainHandStack().getItem() != initialItem) {
            stopMining(player);
            return;
        }

        if (!TCVeinMinerMod.playersHoldingV.contains(player.getUuid())) {
            stopMining(player);
            return;
        }

        if (queue.isEmpty()) {
            if (lockedSnapshot.isEmpty()) {
                finalizeMining(player);
            }
            return;
        }

        int blocksPerTick = c.tickSliceSize > 0 ? c.tickSliceSize : 4;
        List<Entry> batch = queue.drainForTick(world, blocksPerTick);

        boolean snapshotChanged = false;
        // Set guard BEFORE breaking any block so AFTER event is blocked
        isMining = true;
        try {
            for (Entry e : batch) {
                if (!(player instanceof ServerPlayerEntity spe))
                    break;

                String id = blockId(world.getBlockState(e.pos()));
                BlockState currentState = world.getBlockState(e.pos());
                
                // [CRITICAL] Phase 5 Constraint: block identity
                if (originalState != null && currentState.getBlock() != originalState.getBlock()) {
                    continue;
                }

                // [CRITICAL] Use tryBreakBlock for full vanilla pipeline:
                boolean broken = spe.interactionManager.tryBreakBlock(e.pos());
                if (!broken)
                    continue;

                lockedSnapshot.remove(e.pos());
                snapshotChanged = true;

                SessionStats.onBlockBroken(id);
                brokenCount++;

                // Tool may have broken inside tryBreakBlock — check
                if (c.requireCorrectTool && player.getMainHandStack().isEmpty()) {
                    stopMining(player);
                    return;
                }

                // [CRITICAL] Explicitly track durability cost for stats.
                if (c.consumeDurability) {
                    SessionStats.onDurabilityUsed(1);
                }
            }
        } finally {
            isMining = false;
        }

        if (snapshotChanged) {
            renderSnapshot = new HashSet<>(lockedSnapshot);
            if (player instanceof ServerPlayerEntity spe) {
                ServerPlayNetworking.send(spe, new HighlightBlockListPayload(new ArrayList<>(renderSnapshot), this.playerShape));
            }
        }

        if (queue.isEmpty() && lockedSnapshot.isEmpty()) {
            finalizeMining(player);
        }
    }

    public Set<BlockPos> getRenderSnapshot() {
        return renderSnapshot;
    }

    public State getState() {
        return stateMachine.get();
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void stopMining(PlayerEntity player) {
        isMining = false;
        queue.interrupt();
        stateMachine.force(State.CANCELLED);
        lockedSnapshot.clear();
        renderSnapshot = Collections.emptySet();
        if (player instanceof ServerPlayerEntity spe) {
            ServerPlayNetworking.send(spe, new MiningStatePayload(false));
            ServerPlayNetworking.send(spe, new HighlightBlockListPayload(Collections.emptyList(), "FACE"));
        }
    }

    private void finalizeMining(PlayerEntity player) {
        isMining = false;
        HudNotifier.lastMined = brokenCount;
        HudNotifier.lastMax = targetCount;
        HudNotifier.notifyAt = System.currentTimeMillis() + 2500;
        stateMachine.force(State.FINISHED);
        lockedSnapshot.clear();
        renderSnapshot = Collections.emptySet();
        if (player instanceof ServerPlayerEntity spe) {
            ServerPlayNetworking.send(spe, new MiningStatePayload(false));
            ServerPlayNetworking.send(spe, new HighlightBlockListPayload(Collections.emptyList(), "FACE"));
        }
    }

    // Shared cooldown map — cleared in removePlayer() on disconnect
    private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    private static boolean checkCooldown(UUID uuid, long tick, ModConfig c) {
        if (c.cooldownTicks <= 0)
            return true;
        long avail = cooldowns.getOrDefault(uuid, 0L);
        if (tick < avail)
            return false;
        cooldowns.put(uuid, tick + c.cooldownTicks);
        return true;
    }

    private static String blockId(BlockState state) {
        return Registries.BLOCK.getId(state.getBlock()).toString();
    }

    private static Set<String> mergeBlacklists(Set<String> configBlacklist, Set<String> playerBlacklist) {
        Set<String> normalized = new HashSet<>(FilterModeManager.normalizeBlacklist(configBlacklist));
        normalized.addAll(FilterModeManager.normalizeBlacklist(playerBlacklist));
        return normalized;
    }

    private static Direction approximateHitFace(PlayerEntity player) {
        float pitch = player.getPitch();
        if (pitch > 60f)
            return Direction.UP;
        if (pitch < -60f)
            return Direction.DOWN;
        return OrientationContext.facingFromYaw(player.getYaw()).getOpposite();
    }
}
