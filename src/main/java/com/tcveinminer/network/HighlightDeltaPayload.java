package com.tcveinminer.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public record HighlightDeltaPayload(List<BlockPos> addedBlocks, List<BlockPos> removedBlocks, String highlightStyle, String source) implements CustomPayload {
    public static final Id<HighlightDeltaPayload> ID =
        new Id<>(Identifier.of("tc_veinminer", "highlight_delta"));

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
