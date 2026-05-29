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
import com.tcveinminer.network.ActivationConfirmPayload;
import com.tcveinminer.network.FilterResultPayload;
import com.tcveinminer.network.HighlightBlockListPayload;
import com.tcveinminer.network.HighlightDeltaPayload;
import com.tcveinminer.network.LookedAtBlockPayload;
import com.tcveinminer.util.ExpressionEvaluator;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MiningEngine: per-player thin orchestrator.
 * <p>
 * Delegates all work to independent {@link LeftClickEngine} and {@link RightClickEngine}.
 * Both engines run in parallel — no shared mutable state between them.
 * <p>
 * This class manages:
 * <ul>
 *   <li>Player engine lifecycle (create, remove, config propagation)</li>
 *   <li>Preview/highlight computation (activation requests)</li>
 *   <li>Event delegation (break → left, interact → right, tick → both)</li>
 * </ul>
 */
public final class MiningEngine implements EngineQuery {

    private static final Map<UUID, MiningEngine> ENGINES = new ConcurrentHashMap<>();

    // ── Per-player engines ────────────────────────────────────────────────

    private final LeftClickEngine leftEngine = new LeftClickEngine();
    private final RightClickEngine rightEngine = new RightClickEngine();
    private final PreviewManager previewManager = new PreviewManager();

    // ── Preview cache ─────────────────────────────────────────────────────
    private record PreviewCacheKey(BlockPos pos, String shape, int maxBlocks, net.minecraft.item.Item heldItem) {}
    private PreviewCacheKey lastPreviewKey = null;
    private List<BlockPos> lastPreviewResult = null;

    // ── Preview config (shared for activation requests) ───────────────────
    private String playerShape = "FACE";
    private int playerMaxBlocks = 64;
    private MiningStrategy customStrategy = null;
    private Set<String> playerBlacklist = new HashSet<>();
    private Set<String> mergedBlacklist = Collections.emptySet();

    private MiningEngine() {}

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
    public void onBreakTrigger(PlayerEntity player, ServerWorld world,
                                BlockPos origin, BlockState originState) {
        leftEngine.onBreakTrigger(player, world, origin, originState);
    }

    /**
     * Right click — interact/harvest/plant trigger.
     *
     * @return true if the mod handled the interaction
     */
    public boolean onInteractTrigger(PlayerEntity player, ServerWorld world,
                                      Hand hand, BlockHitResult hitResult) {
        return rightEngine.onInteractTrigger(player, world, hand, hitResult);
    }

    /**
     * Server tick — ticks BOTH engines independently (parallel).
     */
    public void onServerTick(PlayerEntity player, ServerWorld world) {
        leftEngine.onServerTick(player, world);
        rightEngine.onServerTick(player, world);
    }

    // ── Preview / Activation Request ──────────────────────────────────────

