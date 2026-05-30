package com.tcveinminer.api.query;

import com.tcveinminer.engine.action.ActionType;
import com.tcveinminer.engine.state.EngineState;
import net.minecraft.core.BlockPos;

import java.util.Set;

/**
 * A read-only query interface for addon developers to inspect
 * the current state of a player's mining engine.
 */
public interface EngineQuery {
    /** @return true if the engine is actively processing blocks. */
    boolean isWorking();

    /** @return the current engine state. */
    EngineState getState();

    /** @return the current shape mode (e.g. "FACE", "TUNNEL_3x3"). */
    String getCurrentShape();

    /** @return the current max blocks limit. */
    int getMaxBlocks();

    /** @return the list of blocks currently highlighted/in-preview. */
    Set<BlockPos> getRenderSnapshot();

    /** @return the number of blocks processed so far in the current session. */
    int getProcessedCount();

    /** @return the total number of blocks targeted in the current session. */
    int getTargetCount();
    
    /** @return the action type being executed, or null if idle. */
    ActionType getCurrentActionType();
}
