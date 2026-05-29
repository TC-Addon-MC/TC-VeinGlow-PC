package com.tcveinminer.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record FilterResultPayload(boolean allowHighlight) implements CustomPayload {
    public static final Id<FilterResultPayload> ID =
        new Id<>(Identifier.of("tc_veinminer", "filter_result"));

    public static final PacketCodec<RegistryByteBuf, FilterResultPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, FilterResultPayload::allowHighlight,
            FilterResultPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
