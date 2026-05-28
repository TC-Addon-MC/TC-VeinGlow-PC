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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

public final class InteractBlockAction implements BlockAction {

    private static Map<Block, Block> strippedBlocks = null;
    private static Map<Block, BlockState> pathStates = null;
    private static Method decreaseOxidationMethod = null;

    private static Map<?, ?> findStaticMapField(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && Map.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    return (Map<?, ?>) field.get(null);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    static {
        try {
            strippedBlocks = (Map<Block, Block>) findStaticMapField(AxeItem.class);
        } catch (Exception ignored) {}

        try {
            pathStates = (Map<Block, BlockState>) findStaticMapField(ShovelItem.class);
        } catch (Exception ignored) {}

        try {
            for (Method m : Oxidizable.class.getMethods()) {
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == BlockState.class && m.getReturnType() == Optional.class) {
                    decreaseOxidationMethod = m;
                    decreaseOxidationMethod.setAccessible(true);
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean execute(ServerPlayerEntity player, ServerWorld world, BlockPos pos, ActionContext ctx) {
        if (!(ctx instanceof ActionContext.InteractContext ic)) return false;

        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        Hand hand = ic.getInteractHand();
        ItemStack stack = player.getStackInHand(hand);

        if (stack.isEmpty()) return false;

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
            } catch (Exception ignored) {}

            // B. De-oxidize (scrape copper oxidation level)
            if (decreaseOxidationMethod != null) {
                try {
                    Optional<BlockState> decreased = (Optional<BlockState>) decreaseOxidationMethod.invoke(null, state);
                    if (decreased.isPresent()) {
                        world.playSound(null, pos, SoundEvents.ITEM_AXE_SCRAPE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                        world.syncWorldEvent(player, 3005, pos, 0); // Scrape particle event
                        world.setBlockState(pos, decreased.get(), 11);
                        if (!player.isCreative()) {
                            stack.damage(1, player, slot);
                        }
                        return true;
                    }
                } catch (Exception ignored) {}
            }

            // C. Strip Log
            Block strippedBlock = null;
            if (strippedBlocks != null) {
                strippedBlock = strippedBlocks.get(block);
            }
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

        // 2. SHOVEL INTERACTIONS (Path grass)
        if (stack.getItem() instanceof ShovelItem) {
            BlockState pathState = null;
            if (pathStates != null) {
                pathState = pathStates.get(block);
            }
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
    private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<T> property) {
        return to.with(property, from.get(property));
    }
}
