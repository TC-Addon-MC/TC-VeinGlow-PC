package com.tcveinminer.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FilterResultPayload(boolean allowHighlight) implements CustomPayload {
    public static final Id<FilterResultPayload> ID =
        new Id<>(ResourceLocation.parse("tc_veinminer", "filter_result"));

    public static final PacketCodec<RegistryByteBuf, FilterResultPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, FilterResultPayload::allowHighlight,
            FilterResultPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
