package com.tcveinminer.engine.strategy;

import java.util.*;

public final class StrategyRegistry {

    private static final Map<String, MiningStrategy> REGISTRY = new LinkedHashMap<>();

    static {
        register(new FaceStrategy());
        register(new EdgeStrategy());
        register(new CornerStrategy());
        register(new TallStrategy());
        register(new TunnelStrategy());
        register(new StairStrategy(+1));
        register(new StairStrategy(-1));
        register(new AreaStrategy());
        register(new Area5x5Strategy());
        register(new TreeCapitatorStrategy());
    }

    public static void register(MiningStrategy s) {
        REGISTRY.put(s.getId(), s);
    }

    public static MiningStrategy get(String id) {
        return REGISTRY.getOrDefault(id, REGISTRY.get(FaceStrategy.ID));
    }

    public static Collection<MiningStrategy> all() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    public static boolean contains(String id) {
        return REGISTRY.containsKey(id);
    }

    private StrategyRegistry() {}
}
