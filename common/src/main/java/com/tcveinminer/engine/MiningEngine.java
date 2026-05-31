package com.tcveinminer.engine;

import com.tcveinminer.api.query.EngineQuery;
import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.engine.left.LeftClickEngine;
import com.tcveinminer.engine.preview.PreviewManager;
import com.tcveinminer.engine.right.RightClickEngine;
import com.tcveinminer.engine.session.ActionSession;
import com.tcveinminer.engine.session.ActionSessionManager;
import com.tcveinminer.engine.state.EngineState;
import com.tcveinminer.engine.action.ActionType;
import com.tcveinminer.engine.capability.CapabilityRegistry;
import com.tcveinminer.engine.filter.LeftClickFilterPipeline;
import com.tcveinminer.engine.filter.RightClickFilterPipeline;
import com.tcveinminer.engine.strategy.CustomEquationStrategy;
import com.tcveinminer.engine.strategy.FilterModeManager;
import com.tcveinminer.engine.strategy.MiningStrategy;
import com.tcveinminer.engine.strategy.StrategyRegistry;
import com.tcveinminer.engine.traversal.OrientationContext;
import com.tcveinminer.network.NetworkManager;
import com.tcveinminer.network.payload.ActivationConfirmData;
import com.tcveinminer.network.payload.FilterResultData;
import com.tcveinminer.network.payload.HighlightDeltaData;
import com.tcveinminer.network.payload.LookedAtBlockData;
import com.tcveinminer.util.ExpressionEvaluator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MiningEngine: per-player thin orchestrator.
 * <p>
 * Delegates all work to independent {@link LeftClickEngine} and
 * {@link RightClickEngine}.
 * Both engines run in parallel — no shared mutable state between them.
 * <p>
 * This class manages:
 * <ul>
 * <li>Player engine lifecycle (create, remove, config propagation)</li>
 * <li>Preview/highlight computation (activation requests)</li>
 * <li>Event delegation (break → left, interact → right, tick → both)</li>
 * </ul>
 */
public final class MiningEngine implements EngineQuery {

    private static final Map<UUID, MiningEngine> ENGINES = new ConcurrentHashMap<>();

    // ── Per-player engines ────────────────────────────────────────────────

    private final LeftClickEngine leftEngine = new LeftClickEngine();
    private final RightClickEngine rightEngine = new RightClickEngine();
    private final PreviewManager previewManager = new PreviewManager();

    // ── Preview cache ─────────────────────────────────────────────────────
    private record PreviewCacheKey(BlockPos pos, String shape, int maxBlocks, net.minecraft.world.item.Item heldItem) {
    }

    private PreviewCacheKey lastPreviewKey = null;
    private List<BlockPos> lastPreviewResult = null;

    // ── Preview config (shared for activation requests) ───────────────────
    private String playerShape = "FACE";
    private int playerMaxBlocks = 64;
    private MiningStrategy customStrategy = null;
    private Set<String> playerBlacklist = new HashSet<>();
    private Set<String> mergedBlacklist = Collections.emptySet();

    private MiningEngine() {
    }

    // ── Static Access ─────────────────────────────────────────────────────

    public static MiningEngine forPlayer(UUID uuid) {
        return ENGINES.computeIfAbsent(uuid, id -> new MiningEngine());
    }

    public static boolean hasEngine(UUID uuid) {
        return ENGINES.containsKey(uuid);
    }

    public static void removePlayer(UUID uuid) {
        MiningEngine engine = ENGINES.remove(uuid);
        if (engine != null) {
            AbstractActionEngine.clearCooldown(uuid);
        }
    }

    // ── Delegates ─────────────────────────────────────────────────────────

    /**
     * Left click — break/vein mine trigger.
     */
    public void onBreakTrigger(Player player, ServerLevel world,
            BlockPos origin, BlockState originState) {
        leftEngine.onBreakTrigger(player, world, origin, originState);
    }