    /**
     * Handle activation request from client (held V + looking at block).
     * Computes preview highlight for the left-click engine.
     */
    public void handleActivationRequest(ServerPlayerEntity spe, boolean active, BlockPos targetPos) {
        // Ignore dynamic updates if already processing
        if (leftEngine.getState() == EngineState.PROCESSING
            || leftEngine.getState() == EngineState.FINISHED
            || leftEngine.getState() == EngineState.CANCELLED) {
            return;
        }

        if (!active || targetPos == null) {
            if (leftEngine.getState() == EngineState.PREVIEW) {
                leftEngine.stateMachine.force(EngineState.IDLE);
            }
            ServerPlayNetworking.send(spe, new ActivationConfirmPayload(false));
            ServerPlayNetworking.send(spe, new FilterResultPayload(false));
            
            ActionSession session = ActionSessionManager.getOrCreate(spe.getUuid());
            Set<BlockPos> prev = session.getRenderSnapshot();
            if (!prev.isEmpty()) {
                ServerPlayNetworking.send(spe, new HighlightDeltaPayload(Collections.emptyList(), new ArrayList<>(prev), "FACE", "LEFT"));
                session.initSnapshot(Collections.emptySet());
            }
            return;
        }

        ModConfig c = ConfigManager.get();
        if (!c.enabled) return;

        leftEngine.stateMachine.force(EngineState.PREVIEW);

        ServerWorld world = spe.getServerWorld();
        if (!world.isChunkLoaded(targetPos.getX() >> 4, targetPos.getZ() >> 4)) return;

        BlockState targetState = world.getBlockState(targetPos);
        if (targetState.isAir()) {
            ServerPlayNetworking.send(spe, new FilterResultPayload(false));
            return;
        }

        if (spe.getMainHandStack().getItem() == Items.BUCKET 
            && !(targetState.getBlock() instanceof FluidBlock && targetState.getFluidState().isStill())) {
            ServerPlayNetworking.send(spe, new ActivationConfirmPayload(false));
            ServerPlayNetworking.send(spe, new FilterResultPayload(false));
            ActionSession session = ActionSessionManager.getOrCreate(spe.getUuid());
            Set<BlockPos> prev = session.getRenderSnapshot();
            if (!prev.isEmpty()) {
                ServerPlayNetworking.send(spe, new HighlightDeltaPayload(
                    Collections.emptyList(), new ArrayList<>(prev), "FACE", "LEFT"));
                session.initSnapshot(Collections.emptySet());
            }
            return;
        }

        Set<String> activeBlacklist = mergedBlacklist;
        boolean isBlacklisted = activeBlacklist.contains(blockId(targetState));

        if (isBlacklisted) {
            ServerPlayNetworking.send(spe, new ActivationConfirmPayload(false));
            ServerPlayNetworking.send(spe, new LookedAtBlockPayload(targetPos));
            ServerPlayNetworking.send(spe, new FilterResultPayload(false));
            return;
        }

        Direction hitFace = approximateHitFace(spe);
        OrientationContext ctx = OrientationContext.of(hitFace, OrientationContext.facingFromYaw(spe.getYaw()));

        MiningStrategy strategy;
        int maxBlocksToMine = this.playerMaxBlocks - 1;
        FilterModeManager.BlockFilter filter;

        ActionType rightAction = CapabilityRegistry.resolve(spe, spe.getMainHandStack(), targetState, Hand.MAIN_HAND);
        
        if (rightAction != ActionType.VANILLA_FALLBACK && rightAction != ActionType.USE_ITEM) {
            strategy = (customStrategy != null) ? customStrategy : StrategyRegistry.get(this.playerShape);
            
            if (rightAction == ActionType.FLUID_SCOOP) {
                int availableBuckets = 0;
                for (int i = 0; i < spe.getInventory().size(); i++) {
                    ItemStack stack = spe.getInventory().getStack(i);
                    if (!stack.isEmpty() && stack.getItem() == Items.BUCKET) {
                        availableBuckets += stack.getCount();
                    }
                }
                int bucketMax = Math.max(0, Math.min(this.playerMaxBlocks, availableBuckets) - 1);
                if (bucketMax < maxBlocksToMine) maxBlocksToMine = bucketMax;
                strategy = StrategyRegistry.get("FACE");
            }
            
            filter = RightClickFilterPipeline.forAction(rightAction, maxBlocksToMine);
        } else {
            strategy = (customStrategy != null) ? customStrategy : StrategyRegistry.get(this.playerShape);
            ActionType leftAction = ("TREE_CAP".equals(this.playerShape) && targetState.isIn(net.minecraft.registry.tag.BlockTags.LOGS)) 
                    ? ActionType.TREE_CAP : ActionType.BREAK;
                    
            filter = LeftClickFilterPipeline.forAction(leftAction, strategy.getModeType(), maxBlocksToMine);
        }

        FilterModeManager.FilterCache cache = new FilterModeManager.FilterCache();

        FilterModeManager.FilterContext fCtxTarget = new FilterModeManager.FilterContext(
                world, spe, spe.getMainHandStack(), targetPos, targetPos,
                targetState, targetState, Direction.UP, 0, 0, 0,
                strategy.getModeType(), cache, activeBlacklist, c.requireHarvestCapability);

        if (!filter.test(fCtxTarget)) {
            ServerPlayNetworking.send(spe, new ActivationConfirmPayload(false));
            ServerPlayNetworking.send(spe, new LookedAtBlockPayload(targetPos));
            ServerPlayNetworking.send(spe, new FilterResultPayload(false));
            return;
        }

        ServerPlayNetworking.send(spe, new ActivationConfirmPayload(true));
        ServerPlayNetworking.send(spe, new LookedAtBlockPayload(targetPos));
        ServerPlayNetworking.send(spe, new FilterResultPayload(true));

        PreviewCacheKey key = new PreviewCacheKey(
                targetPos, this.playerShape, this.playerMaxBlocks,
                spe.getMainHandStack().getItem()
        );

        List<BlockPos> found;
        if (key.equals(lastPreviewKey) && lastPreviewResult != null) {
            found = lastPreviewResult;
        } else {
            MiningStrategy.MiningRequest req = new MiningStrategy.MiningRequest(
                    world, spe, spe.getMainHandStack(),
                    targetPos, targetState, maxBlocksToMine, ctx, filter, cache, activeBlacklist,
                    c.requireHarvestCapability, EngineState.PREVIEW, spe.getMainHandStack().getItem(), c.allowHeldItemChange);
            found = strategy.collectBlocks(req);
            lastPreviewKey = key;
            lastPreviewResult = found;
        }
        if (!found.contains(targetPos)) {
            found.add(targetPos);
        }

        ActionSession session = ActionSessionManager.getOrCreate(spe.getUuid());
        Set<BlockPos> previous = session.getRenderSnapshot();
        Set<BlockPos> current = new HashSet<>(found);

        List<BlockPos> added = new ArrayList<>();
        for (BlockPos p : current) {
            if (!previous.contains(p)) added.add(p);
        }

        List<BlockPos> removed = new ArrayList<>();
        for (BlockPos p : previous) {
            if (!current.contains(p)) removed.add(p);
        }

        if (!added.isEmpty() || !removed.isEmpty()) {
            ServerPlayNetworking.send(spe, new HighlightDeltaPayload(added, removed, this.playerShape, "LEFT"));
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

    public LeftClickEngine left() { return leftEngine; }
    public RightClickEngine right() { return rightEngine; }
    public PreviewManager preview() { return previewManager; }

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
            return leftEngine.session != null ? leftEngine.session.getProcessedCount() : 0;
        } else if (rightEngine.isActive()) {
            return rightEngine.session != null ? rightEngine.session.getProcessedCount() : 0;
        }
        return 0;
    }

