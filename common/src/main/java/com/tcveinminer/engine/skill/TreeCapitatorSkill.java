package com.tcveinminer.engine.skill;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;

public final class TreeCapitatorSkill {

    private TreeCapitatorSkill() {}

    public static void autoReplant(ServerWorld sw, PlayerEntity player, BlockPos treeOriginPos, BlockState treeSaplingType) {
        if (treeOriginPos == null || treeSaplingType == null) return;

        BlockState saplingState = treeSaplingType;
        boolean isLarge = isLargeTree(saplingState);

        if (isLarge) {
            List<BlockPos> replantPositions = get2x2BasePositions(treeOriginPos);
            if (replantPositions.size() == 4) {
                for (BlockPos pos : replantPositions) {
                    placeSapling(sw, pos, saplingState);
                }
            }
        } else {
            placeSapling(sw, treeOriginPos, saplingState);
        }
    }

    private static void placeSapling(ServerWorld sw, BlockPos start, BlockState saplingState) {
        BlockPos groundPos = findGround(sw, start);
        if (groundPos != null && saplingState.canPlaceAt(sw, groundPos)) {
            sw.setBlockState(groundPos, saplingState, 3);
        }
    }

    private static BlockPos findGround(ServerWorld sw, BlockPos start) {
        BlockPos.Mutable mutable = start.mutableCopy();
        // Bắn ray xuống tối đa 15 block để tìm đất
        for (int i = 0; i < 15; i++) {
            BlockState current = sw.getBlockState(mutable);
            BlockState below = sw.getBlockState(mutable.down());
            // Nếu block hiện tại trống (hoặc có thể bị thay thế) và block dưới cứng
            if ((current.isAir() || current.isReplaceable()) && !below.isAir() && !below.isReplaceable()) {
                return mutable.toImmutable();
            }
            mutable.move(Direction.DOWN);
        }
        return null;
    }

    /** Kiểm tra cây có phải loại 2x2 không (dark oak / jungle). */
    private static boolean isLargeTree(BlockState saplingState) {
        Block b = saplingState.getBlock();
        return b == Blocks.DARK_OAK_SAPLING || b == Blocks.JUNGLE_SAPLING;
    }

    private static List<BlockPos> get2x2BasePositions(BlockPos originPos) {
        int x = originPos.getX();
        int y = originPos.getY();
        int z = originPos.getZ();
        return List.of(
                new BlockPos(x, y, z),
                new BlockPos(x + 1, y, z),
                new BlockPos(x, y, z + 1),
                new BlockPos(x + 1, y, z + 1)
        );
    }

    public static BlockState getSaplingForBlock(Block block) {
        Identifier id = Registries.BLOCK.getId(block);
        String path = id.getPath();
        if (path.contains("oak") && !path.contains("dark_oak")) return Blocks.OAK_SAPLING.getDefaultState();
        if (path.contains("spruce")) return Blocks.SPRUCE_SAPLING.getDefaultState();
        if (path.contains("birch")) return Blocks.BIRCH_SAPLING.getDefaultState();
        if (path.contains("jungle")) return Blocks.JUNGLE_SAPLING.getDefaultState();
        if (path.contains("acacia")) return Blocks.ACACIA_SAPLING.getDefaultState();
        if (path.contains("dark_oak")) return Blocks.DARK_OAK_SAPLING.getDefaultState();
        if (path.contains("mangrove")) return Blocks.MANGROVE_PROPAGULE.getDefaultState();
        if (path.contains("cherry")) return Blocks.CHERRY_SAPLING.getDefaultState();
        return null;
    }
}
