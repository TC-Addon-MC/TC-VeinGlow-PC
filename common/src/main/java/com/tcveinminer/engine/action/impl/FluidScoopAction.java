package com.tcveinminer.engine.action.impl;

import com.tcveinminer.engine.action.ActionContext;
import com.tcveinminer.engine.action.BlockAction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

/**
 * Fluid scoop action — scoops a single fluid source block.
 * <p>
 * Bulk orchestration (tick-based queue processing) is handled by the engine.
 * This action handles one block at a time: remove fluid, swap bucket inventory.
 */
public final class FluidScoopAction implements BlockAction {

    @Override
    public boolean execute(ServerPlayer player, ServerLevel world, BlockPos pos, ActionContext ctx) {
        if (!(ctx instanceof ActionContext.BucketContext bc)) return false;

        net.minecraft.world.level.block.state.BlockState state = world.getBlockState(pos);
        if (!state.getFluidState().isSource()) return false;

        net.minecraft.world.level.block.Block LiquidBlock = state.getBlock();
        Item filledBucket;
        if (LiquidBlock == Blocks.WATER) {
            filledBucket = Items.WATER_BUCKET;
        } else if (LiquidBlock == Blocks.LAVA) {
            filledBucket = Items.LAVA_BUCKET;
        } else {
            return false;
        }

        // Remove one empty bucket from inventory
        if (!removeSingleItem(player, Items.BUCKET)) return false;

        // Remove fluid block
        world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

        // Give filled bucket
        giveSingleItem(player, filledBucket);

        return true;
    }

    @Override
    public String getId() {
        return "fluid_scoop";
    }

    // ── Inventory helpers ─────────────────────────────────────────────────

    private static boolean removeSingleItem(Player player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static void giveSingleItem(Player player, Item item) {
        ItemStack stack = new ItemStack(item, 1);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
