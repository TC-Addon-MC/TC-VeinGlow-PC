package com.tcveinminer.logic;

import com.tcveinminer.TCVeinMinerMod;
import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.hud.SessionStats;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public final class VeinMinerLogic {

    // 6 mặt
    private static final int[][] D6 = {
            {1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}
    };

    // 26 hướng (kể cả chéo)
    private static final int[][] D26;
    static {
        List<int[]> d = new ArrayList<>();
        for (int dx=-1;dx<=1;dx++)
            for (int dy=-1;dy<=1;dy++)
                for (int dz=-1;dz<=1;dz++)
                    if (dx!=0||dy!=0||dz!=0) d.add(new int[]{dx,dy,dz});
        D26 = d.toArray(new int[0][]);
    }

    // 12 hướng: 6 mặt + 8 cạnh ngang (không chéo dọc)
    private static final int[][] D18;
    static {
        List<int[]> d = new ArrayList<>(Arrays.asList(D6));
        for (int dx=-1;dx<=1;dx++)
            for (int dz=-1;dz<=1;dz++)
                if (Math.abs(dx)+Math.abs(dz)==2) d.add(new int[]{dx,0,dz});
        // thêm chéo dọc (không góc)
        for (int dx=-1;dx<=1;dx+=2) { d.add(new int[]{dx,1,0}); d.add(new int[]{dx,-1,0}); }
        for (int dz=-1;dz<=1;dz+=2) { d.add(new int[]{0,1,dz}); d.add(new int[]{0,-1,dz}); }
        D18 = d.toArray(new int[0][]);
    }

    private static final Map<UUID, Long> cooldowns = new HashMap<>();

    public static void onBreak(PlayerEntity player, World world, BlockPos pos, BlockState state) {
        if (!(world instanceof ServerWorld sw)) return;
        ModConfig c = ConfigManager.get();
        if (!c.enabled) return;
        if (!TCVeinMinerMod.playersHoldingV.contains(player.getUuid())) return;
        if (c.requireCorrectTool && !toolOk(player, c)) return;

        long tick = sw.getTime();
        if (c.cooldownTicks > 0) {
            long avail = cooldowns.getOrDefault(player.getUuid(), 0L);
            if (tick < avail) return;
            cooldowns.put(player.getUuid(), tick + c.cooldownTicks);
        }

        String blockId = Registries.BLOCK.getId(state.getBlock()).toString();
        if (c.blacklistedBlocks.contains(blockId)) return;

        List<BlockPos> chain = bfs(sw, pos, state, c);
        if (chain.isEmpty()) return;

        SessionStats.onActivate();
        ItemStack tool = player.getMainHandStack();
        int broken = 0;

        for (BlockPos bp : chain) {
            if (broken >= c.maxBlocks - 1) break;
            String id = Registries.BLOCK.getId(sw.getBlockState(bp).getBlock()).toString();
            sw.breakBlock(bp, true, player);
            SessionStats.onBlock(id);
            broken++;
            if (c.consumeDurability && !tool.isEmpty() && tool.isDamageable()) {
                tool.damage(1, player, EquipmentSlot.MAINHAND);
                SessionStats.onDurability(1);
                if (tool.isEmpty()) break;
            }
        }

        HudNotifier.lastMined = broken;
        HudNotifier.lastMax   = c.maxBlocks;
        HudNotifier.notifyAt  = System.currentTimeMillis() + 2500;
    }

    public static List<BlockPos> bfs(World w, BlockPos origin, BlockState target, ModConfig c) {
        return switch (c.miningShape) {
            case FACE       -> bfsGeneric(w, origin, target, c, D6);
            case EDGES      -> bfsGeneric(w, origin, target, c, D18);
            case CORNERS    -> bfsGeneric(w, origin, target, c, D26);
            case TALL_1x2   -> bfsTall(w, origin, target, c);
            case STAIR_UP   -> bfsStair(w, origin, target, c, 1);
            case STAIR_DOWN -> bfsStair(w, origin, target, c, -1);
            case AREA_3x3   -> bfs3x3(w, origin, target, c);
        };
    }

    // Generic BFS với direction set tùy ý
    private static List<BlockPos> bfsGeneric(World w, BlockPos origin, BlockState target,
                                             ModConfig c, int[][] dirs) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        seen.add(origin);
        queue.add(origin);
        while (!queue.isEmpty() && result.size() < c.maxBlocks - 1) {
            BlockPos cur = queue.poll();
            for (int[] d : dirs) {
                BlockPos nb = cur.add(d[0], d[1], d[2]);
                if (seen.add(nb) && matchesBlock(w.getBlockState(nb), target)) {
                    result.add(nb);
                    queue.add(nb);
                }
            }
        }
        return result;
    }

    // 1x2: đào theo cột đứng (cùng block, 2 cao)
    private static List<BlockPos> bfsTall(World w, BlockPos origin, BlockState target, ModConfig c) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        seen.add(origin);
        queue.add(origin);
        while (!queue.isEmpty() && result.size() < c.maxBlocks - 1) {
            BlockPos cur = queue.poll();
            // chỉ spread ngang (D6 trừ Y) + lên/xuống 1 block
            for (int[] d : D6) {
                BlockPos nb = cur.add(d[0], d[1], d[2]);
                if (!seen.add(nb)) continue;
                if (!matchesBlock(w.getBlockState(nb), target)) continue;
                // Cho phép spread ngang + cặp Y (mỗi cặp 2 block đứng liền)
                result.add(nb);
                queue.add(nb);
                // Thêm block trên/dưới ngay cặp nếu match
                BlockPos pair = nb.add(0, d[1] == 0 ? 1 : 0, 0);
                if (seen.add(pair) && matchesBlock(w.getBlockState(pair), target)) {
                    result.add(pair);
                    queue.add(pair);
                }
            }
        }
        return result;
    }

    // Stair: đào hướng lên (dy=+1) hoặc xuống (dy=-1) theo đường thẳng
    private static List<BlockPos> bfsStair(World w, BlockPos origin, BlockState target,
                                           ModConfig c, int dy) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        seen.add(origin);
        queue.add(origin);
        while (!queue.isEmpty() && result.size() < c.maxBlocks - 1) {
            BlockPos cur = queue.poll();
            // Spread mặt + hướng cầu thang chéo
            for (int[] d : D6) {
                BlockPos nb = cur.add(d[0], d[1], d[2]);
                if (!seen.add(nb) || !matchesBlock(w.getBlockState(nb), target)) continue;
                result.add(nb);
                queue.add(nb);
            }
            // Bước cầu thang: tiến 1 ngang + dy dọc
            for (int[] horiz : new int[][]{{1,0,0},{-1,0,0},{0,0,1},{0,0,-1}}) {
                BlockPos stairStep = cur.add(horiz[0], dy, horiz[2]);
                if (!seen.add(stairStep) || !matchesBlock(w.getBlockState(stairStep), target)) continue;
                result.add(stairStep);
                queue.add(stairStep);
            }
        }
        return result;
    }

    // 3x3: lấy tất cả block trong hình trụ 3x3 xung quanh mỗi block match
    private static List<BlockPos> bfs3x3(World w, BlockPos origin, BlockState target, ModConfig c) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        seen.add(origin);
        queue.add(origin);
        while (!queue.isEmpty() && result.size() < c.maxBlocks - 1) {
            BlockPos cur = queue.poll();
            // 3x3 xung quanh trên cùng mặt Y
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    BlockPos nb = cur.add(dx, 0, dz);
                    if (!seen.add(nb) || !matchesBlock(w.getBlockState(nb), target)) continue;
                    result.add(nb);
                    queue.add(nb);
                }
            }
            // Tiếp tục theo mặt 6 hướng cơ bản để chain
            for (int[] d : D6) {
                BlockPos nb = cur.add(d[0], d[1], d[2]);
                if (!seen.add(nb) || !matchesBlock(w.getBlockState(nb), target)) continue;
                result.add(nb);
                queue.add(nb);
            }
        }
        return result;
    }

    private static boolean matchesBlock(BlockState a, BlockState b) {
        return a.getBlock() == b.getBlock();
    }

    private static boolean toolOk(PlayerEntity p, ModConfig c) {
        ItemStack s = p.getMainHandStack();
        if (s.isEmpty())            return c.enabledTools.getOrDefault("hand", false);
        if (s.getItem() instanceof PickaxeItem) return c.enabledTools.getOrDefault("pickaxe", true);
        if (s.getItem() instanceof AxeItem)     return c.enabledTools.getOrDefault("axe", false);
        if (s.getItem() instanceof ShovelItem)  return c.enabledTools.getOrDefault("shovel", false);
        if (s.getItem() instanceof SwordItem)   return c.enabledTools.getOrDefault("sword", false);
        if (s.getItem() instanceof HoeItem)     return c.enabledTools.getOrDefault("hoe", false);
        return false;
    }

    private VeinMinerLogic() {}
}