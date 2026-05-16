package com.tcveinminer.engine.queue;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.*;

/**
 * Mining queue với:
 *   - Deduplication (không thêm block đã có trong queue)
 *   - Validation trước khi execute (block còn tồn tại không?)
 *   - Tick slicing (không break tất cả trong 1 tick)
 *   - Interrupt support (dừng giữa chừng an toàn)
 *
 * Source of truth: cả Executor lẫn Renderer đọc từ đây.
 * Renderer KHÔNG được scan riêng.
 */
public final class MiningQueue {

    /** Block trong queue kèm block state tại thời điểm scan */
    public record Entry(BlockPos pos, BlockState expectedState) {}

    private final Deque<Entry> queue = new ArrayDeque<>();
    private final Set<BlockPos>  inQueue = new HashSet<>();   // dedup set
    private boolean interrupted = false;

    /** Thêm danh sách block vào queue. Tự dedup — bỏ qua block đã có. */
    public void enqueue(List<BlockPos> positions, ServerWorld world) {
        for (BlockPos pos : positions) {
            if (inQueue.add(pos)) {
                queue.add(new Entry(pos, world.getBlockState(pos)));
            }
        }
    }

    /**
     * Lấy tối đa {@code maxPerTick} entry để execute trong tick này.
     * Entry bị lấy ra khỏi queue.
     * Entry có block không còn tồn tại (bị break bởi người khác, v.v.) sẽ bị skip.
     */
    public List<Entry> drainForTick(ServerWorld world, int maxPerTick) {
        List<Entry> batch = new ArrayList<>();
        int checked = 0;

        while (!queue.isEmpty() && batch.size() < maxPerTick && checked < maxPerTick * 3) {
            Entry e = queue.poll();
            inQueue.remove(e.pos());
            checked++;

            // Validate: block có còn match expected state không?
            BlockState current = world.getBlockState(e.pos());
            if (current.getBlock() != e.expectedState().getBlock()) {
                // Block đã bị break/thay đổi — bỏ qua (không ghost mine)
                continue;
            }
            // Validate: block có trong chunk loaded không?
            if (!world.isChunkLoaded(e.pos())) continue;

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

    /** Snapshot hiện tại của queue để render — READ ONLY, không modify. */
    public Set<BlockPos> snapshot() {
        Set<BlockPos> snap = new HashSet<>(inQueue);
        for (Entry e : queue) snap.add(e.pos()); // inQueue đã có tất cả nhưng giữ lại để rõ ràng
        return snap;
    }
}
