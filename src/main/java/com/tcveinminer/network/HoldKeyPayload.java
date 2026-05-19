package com.tcveinminer.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record HoldKeyPayload(boolean isHolding, String shapeId, int maxBlocks) implements CustomPayload {
    public static final Id<HoldKeyPayload> ID =
        new Id<>(Identifier.of("tc_veinminer", "hold_key"));

    public static final PacketCodec<RegistryByteBuf, HoldKeyPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL,    HoldKeyPayload::isHolding,
            PacketCodecs.STRING,  HoldKeyPayload::shapeId,
            PacketCodecs.INTEGER, HoldKeyPayload::maxBlocks,
            HoldKeyPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
