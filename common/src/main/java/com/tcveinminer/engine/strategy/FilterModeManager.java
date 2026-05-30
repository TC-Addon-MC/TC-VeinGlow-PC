package com.tcveinminer.engine.strategy;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LiquidBlock;
import com.tcveinminer.config.ConfigManager;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Trung tâm quản lý Filter System cho VeinMiner, TreeCapitator, TunnelMiner.
 * Thiết kế Single-File Production Quality.
 */
public final class FilterModeManager {

    private FilterModeManager() {
    }

    private static final int DEFAULT_SEARCH_LIMIT = 100;
    private static volatile List<ResourceLocation> blockSearchCache = new ArrayList<>();
    private static volatile String lastBlockSearchQuery = "";

    // ==========================================
    // 1. CORE API & CONTEXT
    // ==========================================

    @FunctionalInterface
    public interface BlockFilter {
        boolean test(FilterContext ctx);
    }

    /**
     * Ngữ cảnh tĩnh và động cho mỗi lần duyệt block.
     * Record đảm bảo tính bất biến, an toàn trong môi trường đa luồng nếu cần.
     */
    public record FilterContext(
            Level Level,
            Player player,
            ItemStack tool,
            BlockPos originPos,
            BlockPos currentPos,
            BlockState targetState,
            BlockState currentState,
            Direction approachDirection, // Hướng từ block trước đó đi tới block này
            int depth, // Độ sâu đệ quy / BFS
            double distance, // Khoảng cách hình học tính từ origin
            int visitedCount, // Tổng số block đã duyệt qua
            MiningMode mode, // Chế độ đang chạy
            FilterCache cache, // Bộ nhớ đệm dùng chung cho một phiên đào
            Set<String> blacklist, // Danh sách đen cá nhân
            boolean requireHarvestCapability // Yêu cầu dụng cụ đúng
    ) {
    }

    public enum MiningMode {
        VEIN, TUNNEL, SHAPE, TREE_CAPITATOR, EXCAVATE
    }

    public static BlockFilter resolveFilter(MiningMode mode, int maxBlocks) {
        int safeMaxBlocks = Math.max(0, maxBlocks);
        return switch (mode) {
            case TREE_CAPITATOR -> Presets.TREE_CAPITATOR(safeMaxBlocks);
            case TUNNEL, SHAPE -> Presets.SAME_BLOCK_SHAPE(safeMaxBlocks);
            default -> Presets.VEIN_ORE(safeMaxBlocks);
        };
    }

    public static Set<String> normalizeBlacklist(Set<String> input) {
        if (input == null || input.isEmpty())
            return Collections.emptySet();

        Set<String> normalized = new HashSet<>();
        for (String raw : input) {
            ResourceLocation id = validateAndParseBlock(raw);
            if (id != null) {
                normalized.add(id.toString());
            }
        }
        return normalized;
    }

