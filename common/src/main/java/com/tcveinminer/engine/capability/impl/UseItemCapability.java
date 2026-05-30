package com.tcveinminer.engine.capability.impl;

import com.tcveinminer.engine.action.ActionType;
import com.tcveinminer.engine.capability.ItemActionCapability;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

/**
 * Detects items that have explicit vanilla use behavior and should
 * NOT be intercepted by the mod (bone meal, flint & steel, spawn eggs, etc.).
 * <p>
 * Priority 100 — checked first, so these items always pass through to vanilla.
 */
public final class UseItemCapability implements ItemActionCapability {

    @Override
    public boolean canPerform(ItemStack stack, BlockState targetState, Player player) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();

        // Bone meal
        if (item == Items.BONE_MEAL) return true;
        // Flint and steel
        if (item == Items.FLINT_AND_STEEL) return true;
        // Fire charge
        if (item == Items.FIRE_CHARGE) return true;
        // Spawn eggs
        if (item instanceof SpawnEggItem) return true;
        // Lead
        if (item == Items.LEAD) return true;
        // Name tag
        if (item == Items.NAME_TAG) return true;
        // Ender eye
        if (item == Items.ENDER_EYE) return true;
        // Dyes (for signs, sheep, etc.)
        if (item instanceof DyeItem) return true;
        // Armor stand
        if (item == Items.ARMOR_STAND) return true;
        // Boats
        if (item instanceof BoatItem) return true;
        // Minecarts
        if (item instanceof MinecartItem) return true;

        return false;
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.USE_ITEM;
    }
}
