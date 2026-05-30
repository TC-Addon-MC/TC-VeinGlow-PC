package com.tcveinminer.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.Map;

public record HoldKeyPayload(boolean isHolding, String shapeId, int maxBlocks, String equation, List<String> blacklist) implements CustomPayload {
    public static final Id<HoldKeyPayload> ID =
        new Id<>(ResourceLocation.parse("tc_veinminer", "hold_key"));

    public static final PacketCodec<RegistryByteBuf, HoldKeyPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL,    HoldKeyPayload::isHolding,
            PacketCodecs.STRING,  HoldKeyPayload::shapeId,
            PacketCodecs.INTEGER, HoldKeyPayload::maxBlocks,
            PacketCodecs.STRING,  HoldKeyPayload::equation,
            PacketCodecs.STRING.collect(PacketCodecs.toList()), HoldKeyPayload::blacklist,
            HoldKeyPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
