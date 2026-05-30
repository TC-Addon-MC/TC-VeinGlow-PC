package com.tcveinminer.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FilterResultPayload(boolean allowHighlight) implements CustomPacketPayload {
    public static final Type<FilterResultPayload> ID =
        new Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "filter_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FilterResultPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, FilterResultPayload::allowHighlight,
            FilterResultPayload::new
    );

    @Override public Type<? extends CustomPacketPayload> type() { return ID; }

    public String channelId() { return ID.id().toString(); }
}
