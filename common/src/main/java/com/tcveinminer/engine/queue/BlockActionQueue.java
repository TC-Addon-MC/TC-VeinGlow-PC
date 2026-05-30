package com.tcveinminer.engine.queue;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import java.util.*;

/**
 * Action queue with deduplication, tick slicing, and interrupt support.
 * <p>
 * Renamed from MiningQueue — this queue serves both left-click (mining)
 * and right-click (interact/harvest/plant) engines.
 * <p>
 * FIXED: chunk loaded check happens BEFORE getBlockState() in both
 *        enqueue() and drainForTick() to prevent forced chunk loading (TPS killer).
 */
public final class BlockActionQueue {

    public record ActionEntry(BlockPos pos, BlockState expectedState) {}

    private final Deque<ActionEntry> queue  = new ArrayDeque<>();
    private final Set<BlockPos> inQueue = new HashSet<>();
    private boolean interrupted = false;

    public void enqueue(List<BlockPos> positions, ServerLevel world) {
        for (BlockPos pos : positions) {
            if (!inQueue.contains(pos)) {
                if (!world.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) continue;
                inQueue.add(pos);
                queue.add(new ActionEntry(pos, world.getBlockState(pos)));
            }
        }
    }

    /**
     * FIXED: isChunkLoaded() checked FIRST, then getBlockState().
     * Old code called getBlockState() before isChunkLoaded(), which force-loaded chunks.
     *
     * FIXED: pollLimit is snapshotted before the loop so stale/unloaded entries at
     * the front of the queue don't cause early exit.
     */
    public List<ActionEntry> drainForTick(ServerLevel world, int maxPerTick) {
        List<ActionEntry> batch = new ArrayList<>();
        int pollLimit = queue.size();
        int polled = 0;

        while (!queue.isEmpty() && batch.size() < maxPerTick && polled < pollLimit) {
            ActionEntry e = queue.poll();
            inQueue.remove(e.pos());
            polled++;

            // 1. Chunk check FIRST — never force-load
            if (!world.hasChunk(e.pos().getX() >> 4, e.pos().getZ() >> 4)) continue;

            // 2. Block state check after chunk is confirmed loaded
            BlockState current = world.getBlockState(e.pos());
            if (current.getBlock() != e.expectedState().getBlock()) continue;

            batch.add(e);
        }
        return batch;
    }

    public void interrupt() {
        interrupted = true;
        queue.clear();
        inQueue.clear();
    }

    public void reset() {
        interrupted = false;
        queue.clear();
        inQueue.clear();
    }

    public boolean isEmpty()       { return queue.isEmpty(); }
    public boolean isInterrupted() { return interrupted; }
    public int getContainerSize()              { return queue.size(); }

    public Set<BlockPos> snapshot() {
        return Collections.unmodifiableSet(new HashSet<>(inQueue));
    }
}
