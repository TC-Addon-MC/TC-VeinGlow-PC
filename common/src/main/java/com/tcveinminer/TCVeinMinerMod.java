package com.tcveinminer;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TCVeinMinerMod {

    public static final Set<UUID> playersHoldingV =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

}
