package com.tcveinminer.engine.action.impl;

import com.tcveinminer.engine.action.ActionContext;
import com.tcveinminer.engine.action.BlockAction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.event.GameEvent;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class HoeTillAction implements BlockAction {

    private static Map<?, ?> tillingActions = null;

    static {
        for (Field field : HoeItem.class.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && Map.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    tillingActions = (Map<?, ?>) field.get(null);
                    break;
                } catch (Exception ignored) {}
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean execute(ServerPlayerEntity player, ServerWorld world, BlockPos pos, ActionContext ctx) {
        if (!(ctx instanceof ActionContext.InteractContext ic)) return false;

        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        Hand hand = ic.getInteractHand();
        ItemStack stack = player.getStackInHand(hand);

        BlockState targetState = null;
        boolean dropRoots = false;

        if (tillingActions != null) {
            Object pairObj = tillingActions.get(block);
            if (pairObj instanceof com.mojang.datafixers.util.Pair) {
                com.mojang.datafixers.util.Pair<?, ?> pair = (com.mojang.datafixers.util.Pair<?, ?>) pairObj;
                Predicate<ItemUsageContext> predicate = (Predicate<ItemUsageContext>) pair.getFirst();
                Consumer<ItemUsageContext> consumer = (Consumer<ItemUsageContext>) pair.getSecond();
                
                ItemUsageContext useCtx = new ItemUsageContext(world, player, hand, stack, ic.getHitResult().withBlockPos(pos));
                if (predicate.test(useCtx)) {
                    world.playSound(null, pos, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    if (!world.isClient) {
                        consumer.accept(useCtx);
                        if (!player.isCreative()) {
                            EquipmentSlot slot = (hand == Hand.MAIN_HAND) ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                            stack.damage(1, player, slot);
                        }
                    }
                    return true;
                }
                return false;
            }
        }

        if (block == Blocks.GRASS_BLOCK || block == Blocks.DIRT || block == Blocks.DIRT_PATH) {
            targetState = Blocks.FARMLAND.getDefaultState();
        } else if (block == Blocks.COARSE_DIRT) {
            targetState = Blocks.DIRT.getDefaultState();
        } else if (block == Blocks.ROOTED_DIRT) {
            targetState = Blocks.DIRT.getDefaultState();
            dropRoots = true;
        }

        if (targetState != null) {
            world.playSound(null, pos, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
            world.setBlockState(pos, targetState, 11);
            world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(player, targetState));
            if (dropRoots) {
                Block.dropStack(world, pos, new ItemStack(Items.HANGING_ROOTS));
            }
            if (!player.isCreative()) {
                EquipmentSlot slot = (hand == Hand.MAIN_HAND) ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                stack.damage(1, player, slot);
            }
            return true;
        }

        return false;
    }

    @Override
    public String getId() {
        return "hoe_till";
    }
}
