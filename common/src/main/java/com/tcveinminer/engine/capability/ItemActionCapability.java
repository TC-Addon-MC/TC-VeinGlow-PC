package com.tcveinminer.engine.capability;

import com.tcveinminer.engine.action.ActionType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * Interface for declaring what action an item can perform on a target block.
 * <p>
 * Replaces hardcoded if/else chains in the action resolver.
 * Items "expose" their capabilities through registered implementations,
 * checked in priority order by {@link CapabilityRegistry}.
 */
public interface ItemActionCapability {

    /**
     * Can this capability handle the given context?
     *
     * @param stack       the item stack held by the player
     * @param targetState the block state being targeted
     * @param player      the player performing the action
     * @return true if this capability matches the context
     */
    boolean canPerform(ItemStack stack, BlockState targetState, PlayerEntity player);

    /**
     * Priority — lower values are checked first.
     * Standard priorities: 100 (UseItem), 200 (Plant), 300 (Hoe),
     * 400 (Bucket), 500 (Harvest), 600 (Interact).
     */
    int priority();

    /**
     * The action type this capability resolves to.
     */
    ActionType getActionType();
}
