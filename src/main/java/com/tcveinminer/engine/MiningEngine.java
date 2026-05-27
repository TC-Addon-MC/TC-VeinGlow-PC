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
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

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

    public static boolean hasEngine(UUID uuid) {
        return ENGINES.containsKey(uuid);
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

    private BlockPos treeOriginPos = null;
    private List<BlockPos> baseLogsBroken = new ArrayList<>();
    private BlockState treeSaplingType = null;

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
        // Ignore dynamic updates if already locked/finished
        if (stateMachine.is(State.LOCKED_MINING) || stateMachine.is(State.FINISHED) || stateMachine.is(State.CANCELLED)) {
            return;
        }

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

        // Chỉ kích hoạt TreeCapitator nếu block bị đập trực tiếp là Gỗ (Log)
        if ("TREE_CAP".equals(strategy.getModeType()) && !originState.isIn(BlockTags.LOGS)) {
            return;
        }

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
        this.treeOriginPos = origin;
        this.baseLogsBroken = new ArrayList<>();
        this.treeSaplingType = getSaplingForLog(originState.getBlock()); // lưu lại phòng khi không có lá rợt drop

        queue.reset();
        queue.enqueue(found, world);
        
        lockedSnapshot = new HashSet<>(found);
        renderSnapshot = new HashSet<>(lockedSnapshot);
        targetCount = lockedSnapshot.size();
        brokenCount = 0;

        stateMachine.force(State.LOCKED_MINING);
        SessionStats.onVeinMineStart();
        if (player instanceof ServerPlayerEntity spe) {
            ServerPlayNetworking.send(spe, new MiningStatePayload(1, 0, targetCount));
        }
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
        
        if (player instanceof ServerPlayerEntity spe) {
            ServerPlayNetworking.send(spe, new MiningStatePayload(1, brokenCount, targetCount));
        }
        try {
            for (Entry e : batch) {
                if (!(player instanceof ServerPlayerEntity spe))
                    break;

                String id = blockId(world.getBlockState(e.pos()));
                BlockState currentState = world.getBlockState(e.pos());
                
                // [CRITICAL] Phase 5 Constraint: block identity
                if (originalState != null && currentState.getBlock() != originalState.getBlock()) {
                    if ("TREE_CAP".equals(this.playerShape) && (currentState.isIn(BlockTags.LOGS) || currentState.isIn(BlockTags.LEAVES))) {
                        // Cho phép lá và các loại gỗ khác trong cùng cái cây
                    } else {
                        lockedSnapshot.remove(e.pos());
                        snapshotChanged = true;
                        continue;
                    }
                }

                // [CRITICAL] Use tryBreakBlock for full vanilla pipeline:
                boolean broken = spe.interactionManager.tryBreakBlock(e.pos());
                if (!broken)
                    continue;
                
                // Hiển thị hiệu ứng đập block cho chính người chơi (vì server tự đập nên client không có)
                world.syncWorldEvent(null, 2001, e.pos(), Block.getRawIdFromState(currentState));
                
                if ("TREE_CAP".equals(this.playerShape) && currentState.isIn(BlockTags.LOGS)) {
                    // Thu thập gỗ gốc (gỗ thấp nhất tiếp xúc với đất bên dưới)
                    BlockState below = world.getBlockState(e.pos().down());
                    if (isSoilForSapling(below)) {
                        baseLogsBroken.add(e.pos());
                    }
                }

                lockedSnapshot.remove(e.pos());
                snapshotChanged = true;

                SessionStats.onBlockBroken(id);
                brokenCount++;

                // Tool may have broken inside tryBreakBlock — check
                if (c.requireCorrectTool && initialItem != net.minecraft.item.Items.AIR && player.getMainHandStack().isEmpty()) {
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
            ServerPlayNetworking.send(spe, new MiningStatePayload(3, brokenCount, targetCount));
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
            ServerPlayNetworking.send(spe, new MiningStatePayload(2, brokenCount, targetCount));
            ServerPlayNetworking.send(spe, new HighlightBlockListPayload(Collections.emptyList(), "FACE"));
        }

        // Tự động trồng lại mầm cây (Auto-Replant)
        if ("TREE_CAP".equals(this.playerShape) && !baseLogsBroken.isEmpty()) {
            ServerWorld sw = (ServerWorld) player.getWorld();
            autoReplant(sw, player);
        }
    }

    /**
     * Trồng lại cây sau khi chặt.
     * Logic:
     *  1. Tìm các item sapling trong vùng xưng quanh vị trí gốc cây.
     *  2. Nếu là cây 2×2 (dark_oak / jungle), kiểm tra đủ 4 mầm mới trồng.
     *  3. Với cây 1×1 thông thường, mỗi vị trí gốc tiêu 1 mầm.
     */
    private void autoReplant(ServerWorld sw, PlayerEntity player) {
        if (baseLogsBroken.isEmpty()) return;

        // Xác định loại mầm cần tìm
        BlockState saplingState = treeSaplingType;
        if (saplingState == null) return;
        net.minecraft.item.Item saplingItem = saplingState.getBlock().asItem();
        if (saplingItem == net.minecraft.item.Items.AIR) return;

        // Tập trung tâm tìm kiếm quanh gốc cây
        BlockPos searchCenter = treeOriginPos != null ? treeOriginPos : baseLogsBroken.get(0);
        Box searchBox = new Box(searchCenter).expand(8.0);
        List<ItemEntity> nearbyDrops = sw.getEntitiesByClass(ItemEntity.class, searchBox,
                ent -> ent.getStack().getItem() == saplingItem);

        // Đếm tổng số mầm có sẵn
        int totalSaplings = nearbyDrops.stream().mapToInt(e -> e.getStack().getCount()).sum();

        boolean isLarge = isLargeTree(saplingState);

        if (isLarge) {
            // Cây 2×2: cần đúng 4 mầm và 4 vị trí gốc 2×2
            List<BlockPos> replantPositions = get2x2BasePositions(sw);
            if (replantPositions.size() == 4 && totalSaplings >= 4) {
                for (BlockPos pos : replantPositions) {
                    if (sw.getBlockState(pos).isAir() &&
                            isSoilForSapling(sw.getBlockState(pos.down()))) {
                        sw.setBlockState(pos, saplingState);
                        consumeSapling(nearbyDrops, 1);
                    }
                }
            }
        } else {
            // Cây 1×1: mỗi vị trí gốc tiêu 1 mầm
            for (BlockPos pos : baseLogsBroken) {
                if (totalSaplings <= 0) break;
                BlockState below = sw.getBlockState(pos.down());
                if (isSoilForSapling(below) && sw.getBlockState(pos).isAir()) {
                    sw.setBlockState(pos, saplingState);
                    consumeSapling(nearbyDrops, 1);
                    totalSaplings--;
                }
            }
        }
    }

    /** Tiêu thụ `count` mầm từ danh sách entity drops. */
    private void consumeSapling(List<ItemEntity> drops, int count) {
        int remaining = count;
        for (ItemEntity ent : drops) {
            if (remaining <= 0) break;
            if (ent.isRemoved()) continue;
            int stackCount = ent.getStack().getCount();
            if (stackCount <= remaining) {
                remaining -= stackCount;
                ent.discard();
            } else {
                ent.getStack().decrement(remaining);
                remaining = 0;
            }
        }
    }

    /** Kiểm tra cây có phải loại 2×2 không (dark oak / jungle). */
    private boolean isLargeTree(BlockState saplingState) {
        Block b = saplingState.getBlock();
        return b == Blocks.DARK_OAK_SAPLING || b == Blocks.JUNGLE_SAPLING;
    }

    /**
     * Tìm 4 vị trí gốc cho cây 2×2:
     * Lấy các gỗ gốc đã đào, nhóm thành cụm 2×2, trả về 4 vị trí góc.
     */
    private List<BlockPos> get2x2BasePositions(ServerWorld sw) {
        if (baseLogsBroken.isEmpty()) return Collections.emptyList();
        // Lấy Y nhỏ nhất (gốc cây)
        int minY = baseLogsBroken.stream().mapToInt(BlockPos::getY).min().orElse(0);
        List<BlockPos> baseLogs = baseLogsBroken.stream()
                .filter(p -> p.getY() == minY)
                .collect(java.util.stream.Collectors.toList());
        if (baseLogs.size() < 4) return Collections.emptyList();

        // Tìm minX, minZ trong các gốc
        int minX = baseLogs.stream().mapToInt(BlockPos::getX).min().orElse(0);
        int minZ = baseLogs.stream().mapToInt(BlockPos::getZ).min().orElse(0);
        List<BlockPos> candidates = List.of(
                new BlockPos(minX, minY, minZ),
                new BlockPos(minX + 1, minY, minZ),
                new BlockPos(minX, minY, minZ + 1),
                new BlockPos(minX + 1, minY, minZ + 1)
        );
        return candidates;
    }

    /** Kiểm tra block có phải đất hợp lệ để trồng cây không. */
    private boolean isSoilForSapling(BlockState state) {
        Block b = state.getBlock();
        return state.isIn(BlockTags.DIRT)
                || b == Blocks.GRASS_BLOCK
                || b == Blocks.PODZOL
                || b == Blocks.MYCELIUM
                || b == Blocks.FARMLAND
                || b == Blocks.ROOTED_DIRT;
    }

    private BlockState getSaplingForLog(Block log) {
        Identifier id = Registries.BLOCK.getId(log);
        String path = id.getPath();
        if (path.contains("oak") && !path.contains("dark_oak")) return Blocks.OAK_SAPLING.getDefaultState();
        if (path.contains("spruce")) return Blocks.SPRUCE_SAPLING.getDefaultState();
        if (path.contains("birch")) return Blocks.BIRCH_SAPLING.getDefaultState();
        if (path.contains("jungle")) return Blocks.JUNGLE_SAPLING.getDefaultState();
        if (path.contains("acacia")) return Blocks.ACACIA_SAPLING.getDefaultState();
        if (path.contains("dark_oak")) return Blocks.DARK_OAK_SAPLING.getDefaultState();
        if (path.contains("mangrove")) return Blocks.MANGROVE_PROPAGULE.getDefaultState();
        if (path.contains("cherry")) return Blocks.CHERRY_SAPLING.getDefaultState();
        return null;
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
