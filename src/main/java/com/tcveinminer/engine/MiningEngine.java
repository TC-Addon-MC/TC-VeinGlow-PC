package com.tcveinminer.engine;

import com.tcveinminer.TCVeinMinerMod;
import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.engine.queue.MiningQueue;
import com.tcveinminer.engine.queue.MiningQueue.Entry;
import com.tcveinminer.engine.state.MiningStateMachine;
import com.tcveinminer.engine.state.MiningStateMachine.State;
import com.tcveinminer.engine.strategy.MiningStrategy;
import com.tcveinminer.engine.strategy.StrategyRegistry;
import com.tcveinminer.logic.HudNotifier;
import com.tcveinminer.util.SessionStats;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MiningEngine: orchestrator trung tâm thay thế VeinMinerLogic.
 *
 * Mỗi player có một engine instance riêng (per-UUID).
 * Engine điều phối: Scanner → Strategy → Queue → Executor.
 *
 * Renderer đọc queue snapshot từ đây — KHÔNG scan riêng.
 */
public final class MiningEngine {

    // Per-player engine instances
    private static final Map<UUID, MiningEngine> ENGINES = new ConcurrentHashMap<>();

    public static MiningEngine forPlayer(UUID uuid) {
        return ENGINES.computeIfAbsent(uuid, id -> new MiningEngine());
    }

    public static void removePlayer(UUID uuid) {
        ENGINES.remove(uuid);
    }

    // --- Instance ---

    private final MiningStateMachine stateMachine = new MiningStateMachine();
    private final MiningQueue queue = new MiningQueue();

    // Snapshot dùng cho renderer — updated sau mỗi lần queue rebuild
    // Volatile vì renderer đọc từ client thread, engine chạy trên server thread
    private volatile Set<BlockPos> renderSnapshot = Collections.emptySet();

    private int brokenCount = 0;
    private int targetCount = 0;
    private String playerShape = "FACE";
    private int playerMaxBlocks = 64;

    public void updatePlayerConfig(String shapeId, int maxBlocks) {
        this.playerShape = shapeId;
        this.playerMaxBlocks = maxBlocks;
    }
    private MiningEngine() {}

    // ─────────────────────────────────────────────────────────────────────────
    // Public API — gọi từ event handlers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Trigger khi player break một block và đang giữ phím.
     * Pipeline: IDLE → SCANNING → QUEUEING → bắt đầu MINING.
     * Phần execute thật sự xảy ra ở onServerTick().
     */
    public void onBreakTrigger(PlayerEntity player, ServerWorld world,
                                BlockPos origin, BlockState originState) {
        ModConfig c = ConfigManager.get();

        // Pre-condition checks
        if (!c.enabled) return;
        if (!TCVeinMinerMod.playersHoldingV.contains(player.getUuid())) return;
        if (c.requireCorrectTool && !toolOk(player, c)) return;
        if (c.blacklistedBlocks.contains(blockId(originState))) return;

        // Cooldown check
        if (!checkCooldown(player.getUuid(), world.getTime(), c)) return;

        // Nếu đang mining từ break trước, interrupt trước khi bắt đầu đợt mới
        if (!stateMachine.is(State.IDLE)) {
            queue.interrupt();
            stateMachine.force(State.IDLE);
        }

        // SCANNING

        stateMachine.transition(State.SCANNING);
        MiningStrategy strategy = StrategyRegistry.get(this.playerShape);
        List<BlockPos> found = strategy.collectBlocks(world, origin, originState, this.playerMaxBlocks - 1);
        if (found.isEmpty()) {
            stateMachine.force(State.IDLE);
            return;
        }

        // QUEUEING
        stateMachine.transition(State.QUEUEING);
        queue.reset();
        queue.enqueue(found, world);
        renderSnapshot = queue.snapshot(); // Renderer sees queued blocks immediately

        targetCount = queue.size();
        brokenCount = 0;

        stateMachine.transition(State.MINING);
        SessionStats.onVeinMineStart();
    }

