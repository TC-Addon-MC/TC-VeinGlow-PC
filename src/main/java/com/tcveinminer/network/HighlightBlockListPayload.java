package com.tcveinminer.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public record HighlightBlockListPayload(List<BlockPos> blocks, String highlightStyle) implements CustomPayload {
    public static final Id<HighlightBlockListPayload> ID =
        new Id<>(Identifier.of("tc_veinminer", "highlight_block_list"));

    public static final PacketCodec<RegistryByteBuf, HighlightBlockListPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC.collect(PacketCodecs.toList()), HighlightBlockListPayload::blocks,
            PacketCodecs.STRING, HighlightBlockListPayload::highlightStyle,
            HighlightBlockListPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
