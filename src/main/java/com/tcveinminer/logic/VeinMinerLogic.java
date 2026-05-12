package com.tcveinminer.logic;

import com.tcveinminer.TCVeinMinerMod; // THÊM IMPORT NÀY
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.hud.SessionStats;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

import net.minecraft.entity.EquipmentSlot;

public final class VeinMinerLogic {

    private static final int[][] D6 = {
            {1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}
    };

    private static final int[][] D26;

    static {
        List<int[]> d = new ArrayList<>();

        for (int dx=-1;dx<=1;dx++)
            for (int dy=-1;dy<=1;dy++)
                for (int dz=-1;dz<=1;dz++)
                    if (dx!=0||dy!=0||dz!=0)
                        d.add(new int[]{dx,dy,dz});

        D26 = d.toArray(new int[0][]);
    }

    private static final Map<UUID, Long> cooldowns = new HashMap<>();

    public static void onBreak(PlayerEntity player, World world, BlockPos pos, BlockState state) {

        if (!(world instanceof ServerWorld sw)) return;

        ModConfig c = ModConfig.get();

        if (!c.enabled) return;

        // XÓA CHECK SNEAK CŨ
        // if (c.requireSneak && !player.isSneaking()) return;

        // THAY BẰNG CHECK GIỮ PHÍM V
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

        // Notify HUD
        HudNotifier.lastMined = broken;
        HudNotifier.lastMax   = c.maxBlocks;
        HudNotifier.notifyAt  = System.currentTimeMillis() + 2500;
    }

    // ĐỔI private -> public
    public static List<BlockPos> bfs(World w, BlockPos origin, BlockState target, ModConfig c) {

        int[][] dirs = c.diagonalMining ? D26 : D6;

        List<BlockPos> result = new ArrayList<>();

        Set<BlockPos> seen = new HashSet<>();

        Queue<BlockPos> queue = new LinkedList<>();

        seen.add(origin);

        queue.add(origin);

        while (!queue.isEmpty() && result.size() < c.maxBlocks - 1) {

            BlockPos cur = queue.poll();

            for (int[] d : dirs) {

                BlockPos nb = cur.add(d[0], d[1], d[2]);

                if (seen.add(nb) && matches(w.getBlockState(nb), target, c.miningShape)) {

                    result.add(nb);

                    queue.add(nb);
                }
            }
        }

        return result;
    }

    private static boolean matches(BlockState a, BlockState b, ModConfig.MiningShape shape) {

        return switch (shape) {

            case SAME_TAG -> sameOreTag(a, b);

            case ALL -> !a.isAir();

            case SAME_BLOCK -> a.getBlock() == b.getBlock();
        };
    }

    private static boolean sameOreTag(BlockState a, BlockState b) {

        for (var tag : List.of(
                BlockTags.COAL_ORES,
                BlockTags.IRON_ORES,
                BlockTags.GOLD_ORES,
                BlockTags.DIAMOND_ORES,
                BlockTags.EMERALD_ORES,
                BlockTags.LAPIS_ORES,
                BlockTags.REDSTONE_ORES,
                BlockTags.COPPER_ORES
        )) {
            if (a.isIn(tag) && b.isIn(tag)) return true;
        }

        return false;
    }

    private static boolean toolOk(PlayerEntity p, ModConfig c) {

        ItemStack s = p.getMainHandStack();

        if (s.isEmpty())
            return c.enabledTools.getOrDefault("hand", false);

        if (s.getItem() instanceof PickaxeItem)
            return c.enabledTools.getOrDefault("pickaxe", true);

        if (s.getItem() instanceof AxeItem)
            return c.enabledTools.getOrDefault("axe", false);

        if (s.getItem() instanceof ShovelItem)
            return c.enabledTools.getOrDefault("shovel", false);

        if (s.getItem() instanceof SwordItem)
            return c.enabledTools.getOrDefault("sword", false);

        if (s.getItem() instanceof HoeItem)
            return c.enabledTools.getOrDefault("hoe", false);

        return false;
    }

    private VeinMinerLogic() {}
}