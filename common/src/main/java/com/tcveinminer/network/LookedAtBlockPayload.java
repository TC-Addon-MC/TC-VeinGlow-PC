package com.tcveinminer.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

public record LookedAtBlockPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<LookedAtBlockPayload> ID =
        new Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "looked_at_block"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LookedAtBlockPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, LookedAtBlockPayload::pos,
            LookedAtBlockPayload::new
    );

    @Override public Type<? extends CustomPacketPayload> type() { return ID; }

    public String channelId() { return ID.id().toString(); }
}
