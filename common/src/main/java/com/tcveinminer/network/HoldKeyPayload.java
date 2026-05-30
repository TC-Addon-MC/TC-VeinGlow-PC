package com.tcveinminer.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

public record HoldKeyPayload(boolean isHolding, String shapeId, int maxBlocks, String equation, List<String> blacklist) implements CustomPacketPayload {
    public static final Type<HoldKeyPayload> ID =
        new Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "hold_key"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HoldKeyPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,    HoldKeyPayload::isHolding,
            ByteBufCodecs.STRING_UTF8,  HoldKeyPayload::shapeId,
            ByteBufCodecs.INT, HoldKeyPayload::maxBlocks,
            ByteBufCodecs.STRING_UTF8,  HoldKeyPayload::equation,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), HoldKeyPayload::blacklist,
            HoldKeyPayload::new
    );

    @Override public Type<? extends CustomPacketPayload> type() { return ID; }

    public String channelId() { return ID.id().toString(); }
}
