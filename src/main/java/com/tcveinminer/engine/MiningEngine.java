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
 * MiningEngine: orchestrator trung tâm.
 * Mỗi player có một engine instance riêng (per-UUID).
 */
public final class MiningEngine {

    private static final Map<UUID, MiningEngine> ENGINES = new ConcurrentHashMap<>();

    public static MiningEngine forPlayer(UUID uuid) {
        return ENGINES.computeIfAbsent(uuid, id -> new MiningEngine());
    }

    public static void removePlayer(UUID uuid) { ENGINES.remove(uuid); }

    // --- Instance state ---
    private final MiningStateMachine stateMachine = new MiningStateMachine();
    private final MiningQueue queue = new MiningQueue();
    private volatile Set<BlockPos> renderSnapshot = Collections.emptySet();

    private int brokenCount = 0;
    private int targetCount = 0;
    private String playerShape    = "FACE";
    private int    playerMaxBlocks = 64;

    private MiningEngine() {}

    public void updatePlayerConfig(String shapeId, int maxBlocks) {
        this.playerShape     = shapeId;
        this.playerMaxBlocks = maxBlocks;
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    public void onBreakTrigger(PlayerEntity player, ServerWorld world,
                                BlockPos origin, BlockState originState) {
        ModConfig c = ConfigManager.get();
        if (!c.enabled) return;
        if (!TCVeinMinerMod.playersHoldingV.contains(player.getUuid())) return;
        if (c.requireCorrectTool && !toolOk(player, c)) return;
        if (c.blacklistedBlocks.contains(blockId(originState))) return;
        if (c.requireSneak && !player.isSneaking()) return;
        if (!checkCooldown(player.getUuid(), world.getTime(), c)) return;

        // Interrupt session trước đó nếu đang chạy
        if (!stateMachine.is(State.IDLE)) {
            queue.interrupt();
            stateMachine.force(State.IDLE);
        }

        stateMachine.transition(State.SCANNING);
        MiningStrategy strategy = StrategyRegistry.get(this.playerShape);
        List<BlockPos> found = strategy.collectBlocks(world, origin, originState, this.playerMaxBlocks - 1);

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
        if (queue.isEmpty()) { finalize(player); return; }

        // Dừng nếu player nhả phím
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

        renderSnapshot = queue.snapshot();
        if (queue.isEmpty()) finalize(player);
    }

    public Set<BlockPos> getRenderSnapshot() { return renderSnapshot; }
    public State getState() { return stateMachine.get(); }

    // ─── Internal ─────────────────────────────────────────────────────────────

    private void finalize(PlayerEntity player) {
        HudNotifier.lastMined = brokenCount;
        HudNotifier.lastMax   = targetCount;
        HudNotifier.notifyAt  = System.currentTimeMillis() + 2500;
        stateMachine.force(State.IDLE);
        renderSnapshot = Collections.emptySet();
    }

    private static final Map<UUID, Long> cooldowns = new HashMap<>();

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
}
