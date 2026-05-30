package com.tcveinminer.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

import java.util.List;

public record HighlightDeltaPayload(List<BlockPos> addedBlocks, List<BlockPos> removedBlocks, String highlightStyle, String source) implements CustomPayload {
    public static final Id<HighlightDeltaPayload> ID =
        new Id<>(ResourceLocation.parse("tc_veinminer", "highlight_delta"));

    public static final PacketCodec<RegistryByteBuf, HighlightDeltaPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC.collect(PacketCodecs.toList()), HighlightDeltaPayload::addedBlocks,
            BlockPos.PACKET_CODEC.collect(PacketCodecs.toList()), HighlightDeltaPayload::removedBlocks,
            PacketCodecs.STRING, HighlightDeltaPayload::highlightStyle,
            PacketCodecs.STRING, HighlightDeltaPayload::source,
            HighlightDeltaPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