    /**
     * Right click — interact/harvest/plant trigger.
     *
     * @return true if the mod handled the interaction
     */
    public boolean onInteractTrigger(Player player, ServerLevel world,
            InteractionHand InteractionHand, BlockHitResult hitResult) {
        return rightEngine.onInteractTrigger(player, world, InteractionHand, hitResult);
    }

    /**
     * Server tick — ticks BOTH engines independently (parallel).
     */
    public void onServerTick(Player player, ServerLevel world) {
        leftEngine.onServerTick(player, world);
        rightEngine.onServerTick(player, world);
    }

    // ── Preview / Activation Request ──────────────────────────────────────

    /**
     * Handle activation request from client (held V + looking at block).
     * Computes preview highlight for the left-click engine.
     */
    public void handleActivationRequest(ServerPlayer spe, boolean active, BlockPos targetPos) {
        // Ignore dynamic updates if actively processing a vein mine session.
        // FINISHED/CANCELLED are terminal but transient — they will be reset to IDLE
        // on the next server tick. Blocking activation on those states causes highlight
        // to never refresh when the client sends its post-mining activation request,
        // because that request arrives before the server tick that resets the state.
        if (leftEngine.getState() == EngineState.PROCESSING) {
            return;
        }
        // Eagerly clear terminal states so activation can proceed immediately.
        if (leftEngine.getState() == EngineState.FINISHED
                || leftEngine.getState() == EngineState.CANCELLED) {
            leftEngine.forceState(EngineState.IDLE);
        }

        if (!active || targetPos == null) {
            if (leftEngine.getState() == EngineState.PREVIEW) {
                leftEngine.forceState(EngineState.IDLE);
            }
            NetworkManager.sendToPlayer(spe, new ActivationConfirmData(false));
            NetworkManager.sendToPlayer(spe, new FilterResultData(false));

            ActionSession session = ActionSessionManager.getOrCreate(spe.getUUID());
            Set<BlockPos> prev = session.getRenderSnapshot();
            if (!prev.isEmpty()) {
                NetworkManager.sendToPlayer(spe, new HighlightDeltaData(Collections.emptyList(),
                        prev.stream().map(BlockPos::asLong).toList(), "FACE", "LEFT"));
                session.initSnapshot(Collections.emptySet());
            }
            return;
        }

        ModConfig c = ConfigManager.get();
        if (!c.enabled)
            return;

        leftEngine.forceState(EngineState.PREVIEW);

        ServerLevel world = spe.serverLevel();
        if (!world.hasChunk(targetPos.getX() >> 4, targetPos.getZ() >> 4))
            return;

        BlockState targetState = world.getBlockState(targetPos);
        if (targetState.isAir()) {
            NetworkManager.sendToPlayer(spe, new FilterResultData(false));
            return;
        }

        if (spe.getMainHandItem().getItem() == Items.BUCKET
                && !(targetState.getBlock() instanceof LiquidBlock && targetState.getFluidState().isSource())) {
            NetworkManager.sendToPlayer(spe, new ActivationConfirmData(false));
            NetworkManager.sendToPlayer(spe, new FilterResultData(false));
            ActionSession session = ActionSessionManager.getOrCreate(spe.getUUID());
            Set<BlockPos> prev = session.getRenderSnapshot();
            if (!prev.isEmpty()) {
                NetworkManager.sendToPlayer(spe, new HighlightDeltaData(
                        Collections.emptyList(), prev.stream().map(BlockPos::asLong).toList(), "FACE", "LEFT"));
                session.initSnapshot(Collections.emptySet());
            }
            return;
        }

        Set<String> activeBlacklist = mergedBlacklist;
        boolean isBlacklisted = activeBlacklist.contains(blockId(targetState));

        if (isBlacklisted) {
            NetworkManager.sendToPlayer(spe, new ActivationConfirmData(false));
            NetworkManager.sendToPlayer(spe,
                    new LookedAtBlockData(java.util.Optional.ofNullable(targetPos).map(BlockPos::asLong)));
            NetworkManager.sendToPlayer(spe, new FilterResultData(false));
            return;
        }

        Direction hitFace = approximateHitFace(spe);
        OrientationContext ctx = OrientationContext.of(hitFace, OrientationContext.facingFromYaw(spe.getYRot()));

        MiningStrategy strategy;
        int maxBlocksToMine = this.playerMaxBlocks - 1;
        FilterModeManager.BlockFilter filter;

        ActionType rightAction = CapabilityRegistry.resolve(spe, spe.getMainHandItem(), targetState,
                InteractionHand.MAIN_HAND);

        if (rightAction != ActionType.VANILLA_FALLBACK && rightAction != ActionType.USE_ITEM) {
            strategy = (customStrategy != null) ? customStrategy : StrategyRegistry.get(this.playerShape);

            if (rightAction == ActionType.FLUID_SCOOP) {
                int availableBuckets = 0;
                for (int i = 0; i < spe.getInventory().getContainerSize(); i++) {
                    ItemStack stack = spe.getInventory().getItem(i);
                    if (!stack.isEmpty() && stack.getItem() == Items.BUCKET) {
                        availableBuckets += stack.getCount();
                    }
                }
                int bucketMax = Math.max(0, Math.min(this.playerMaxBlocks, availableBuckets) - 1);
                if (bucketMax < maxBlocksToMine)
                    maxBlocksToMine = bucketMax;
                strategy = StrategyRegistry.get("FACE");
            }

            filter = RightClickFilterPipeline.forAction(rightAction, maxBlocksToMine);
        } else {
            strategy = (customStrategy != null) ? customStrategy : StrategyRegistry.get(this.playerShape);
            ActionType leftAction = ("TREE_CAP".equals(this.playerShape)
                    && targetState.is(net.minecraft.tags.BlockTags.LOGS))
                            ? ActionType.TREE_CAP
                            : ActionType.BREAK;

            filter = LeftClickFilterPipeline.forAction(leftAction, strategy.getModeType(), maxBlocksToMine);
        }

        FilterModeManager.FilterCache cache = new FilterModeManager.FilterCache();

        FilterModeManager.FilterContext fCtxTarget = new FilterModeManager.FilterContext(
                world, spe, spe.getMainHandItem(), targetPos, targetPos,
                targetState, targetState, Direction.UP, 0, 0, 0,
                strategy.getModeType(), cache, activeBlacklist, c.requireHarvestCapability);

        if (!filter.test(fCtxTarget)) {
            NetworkManager.sendToPlayer(spe, new ActivationConfirmData(false));
            NetworkManager.sendToPlayer(spe,
                    new LookedAtBlockData(java.util.Optional.ofNullable(targetPos).map(BlockPos::asLong)));
            NetworkManager.sendToPlayer(spe, new FilterResultData(false));
            return;
        }

        NetworkManager.sendToPlayer(spe, new ActivationConfirmData(true));
        NetworkManager.sendToPlayer(spe,
                new LookedAtBlockData(java.util.Optional.ofNullable(targetPos).map(BlockPos::asLong)));
        NetworkManager.sendToPlayer(spe, new FilterResultData(true));

        PreviewCacheKey key = new PreviewCacheKey(
                targetPos, this.playerShape, this.playerMaxBlocks,
                spe.getMainHandItem().getItem());

        List<BlockPos> found;
        if (key.equals(lastPreviewKey) && lastPreviewResult != null) {
            found = lastPreviewResult;
        } else {
            MiningStrategy.MiningRequest req = new MiningStrategy.MiningRequest(
                    world, spe, spe.getMainHandItem(),
                    targetPos, targetState, maxBlocksToMine, ctx, filter, cache, activeBlacklist,
                    c.requireHarvestCapability, EngineState.PREVIEW, spe.getMainHandItem().getItem(),
                    c.allowHeldItemChange);
            found = strategy.collectBlocks(req);
            lastPreviewKey = key;
            lastPreviewResult = found;
        }
        if (!found.contains(targetPos)) {
            found.add(targetPos);
        }

        ActionSession session = ActionSessionManager.getOrCreate(spe.getUUID());
        Set<BlockPos> previous = session.getRenderSnapshot();
        Set<BlockPos> current = new HashSet<>(found);

        List<BlockPos> added = new ArrayList<>();
        for (BlockPos p : current) {
            if (!previous.contains(p))
                added.add(p);
        }

        List<BlockPos> removed = new ArrayList<>();
        for (BlockPos p : previous) {
            if (!current.contains(p))
                removed.add(p);
        }

        if (!added.isEmpty() || !removed.isEmpty()) {
            NetworkManager.sendToPlayer(spe, new HighlightDeltaData(added.stream().map(BlockPos::asLong).toList(),
                    removed.stream().map(BlockPos::asLong).toList(), this.playerShape, "LEFT"));
            session.initSnapshot(current);
            session.updateTime();
        }
    }

