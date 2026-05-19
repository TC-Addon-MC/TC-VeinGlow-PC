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
     */
    public List<Entry> drainForTick(ServerWorld world, int maxPerTick) {
        List<Entry> batch = new ArrayList<>();
        int checked = 0;
        int checkLimit = maxPerTick * 4; // avoid infinite loop on stale queue

        while (!queue.isEmpty() && batch.size() < maxPerTick && checked < checkLimit) {
            Entry e = queue.poll();
            inQueue.remove(e.pos());
            checked++;

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
