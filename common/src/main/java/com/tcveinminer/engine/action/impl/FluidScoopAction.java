package com.tcveinminer.engine.action.impl;

import com.tcveinminer.engine.action.ActionContext;
import com.tcveinminer.engine.action.BlockAction;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Fluid scoop action — scoops a single fluid source block.
 * <p>
 * Bulk orchestration (tick-based queue processing) is handled by the engine.
 * This action handles one block at a time: remove fluid, swap bucket inventory.
 */
public final class FluidScoopAction implements BlockAction {

    @Override
    public boolean execute(ServerPlayerEntity player, ServerWorld world, BlockPos pos, ActionContext ctx) {
        if (!(ctx instanceof ActionContext.BucketContext bc)) return false;

        net.minecraft.block.BlockState state = world.getBlockState(pos);
        if (!state.getFluidState().isStill()) return false;

        net.minecraft.block.Block fluidBlock = state.getBlock();
        Item filledBucket;
        if (fluidBlock == Blocks.WATER) {
            filledBucket = Items.WATER_BUCKET;
        } else if (fluidBlock == Blocks.LAVA) {
            filledBucket = Items.LAVA_BUCKET;
        } else {
            return false;
        }

        // Remove one empty bucket from inventory
        if (!removeSingleItem(player, Items.BUCKET)) return false;

        // Remove fluid block
        world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);

        // Give filled bucket
        giveSingleItem(player, filledBucket);

        return true;
    }

    @Override
    public String getId() {
        return "fluid_scoop";
    }

    // ── Inventory helpers ─────────────────────────────────────────────────

    private static boolean removeSingleItem(PlayerEntity player, Item item) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                stack.decrement(1);
                return true;
            }
        }
        return false;
    }

    private static void giveSingleItem(PlayerEntity player, Item item) {
        ItemStack stack = new ItemStack(item, 1);
        if (!player.getInventory().insertStack(stack)) {
            player.dropItem(stack, false);
        }
    }
}
