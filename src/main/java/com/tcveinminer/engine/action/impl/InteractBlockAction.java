package com.tcveinminer.engine.action.impl;

import com.tcveinminer.engine.action.ActionContext;
import com.tcveinminer.engine.action.BlockAction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Oxidizable;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoneycombItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShovelItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Property;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InteractBlockAction implements BlockAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(InteractBlockAction.class);

    @SuppressWarnings("unchecked")
    @Override
    public boolean execute(ServerPlayerEntity player, ServerWorld world, BlockPos pos, ActionContext ctx) {
        if (!(ctx instanceof ActionContext.InteractContext ic))
            return false;

        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        Hand hand = ic.getInteractHand();
        ItemStack stack = player.getStackInHand(hand);

        if (stack.isEmpty())
            return false;

        EquipmentSlot slot = (hand == Hand.MAIN_HAND) ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;

        // 1. AXE INTERACTIONS
        if (stack.getItem() instanceof AxeItem) {
            // A. Wax off (waxed copper -> copper)
            try {
                Map<Block, Block> waxedMap = HoneycombItem.WAXED_TO_UNWAXED_BLOCKS.get();
                if (waxedMap != null && waxedMap.containsKey(block)) {
                    Block unwaxedBlock = waxedMap.get(block);
                    BlockState targetState = copyProperties(state, unwaxedBlock.getDefaultState());
                    world.playSound(null, pos, SoundEvents.ITEM_AXE_WAX_OFF, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    world.syncWorldEvent(player, 3004, pos, 0); // Wax off particle event
                    world.setBlockState(pos, targetState, 11);
                    if (!player.isCreative()) {
                        stack.damage(1, player, slot);
                    }
                    return true;
                }
            } catch (Exception ignored) {
            }

            // B. De-oxidize (scrape copper oxidation level)
            Optional<BlockState> decreased = Oxidizable.getDecreasedOxidationState(state);
            if (decreased.isPresent()) {
                world.playSound(null, pos, SoundEvents.ITEM_AXE_SCRAPE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                world.syncWorldEvent(player, 3005, pos, 0); // Scrape particle event
                world.setBlockState(pos, decreased.get(), 11);
                if (!player.isCreative()) {
                    stack.damage(1, player, slot);
                }
                return true;
            }

            // C. Strip Log
            Map<Block, Block> strippedBlocksMap = AxeItem.STRIPPED_BLOCKS;
            if (strippedBlocksMap == null || strippedBlocksMap.isEmpty()) {
                LOGGER.warn("AxeItem.STRIPPED_BLOCKS is null or empty. Stripping logs may not work.");
            } else {
                Block strippedBlock = strippedBlocksMap.get(block);
                if (strippedBlock != null) {
                    BlockState targetState = copyProperties(state, strippedBlock.getDefaultState());
                    world.playSound(null, pos, SoundEvents.ITEM_AXE_STRIP, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    world.setBlockState(pos, targetState, 11);
                    if (!player.isCreative()) {
                        stack.damage(1, player, slot);
                    }
                    return true;
                }
            }
        }

        // 2. SHOVEL INTERACTIONS (Path grass)
        if (stack.getItem() instanceof ShovelItem) {
            Map<Block, BlockState> pathStatesMap = ShovelItem.PATH_STATES;
            if (pathStatesMap == null || pathStatesMap.isEmpty()) {
                LOGGER.warn("ShovelItem.PATH_STATES is null or empty. Creating paths may not work.");
            } else {
                BlockState pathState = pathStatesMap.get(block);
                if (pathState != null) {
                    BlockPos above = pos.up();
                    if (world.getBlockState(above).isAir() || world.getBlockState(above).isReplaceable()) {
                        world.playSound(null, pos, SoundEvents.ITEM_SHOVEL_FLATTEN, SoundCategory.BLOCKS, 1.0F, 1.0F);
                        world.setBlockState(pos, pathState, 11);
                        if (!player.isCreative()) {
                            stack.damage(1, player, slot);
                        }
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public String getId() {
        return "interact_block";
    }

    private static BlockState copyProperties(BlockState from, BlockState to) {
        BlockState result = to;
        for (Property<?> property : from.getProperties()) {
            if (result.contains(property)) {
                result = copyProperty(from, result, property);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to,
            Property<T> property) {
        return to.with(property, from.get(property));
    }
}
