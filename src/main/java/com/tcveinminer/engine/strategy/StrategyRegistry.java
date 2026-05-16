package com.tcveinminer.engine.strategy;

import java.util.*;

/**
 * Registry tập trung cho tất cả MiningStrategy.
 * Không có if/else chain — strategies được register và lookup bằng ID.
 * Thêm mode mới = gọi register(), không cần sửa logic nào khác.
 */
public final class StrategyRegistry {

    private static final Map<String, MiningStrategy> REGISTRY = new LinkedHashMap<>();

    static {
        // Register tất cả built-in strategies theo đúng thứ tự hiển thị
        register(new FaceStrategy());
        register(new EdgeStrategy());
        register(new CornerStrategy());
        register(new TallStrategy());
        register(new StairStrategy(+1));
        register(new StairStrategy(-1));
        register(new AreaStrategy());
    }

    public static void register(MiningStrategy strategy) {
        REGISTRY.put(strategy.getId(), strategy);
    }

    /** Lấy strategy theo ID. Nếu không tìm thấy, trả về FaceStrategy làm fallback an toàn. */
    public static MiningStrategy get(String id) {
        return REGISTRY.getOrDefault(id, REGISTRY.get(FaceStrategy.ID));
    }

    /** Tất cả strategy đã register, theo thứ tự đăng ký. */
    public static Collection<MiningStrategy> all() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    public static boolean contains(String id) {
        return REGISTRY.containsKey(id);
    }

    private StrategyRegistry() {}
}