    @Override
    public int getTargetCount() {
        if (leftEngine.isActive()) {
            return leftEngine.session != null ? leftEngine.session.getTargetCount() : 0;
        } else if (rightEngine.isActive()) {
            return rightEngine.session != null ? rightEngine.session.getTargetCount() : 0;
        }
        return 0;
    }

    @Override
    public ActionType getCurrentActionType() {
        if (leftEngine.isActive()) {
            return leftEngine.session != null ? leftEngine.session.getActionType() : null;
        } else if (rightEngine.isActive()) {
            return rightEngine.session != null ? rightEngine.session.getActionType() : null;
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
        return net.minecraft.registry.Registries.BLOCK.getId(state.getBlock()).toString();
    }

    private static Set<String> mergeBlacklists(Set<String> configBlacklist, Set<String> playerBlacklist) {
        Set<String> result = new HashSet<>(FilterModeManager.normalizeBlacklist(configBlacklist));
        result.addAll(playerBlacklist);
        return result;
    }

    private static Direction approximateHitFace(PlayerEntity player) {
        float pitch = player.getPitch();
        if (pitch > 60f) return Direction.UP;
        if (pitch < -60f) return Direction.DOWN;
        return OrientationContext.facingFromYaw(player.getYaw()).getOpposite();
    }
}