    public static Set<String> normalizeIdentifierBlacklist(Set<ResourceLocation> input) {
        if (input == null || input.isEmpty())
            return Collections.emptySet();

        Set<String> normalized = new LinkedHashSet<>();
        for (ResourceLocation id : input) {
            if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) {
                normalized.add(id.toString());
            }
        }
        return normalized;
    }

    public static void updateBlockSearch(String query) {
        searchBlocks(query, DEFAULT_SEARCH_LIMIT);
    }

    public static List<ResourceLocation> getBlockSearchCache() {
        return blockSearchCache;
    }

    public static synchronized List<ResourceLocation> searchBlocks(String query, int limit) {
        String lowerQuery = query == null ? "" : query.toLowerCase().trim();
        if (lowerQuery.equals(lastBlockSearchQuery)) {
            return blockSearchCache;
        }
        lastBlockSearchQuery = lowerQuery;

        if (lowerQuery.isEmpty()) {
            blockSearchCache = Collections.emptyList();
            return blockSearchCache;
        }

        int safeLimit = Math.max(1, limit);
        blockSearchCache = BuiltInRegistries.BLOCK.keySet().stream()
                .filter(id -> id.toString().contains(lowerQuery) || id.getPath().contains(lowerQuery))
                .limit(safeLimit)
                .collect(Collectors.toList());
        return blockSearchCache;
    }

    public static ResourceLocation validateAndParseBlock(String input) {
        if (input == null)
            return null;

        String value = input.trim().toLowerCase();
        if (value.isEmpty())
            return null;
        if (!value.contains(":")) {
            value = "minecraft:" + value;
        }

        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id))
            return null;

        Block block = BuiltInRegistries.BLOCK.get(id);
        return block == net.minecraft.world.level.block.Blocks.AIR ? null : id;
    }

    /**
     * Bộ nhớ đệm cục bộ cho mỗi phiên đào để tránh tính toán lại các logic nặng.
     */
    public static final class FilterCache {
        private final Map<Block, Float> hardnessCache = new HashMap<>();
        private final Map<BlockPos, Boolean> blockEntityCache = new HashMap<>();
        public final boolean preventMiningNearFluids = ConfigManager.get().preventMiningNearFluids;

        public float getHardness(BlockState state) {
            return hardnessCache.computeIfAbsent(state.getBlock(), b -> state.getDestroySpeed(null, null));
        }

        public boolean hasBlockEntity(Level Level, BlockPos pos, BlockState state) {
            if (state.hasBlockEntity()) {
                return blockEntityCache.computeIfAbsent(pos, p -> Level.getBlockEntity(p) != null);
            }
            return false;
        }
    }

    // ==========================================
    // 2. COMPOSITE SYSTEM (Lõi kết hợp)
    // ==========================================

    public static final class Composite {
        /**
         * Chạy tuần tự, short-circuit ngay khi gặp false.
         * Sử dụng mảng để tránh iterator allocation (rác bộ nhớ).
         */
        public static BlockFilter and(BlockFilter... filters) {
            return ctx -> {
                for (BlockFilter filter : filters) {
                    if (!filter.test(ctx))
                        return false;
                }
                return true;
            };
        }

        /**
         * Chạy tuần tự, short-circuit ngay khi gặp true.
         */
        public static BlockFilter or(BlockFilter... filters) {
            return ctx -> {
                for (BlockFilter filter : filters) {
                    if (filter.test(ctx))
                        return true;
                }
                return false;
            };
        }

        public static BlockFilter not(BlockFilter filter) {
            return ctx -> !filter.test(ctx);
        }
    }

    // ==========================================
    // 3. FILTER IMPLEMENTATIONS
    // ==========================================

    public static final class Filters {

        public static BlockFilter blacklist(Set<String> blocked) {
            return ctx -> {
                if (blocked == null || blocked.isEmpty())
                    return true;
                String id = BuiltInRegistries.BLOCK.getKey(ctx.currentState().getBlock()).toString();
                return !blocked.contains(id);
            };
        }

        // --- SINGLETONS CƠ BẢN (Không cấp phát rác) ---
        public static final BlockFilter NOT_AIR = ctx -> !ctx.currentState().isAir();
        public static final BlockFilter SOLID_ONLY = ctx -> ctx.currentState().isRedstoneConductor(ctx.Level(),
                ctx.currentPos());
        public static final BlockFilter BREAKABLE_ONLY = ctx -> ctx.cache().getHardness(ctx.currentState()) >= 0;
        public static final BlockFilter ORES_ONLY = ctx -> {
            BlockState s = ctx.currentState();
            return s.is(BlockTags.COAL_ORES) || s.is(BlockTags.IRON_ORES) || s.is(BlockTags.GOLD_ORES)
                    || s.is(BlockTags.DIAMOND_ORES) || s.is(BlockTags.REDSTONE_ORES) || s.is(BlockTags.LAPIS_ORES)
                    || s.is(BlockTags.EMERALD_ORES) || s.is(BlockTags.COPPER_ORES);
        };
        public static final BlockFilter LOGS_ONLY = ctx -> ctx.currentState().is(BlockTags.LOGS);
        public static final BlockFilter LEAVES_ONLY = ctx -> ctx.currentState().is(BlockTags.LEAVES);

        // --- 3.1 BASIC FILTERS ---
        public static BlockFilter sameBlock() {
            return ctx -> {
                if (ctx.currentState().getBlock() != ctx.targetState().getBlock())
                    return false;
                Block b = ctx.currentState().getBlock();
                if (b instanceof net.minecraft.world.level.block.CropBlock crop) {
                    return crop.isMaxAge(ctx.currentState()) == crop.isMaxAge(ctx.targetState());
                } else if (b instanceof net.minecraft.world.level.block.NetherWartBlock) {
                    return ctx.currentState()
                            .getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_3)
                            .equals(
                                    ctx.targetState().getValue(
                                            net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_3));
                } else if (b instanceof net.minecraft.world.level.block.CocoaBlock) {
                    return ctx.currentState()
                            .getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_2)
                            .equals(
                                    ctx.targetState().getValue(
                                            net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_2));
                }
                return true;
            };
        }

        public static BlockFilter sameState() {
            return ctx -> ctx.currentState().equals(ctx.targetState());
        }

        public static BlockFilter tag(TagKey<Block> tag) {
            return ctx -> ctx.currentState().is(tag);
        }

        public static BlockFilter hardnessBelow(float max) {
            return ctx -> ctx.cache().getHardness(ctx.currentState()) <= max;
        }

        public static BlockFilter hardnessDelta(float tolerance) {
            return ctx -> {
                float targetH = ctx.cache().getHardness(ctx.targetState());
                float currentH = ctx.cache().getHardness(ctx.currentState());
                return Math.abs(targetH - currentH) <= tolerance;
            };
        }

        // --- 3.2 TOOL FILTERS ---
        public static BlockFilter harvestableByTool() {
            return ctx -> ctx.player().hasCorrectToolForDrops(ctx.currentState());
        }

        public static BlockFilter durabilitySafe(int minRemaining) {
            return ctx -> {
                ItemStack tool = ctx.tool();
                if (!tool.isDamageableItem())
                    return true;
                return (tool.getMaxDamage() - tool.getDamageValue()) > minRemaining;
            };
        }

        // --- 3.3 SAFETY FILTERS ---
        public static BlockFilter avoidTileEntity() {
            return ctx -> !ctx.cache().hasBlockEntity(ctx.Level(), ctx.currentPos(), ctx.currentState());
        }

        public static BlockFilter avoidFallingBlocks() {
            return ctx -> !(ctx.currentState().getBlock() instanceof FallingBlock);
        }

        public static BlockFilter avoidLiquids() {
            return ctx -> ctx.currentState().getFluidState().isEmpty();
        }

        public static BlockFilter avoidAdjacentLiquids() {
            return ctx -> {
                if (!ctx.cache().preventMiningNearFluids) {
                    return true;
                }
                for (Direction dir : Direction.values()) {
                    BlockPos adjPos = ctx.currentPos().relative(dir);
                    if (dir.getAxis() != Direction.Axis.Y
                            && !ctx.Level().hasChunk(adjPos.getX() >> 4, adjPos.getZ() >> 4)) {
                        return false;
                    }
                    BlockState adjState = ctx.Level().getBlockState(adjPos);
                    if (adjState.getBlock() instanceof LiquidBlock || !adjState.getFluidState().isEmpty()) {
                        return false;
                    }
                }
                return true;
            };
        }

        // --- 3.4 SPATIAL & PERFORMANCE FILTERS ---
        public static BlockFilter maxDepth(int max) {
            return ctx -> ctx.depth() <= max;
        }

        public static BlockFilter maxDistance(int maxRadius) {
            return ctx -> ctx.distance() <= maxRadius;
        }

        public static BlockFilter chunkLoadedOnly() {
            return ctx -> ctx.Level().hasChunk(ctx.currentPos().getX() >> 4, ctx.currentPos().getZ() >> 4);
        }

        public static BlockFilter maxVisited(int maxLimit) {
            return ctx -> ctx.visitedCount() <= maxLimit;
        }

        // --- 3.5 TREE FILTERS ---
        public static BlockFilter naturalTreeOnly() {
            return ctx -> {
                // Heuristic: Cây tự nhiên thường có lá ở trên hoặc gần đó.
                // Hàm này nên kết hợp với logic tìm lá theo chiều dọc.
                // Tránh phức tạp hóa filter, ta giới hạn chiều cao Y phải lớn hơn hoặc bằng rễ.
                return ctx.currentPos().getY() >= ctx.originPos().getY();
            };
        }

        // --- 3.6 SMART FILTERS ---
        public static BlockFilter whitelist(Set<Block> allowed) {
            return ctx -> allowed.contains(ctx.currentState().getBlock());
        }

        public static <T extends Comparable<T>> BlockFilter stateProperty(Property<T> property, T value) {
            return ctx -> ctx.currentState().hasProperty(property)
                    && ctx.currentState().getValue(property).equals(value);
        }

        // --- 3.7 ADVANCED SERVER-SIDE FILTERS ---
        public static BlockFilter survivalModeOnly() {
            return ctx -> {
                if (ctx.player() instanceof ServerPlayer spe) {
                    return !spe.isCreative();
                }
                return true; // Assume survival if not server player
            };
        }

        public static BlockFilter bedrock() {
            return ctx -> ctx.currentState().getBlock() != net.minecraft.world.level.block.Blocks.BEDROCK;
        }

        public static BlockFilter withinWorldBorder() {
            return ctx -> {
                WorldBorder border = ctx.Level().getWorldBorder();
                return border.isWithinBounds(ctx.currentPos());
            };
        }

        public static BlockFilter allowedByExplosionResistance(float minResistance) {
            return ctx -> {
                float resistance = ctx.currentState().getBlock().getExplosionResistance();
                return resistance >= minResistance;
            };
        }

        public static BlockFilter notLockedByPiston() {
            return ctx -> {
                // Kiểm tra xem block có bị piston khóa không
                BlockState state = ctx.currentState();
                return state.getPistonPushReaction() != PushReaction.BLOCK;
            };
        }

        private static final float HAND_MINING_SPEED = 1.0F;

        public static BlockFilter canHarvestBlock() {
            return ctx -> {
                BlockState state = ctx.currentState();
                Player player = ctx.player();
                ItemStack tool = ctx.tool();

                // Nếu không cần validate tool
                if (!ctx.requireHarvestCapability()) {
                    return true;
                }

                // Tay không
                if (tool.isEmpty()) {
                    return !state.requiresCorrectToolForDrops();
                }

                float speed = tool.getDestroySpeed(state);

                // Block yêu cầu tool thật sự
                if (state.requiresCorrectToolForDrops()) {
                    return player.hasCorrectToolForDrops(state)
                            && (speed > HAND_MINING_SPEED || tool.isCorrectToolForDrops(state));
                }

                // Block không yêu cầu tool
                return speed > HAND_MINING_SPEED
                        || tool.isCorrectToolForDrops(state)
                        || player.hasCorrectToolForDrops(state);
            };
        }

        public static BlockFilter allowEnchantments() {
            return ctx -> {
                ItemStack tool = ctx.tool();
                if (tool.isEmpty())
                    return false;

                return true;
            };
        }

        public static BlockFilter chunkLoadedForBreak() {
            return ctx -> {
                return ctx.Level().hasChunk(ctx.currentPos().getX() >> 4, ctx.currentPos().getZ() >> 4);
            };
        }
    }

    // ==========================================
    // 4. PRESET SYSTEM (Cấu hình sẵn)
    // ==========================================

    public static final class Presets {

        /**
         * Pipeline cơ sở: Chặn lỗi server, chặn đào hỏng đồ.
         * Thứ tự: Rẻ/nhanh -> Đắt/chậm.
         */
        public static final BlockFilter BASE_SAFETY = Composite.and(
                Filters.chunkLoadedOnly(),
                Filters.NOT_AIR,
                Filters.BREAKABLE_ONLY,
                Filters.avoidTileEntity(),
                ctx -> Filters.blacklist(ctx.blacklist()).test(ctx));

        /**
         * Chế độ đào quặng an toàn.
         */
        public static BlockFilter VEIN_ORE(int maxBlocks) {
            return Composite.and(
                    BASE_SAFETY,
                    Filters.maxVisited(maxBlocks),
                    Filters.sameBlock(), // Hoặc sameOreFamily nếu mở rộng
                    Filters.avoidAdjacentLiquids(), // Tránh lava chảy vào
                    Filters.canHarvestBlock(),
                    Filters.durabilitySafe(5));
        }

        public static BlockFilter SAME_BLOCK_SHAPE(int maxBlocks) {
            return Composite.and(
                    BASE_SAFETY,
                    Filters.sameBlock(),
                    Filters.maxVisited(maxBlocks),
                    Filters.canHarvestBlock());
        }

        /**
         * Chế độ múc chất lỏng (Xô không)
         */
        public static BlockFilter FLUID_SCOOP(int maxBlocks) {
            return Composite.and(
                    Filters.chunkLoadedOnly(),
                    Filters.maxVisited(maxBlocks),
                    Filters.sameBlock(),
                    ctx -> ctx.currentState().getFluidState().isSource(),
                    ctx -> Filters.blacklist(ctx.blacklist()).test(ctx));
        }

        /**
         * Chế độ chặt cây.
         */
        public static BlockFilter TREE_CAPITATOR(int maxBlocks) {
            return Composite.and(
                    BASE_SAFETY,
                    Filters.maxVisited(maxBlocks),
                    Filters.maxDistance(32),
                    Composite.or(Filters.LOGS_ONLY, Filters.LEAVES_ONLY),
                    Filters.naturalTreeOnly(),
                    Filters.canHarvestBlock());
        }

    }

    // ==========================================
    // 5. EXTENSIBILITY (Mở rộng cho Addon)
    // ==========================================

    public static final class Registry {
        private static final Map<String, Supplier<BlockFilter>> CUSTOM_FILTERS = new ConcurrentHashMap<>();

        /**
         * Đăng ký filter từ các mod khác hoặc cấu hình JSON.
         */
        public static void register(String id, Supplier<BlockFilter> filterFactory) {
            CUSTOM_FILTERS.put(id, filterFactory);
        }

        public static BlockFilter get(String id) {
            Supplier<BlockFilter> factory = CUSTOM_FILTERS.get(id);
            return factory != null ? factory.get() : ctx -> true;
        }
    }
}
