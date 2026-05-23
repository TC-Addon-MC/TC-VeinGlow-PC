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
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
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
 * [CRITICAL] Enchantment/XP/exhaustion: uses ServerPlayerEntity.interactionManager
 * .tryBreakBlock() which runs full vanilla break pipeline.
 * [CRITICAL] Tool check every tick: toolOk() called in onServerTick, not just trigger.
 * [CRITICAL] Tool break: full vanilla pipeline handles it correctly.
 * [CRITICAL] Cooldowns/playersHoldingV never cleaned → moved to disconnect handler
 * in TCVeinMinerMod.
 * [SECURITY] maxBlocks clamped server-side in TCVeinMinerMod packet handler.
 * [FEATURE]  Đã tích hợp FilterModeManager và MiningRequest vào quy trình BFS.
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
    private volatile Set<BlockPos> renderSnapshot = Collections.emptySet();

    /**
     * RE-ENTRY GUARD: set to true while engine is breaking blocks.
     * onBreakTrigger checks this flag and returns immediately if true,
     * preventing the AFTER break event from recursing back into the engine.
     */
    private boolean isMining = false;

    private int brokenCount = 0;
    private int targetCount = 0;
    private String playerShape    = "FACE";
    private int    playerMaxBlocks = 64;
    private MiningStrategy customStrategy = null;

    private MiningEngine() {}

    public void updatePlayerConfig(String shapeId, int maxBlocks) {
        updatePlayerConfig(shapeId, maxBlocks, "");
    }

    public void updatePlayerConfig(String shapeId, int maxBlocks, String equation) {
        this.playerMaxBlocks  = maxBlocks;
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
        if (!evaluator.isValid()) return null;
        return new CustomEquationStrategy(id, evaluator);
    }

    // ── Public API ───────────────────────────────────────────────────────────

    public void onBreakTrigger(PlayerEntity player, ServerWorld world,
                               BlockPos origin, BlockState originState) {
        // [CRITICAL] Re-entry guard: if we are already breaking blocks, the AFTER event
        // fired by our own world.interactionManager.tryBreakBlock() must be ignored.
        if (isMining) return;

        ModConfig c = ConfigManager.get();
        if (!c.enabled) return;
        if (!TCVeinMinerMod.playersHoldingV.contains(player.getUuid())) return;
        if (c.requireCorrectTool && !toolOk(player, c)) return;
        if (c.blacklistedBlocks.contains(blockId(originState))) return;
        if (c.requireSneak && !player.isSneaking()) return;
        if (!checkCooldown(player.getUuid(), world.getTime(), c)) return;

        if (!stateMachine.is(State.IDLE)) {
            queue.interrupt();
            stateMachine.force(State.IDLE);
        }

        stateMachine.transition(State.SCANNING);

        Direction hitFace = approximateHitFace(player);
        OrientationContext ctx = OrientationContext.of(
                hitFace,
                OrientationContext.facingFromYaw(player.getYaw())
        );

        MiningStrategy strategy = (customStrategy != null)
                ? customStrategy
                : StrategyRegistry.get(this.playerShape);
        int maxBlocksToMine = this.playerMaxBlocks - 1;

        // 1. Khởi tạo Cache và lựa chọn Pipeline Filter cho tác vụ nội bộ Server
        FilterModeManager.FilterCache cache = new FilterModeManager.FilterCache();
        FilterModeManager.BlockFilter filter;

        switch (strategy.getModeType()) {
            case TREE_CAPITATOR -> filter = FilterModeManager.Presets.TREE_CAPITATOR(maxBlocksToMine);
            case TUNNEL, SHAPE  -> filter = FilterModeManager.Composite.and(
                    FilterModeManager.Presets.BASE_SAFETY,
                    FilterModeManager.Filters.maxVisited(maxBlocksToMine),
                    FilterModeManager.Filters.sameBlock() // Tunnel/Shape mặc định chỉ đào block cùng loại với block vừa phá
            );
            default -> filter = FilterModeManager.Presets.VEIN_ORE(maxBlocksToMine);
        }

        // 2. Nạp toàn bộ dữ liệu vào MiningRequest để Strategy xử lý (Contextual Injection)
        MiningStrategy.MiningRequest req = new MiningStrategy.MiningRequest(
                world, player, player.getMainHandStack(),
                origin, originState, maxBlocksToMine, ctx, filter, cache
        );

        // 3. Tiến hành thu thập khối theo Filter Mode mới
        List<BlockPos> found = strategy.collectBlocks(req);

        if (found.isEmpty()) {
            stateMachine.force(State.IDLE);
            return;
        }

        stateMachine.transition(State.QUEUEING);
        queue.reset();
        queue.enqueue(found, world);
        renderSnapshot = queue.snapshot();
        targetCount = queue.size();
        brokenCount = 0;

        stateMachine.transition(State.MINING);
        SessionStats.onVeinMineStart();
    }

    public void onServerTick(PlayerEntity player, ServerWorld world) {
        if (!stateMachine.is(State.MINING)) return;

        // [CRITICAL] Check tool validity every tick, not just at trigger
        ModConfig c = ConfigManager.get();
        if (c.requireCorrectTool && !toolOk(player, c)) {
            stopMining();
            return;
        }

        if (queue.isEmpty()) { finalizeMining(player); return; }

        if (!TCVeinMinerMod.playersHoldingV.contains(player.getUuid())) {
            stopMining();
            return;
        }

        int blocksPerTick = c.tickSliceSize > 0 ? c.tickSliceSize : 4;
        List<Entry> batch = queue.drainForTick(world, blocksPerTick);

        // Set guard BEFORE breaking any block so AFTER event is blocked
        isMining = true;
        try {
            for (Entry e : batch) {
                if (!(player instanceof ServerPlayerEntity spe)) break;

                String id = blockId(world.getBlockState(e.pos()));

                // [CRITICAL] Use tryBreakBlock for full vanilla pipeline:
                boolean broken = spe.interactionManager.tryBreakBlock(e.pos());
                if (!broken) continue;

                SessionStats.onBlockBroken(id);
                brokenCount++;

                // Tool may have broken inside tryBreakBlock — check
                if (c.requireCorrectTool && player.getMainHandStack().isEmpty()) {
                    stopMining();
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

        renderSnapshot = queue.snapshot();
        if (queue.isEmpty()) finalizeMining(player);
    }

    public Set<BlockPos> getRenderSnapshot() { return renderSnapshot; }
    public State getState()                  { return stateMachine.get(); }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void stopMining() {
        isMining = false;
        queue.interrupt();
        stateMachine.force(State.INTERRUPTED);
        stateMachine.transition(State.IDLE);
        renderSnapshot = Collections.emptySet();
    }

    private void finalizeMining(PlayerEntity player) {
        isMining = false;
        HudNotifier.lastMined = brokenCount;
        HudNotifier.lastMax   = targetCount;
        HudNotifier.notifyAt  = System.currentTimeMillis() + 2500;
        stateMachine.force(State.IDLE);
        renderSnapshot = Collections.emptySet();
    }

    // Shared cooldown map — cleared in removePlayer() on disconnect
    private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    private static boolean checkCooldown(UUID uuid, long tick, ModConfig c) {
        if (c.cooldownTicks <= 0) return true;
        long avail = cooldowns.getOrDefault(uuid, 0L);
        if (tick < avail) return false;
        cooldowns.put(uuid, tick + c.cooldownTicks);
        return true;
    }

    private static boolean toolOk(PlayerEntity p, ModConfig c) {
        ItemStack s = p.getMainHandStack();
        if (s.isEmpty())                        return c.enabledTools.getOrDefault("hand",    false);
        if (s.getItem() instanceof PickaxeItem) return c.enabledTools.getOrDefault("pickaxe", true);
        if (s.getItem() instanceof AxeItem)     return c.enabledTools.getOrDefault("axe",     true);
        if (s.getItem() instanceof ShovelItem)  return c.enabledTools.getOrDefault("shovel",  false);
        if (s.getItem() instanceof SwordItem)   return c.enabledTools.getOrDefault("sword",   false);
        if (s.getItem() instanceof HoeItem)     return c.enabledTools.getOrDefault("hoe",     false);
        return false;
    }

    private static String blockId(BlockState state) {
        return Registries.BLOCK.getId(state.getBlock()).toString();
    }

    private static Direction approximateHitFace(PlayerEntity player) {
        float pitch = player.getPitch();
        if (pitch > 60f)  return Direction.UP;
        if (pitch < -60f) return Direction.DOWN;
        return OrientationContext.facingFromYaw(player.getYaw()).getOpposite();
    }
}
