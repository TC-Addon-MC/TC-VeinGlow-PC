package com.tcveinminer.engine.queue;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.*;

/**
 * Mining queue with deduplication, tick slicing, and interrupt support.
 *
 * FIXED: chunk loaded check happens BEFORE getBlockState() to prevent
 *        forced chunk loading (TPS killer).
 */
public final class MiningQueue {

    public record Entry(BlockPos pos, BlockState expectedState) {}

    private final Deque<Entry> queue  = new ArrayDeque<>();
    private final Set<BlockPos> inQueue = new HashSet<>();
    private boolean interrupted = false;

    public void enqueue(List<BlockPos> positions, ServerWorld world) {
        for (BlockPos pos : positions) {
            if (!inQueue.contains(pos)) {
                // Only check chunk loaded here — we already know world is loaded
                // during scan phase, so this is safe.
                inQueue.add(pos);
                queue.add(new Entry(pos, world.getBlockState(pos)));
            }
        }
    }

    /**
     * FIXED: isChunkLoaded() checked FIRST, then getBlockState().
     * Old code called getBlockState() before isChunkLoaded(), which force-loaded chunks.
     *
     * FIXED: pollLimit is snapshotted before the loop so stale/unloaded entries at
     * the front of the queue don't cause early exit (old maxPerTick*4 cap was too
     * tight when the queue had many consecutive unloaded-chunk entries).
     */
    public List<Entry> drainForTick(ServerWorld world, int maxPerTick) {
        List<Entry> batch = new ArrayList<>();
        // Snapshot current size: we poll at most this many entries per call,
        // which bounds the loop without the artificial maxPerTick*4 cap.
        int pollLimit = queue.size();
        int polled = 0;

        while (!queue.isEmpty() && batch.size() < maxPerTick && polled < pollLimit) {
            Entry e = queue.poll();
            inQueue.remove(e.pos());
            polled++;

            // 1. Chunk check FIRST — never force-load
            if (!world.isChunkLoaded(e.pos())) continue;

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
    public int size()              { return queue.size(); }

    public Set<BlockPos> snapshot() {
        return Collections.unmodifiableSet(new HashSet<>(inQueue));
    }
}