    /**
     * Gọi mỗi server tick để execute blocks từ queue theo batch.
     * Tick-sliced: tối đa BLOCKS_PER_TICK mỗi tick.
     */
    public void onServerTick(PlayerEntity player, ServerWorld world) {
        if (!stateMachine.is(State.MINING)) return;
        if (queue.isEmpty()) {
            finalize(player);
            return;
        }

        // Interrupt nếu player không còn giữ phím
        if (!TCVeinMinerMod.playersHoldingV.contains(player.getUuid())) {
            queue.interrupt();
            stateMachine.force(State.INTERRUPTED);
            stateMachine.transition(State.IDLE);
            renderSnapshot = Collections.emptySet();
            return;
        }

        ModConfig c = ConfigManager.get();
        ItemStack tool = player.getMainHandStack();
        int blocksPerTick = c.tickSliceSize > 0 ? c.tickSliceSize : 4;

        List<Entry> batch = queue.drainForTick(world, blocksPerTick);

        for (Entry e : batch) {
            String id = blockId(world.getBlockState(e.pos()));
            world.breakBlock(e.pos(), true, player);
            SessionStats.onBlockBroken(id);
            brokenCount++;

            if (c.consumeDurability && !tool.isEmpty() && tool.isDamageable()) {
                tool.damage(1, player, EquipmentSlot.MAINHAND);
                SessionStats.onDurabilityUsed(1);
                if (tool.isEmpty()) {
                    queue.interrupt();
                    stateMachine.force(State.INTERRUPTED);
                    stateMachine.transition(State.IDLE);
                    renderSnapshot = Collections.emptySet();
                    return;
                }
            }
        }

        // Update render snapshot sau mỗi batch
        renderSnapshot = queue.snapshot();

        if (queue.isEmpty()) finalize(player);
    }

    /** Renderer gọi đây để lấy blocks cần highlight — KHÔNG scan riêng. */
    public Set<BlockPos> getRenderSnapshot() {
        return renderSnapshot;
    }

    public MiningStateMachine.State getState() {
        return stateMachine.get();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void finalize(PlayerEntity player) {
        HudNotifier.lastMined = brokenCount;
        HudNotifier.lastMax   = targetCount;
        HudNotifier.notifyAt  = System.currentTimeMillis() + 2500;
        stateMachine.force(State.IDLE);
        renderSnapshot = Collections.emptySet();
    }

    // ─── Cooldown ────────────────────────────────────────────────────────────

    private static final Map<UUID, Long> cooldowns = new HashMap<>();

    private static boolean checkCooldown(UUID uuid, long tick, ModConfig c) {
        if (c.cooldownTicks <= 0) return true;
        long avail = cooldowns.getOrDefault(uuid, 0L);
        if (tick < avail) return false;
        cooldowns.put(uuid, tick + c.cooldownTicks);
        return true;
    }

    // ─── Tool check ──────────────────────────────────────────────────────────

    private static boolean toolOk(PlayerEntity p, ModConfig c) {
        ItemStack s = p.getMainHandStack();
        if (s.isEmpty())                          return c.enabledTools.getOrDefault("hand",    false);
        if (s.getItem() instanceof PickaxeItem)   return c.enabledTools.getOrDefault("pickaxe", true);
       if (s.getItem() instanceof AxeItem)       return c.enabledTools.getOrDefault("axe",     false);
        if (s.getItem() instanceof ShovelItem)    return c.enabledTools.getOrDefault("shovel",  false);
        if (s.getItem() instanceof SwordItem)     return c.enabledTools.getOrDefault("sword",   false);
        if (s.getItem() instanceof HoeItem)       return c.enabledTools.getOrDefault("hoe",     false);
        return false;
    }

    private static String blockId(BlockState state) {
        return Registries.BLOCK.getId(state.getBlock()).toString();
    }
}
