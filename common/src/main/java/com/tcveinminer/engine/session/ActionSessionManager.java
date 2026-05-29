package com.tcveinminer.engine.session;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ActionSessionManager {
    private static final Map<UUID, ActionSession> sessions = new ConcurrentHashMap<>();

    public static ActionSession getOrCreate(UUID playerId) {
        return sessions.computeIfAbsent(playerId, id -> new ActionSession());
    }

    public static ActionSession get(UUID playerId) {
        return sessions.get(playerId);
    }

    public static boolean hasSession(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    public static void remove(UUID playerId) {
        ActionSession session = sessions.remove(playerId);
        if (session != null) {
            session.clear();
        }
    }

    public static void checkTimeouts(long currentTime, long timeoutMs) {
        sessions.entrySet().removeIf(entry -> {
            ActionSession session = entry.getValue();
            if (currentTime - session.getLastUpdateTime() > timeoutMs) {
                session.clear();
                return true;
            }
            return false;
        });
    }
}
