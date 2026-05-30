package com.tcveinminer.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

import java.util.List;

public record HighlightBlockListPayload(List<BlockPos> blocks, String highlightStyle, String source) implements CustomPayload {
    public static final Id<HighlightBlockListPayload> ID =
        new Id<>(ResourceLocation.parse("tc_veinminer", "highlight_block_list"));

    public static final PacketCodec<RegistryByteBuf, HighlightBlockListPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC.collect(PacketCodecs.toList()), HighlightBlockListPayload::blocks,
            PacketCodecs.STRING, HighlightBlockListPayload::highlightStyle,
            PacketCodecs.STRING, HighlightBlockListPayload::source,
            HighlightBlockListPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
