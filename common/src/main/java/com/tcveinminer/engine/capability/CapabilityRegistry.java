package com.tcveinminer.engine.capability;

import com.tcveinminer.engine.action.ActionType;
import com.tcveinminer.engine.capability.impl.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Registry of {@link ItemActionCapability} implementations, sorted by priority.
 * <p>
 * Resolves the highest-priority action for a given player/item/block context.
 * Returns {@link ActionType#VANILLA_FALLBACK} if nothing matches.
 */
public final class CapabilityRegistry {

    private static final List<ItemActionCapability> CAPABILITIES = new ArrayList<>();

    static {
        // Priority order (lower = checked first)
        register(new UseItemCapability());          // 100 — bone meal, flint, etc.
        register(new PlantCapability());            // 200 — seeds on farmland
        register(new HoeCapability());              // 300 — hoe items
        register(new BucketCapability());           // 400 — empty bucket + fluid
        register(new HarvestCapability());          // 500 — mature crops
        register(new InteractBlockCapability());    // 600 — strip/path/etc.
    }

    private CapabilityRegistry() {}

    /**
     * Register a new capability. The registry is re-sorted by priority after each registration.
     */
    public static void register(ItemActionCapability capability) {
        CAPABILITIES.add(capability);
        CAPABILITIES.sort(Comparator.comparingInt(ItemActionCapability::priority));
    }

    /**
     * Resolve the highest-priority action for the given context.
     *
     * @param player      the player
     * @param heldItem    the item stack held in the given hand
     * @param targetState the block state being targeted
     * @param hand        the hand used (unused in resolution but available for future use)
     * @return the resolved action type, or {@link ActionType#VANILLA_FALLBACK}
     */
    public static ActionType resolve(PlayerEntity player, ItemStack heldItem,
                                      BlockState targetState, Hand hand) {
        for (ItemActionCapability cap : CAPABILITIES) {
            if (cap.canPerform(heldItem, targetState, player)) {
                return cap.getActionType();
            }
        }
        return ActionType.VANILLA_FALLBACK;
    }
}