    // ── Config ────────────────────────────────────────────────────────────

    public void updatePlayerConfig(String shapeId, int maxBlocks) {
        updatePlayerConfig(shapeId, maxBlocks, "", Collections.emptyList());
    }

    public void updatePlayerConfig(String shapeId, int maxBlocks, String equation, List<String> blacklist) {
        if (!Objects.equals(shapeId, this.playerShape) || maxBlocks != this.playerMaxBlocks) {
            lastPreviewKey = null;
            lastPreviewResult = null;
        }
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

        // Propagate to both engines
        leftEngine.updateConfig(shapeId, maxBlocks, equation, blacklist);
        rightEngine.updateConfig(shapeId, maxBlocks, equation, blacklist);
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * @return true if EITHER engine is actively processing
     */
    public boolean isWorking() {
        return leftEngine.isActive() || rightEngine.isActive();
    }

    public LeftClickEngine left() {
        return leftEngine;
    }

    public RightClickEngine right() {
        return rightEngine;
    }

    public PreviewManager preview() {
        return previewManager;
    }

    public EngineState getState() {
        // Return left engine state for backward compat
        return leftEngine.getState();
    }

    @Override
    public String getCurrentShape() {
        return playerShape;
    }

    @Override
    public int getMaxBlocks() {
        return playerMaxBlocks;
    }

    @Override
    public int getProcessedCount() {
        if (leftEngine.isActive()) {
            return leftEngine.getSessionProcessedCount();
        } else if (rightEngine.isActive()) {
            return rightEngine.getSessionProcessedCount();
        }
        return 0;
    }

    @Override
    public int getTargetCount() {
        if (leftEngine.isActive()) {
            return leftEngine.getSessionTargetCount();
        } else if (rightEngine.isActive()) {
            return rightEngine.getSessionTargetCount();
        }
        return 0;
    }

    @Override
    public ActionType getCurrentActionType() {
        if (leftEngine.isActive()) {
            return leftEngine.getSessionActionType();
        } else if (rightEngine.isActive()) {
            return rightEngine.getSessionActionType();
        }
        return null;
    }

    public Set<BlockPos> getRenderSnapshot() {
        // Merge both for backward compat
        Set<BlockPos> merged = new HashSet<>(leftEngine.getRenderSnapshot());
        merged.addAll(rightEngine.getRenderSnapshot());
        return merged;
    }

    // ── Utilities ─────────────────────────────────────────────────────────

    private static String blockId(BlockState state) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    private static Set<String> mergeBlacklists(Set<String> configBlacklist, Set<String> playerBlacklist) {
        Set<String> result = new HashSet<>(FilterModeManager.normalizeBlacklist(configBlacklist));
        result.addAll(playerBlacklist);
        return result;
    }

    private static Direction approximateHitFace(Player player) {
        float pitch = player.getXRot();
        if (pitch > 60f)
            return Direction.UP;
        if (pitch < -60f)
            return Direction.DOWN;
        return OrientationContext.facingFromYaw(player.getYRot()).getOpposite();
    }
}