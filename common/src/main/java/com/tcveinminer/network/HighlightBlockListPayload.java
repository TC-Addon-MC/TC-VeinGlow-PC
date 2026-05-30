package com.tcveinminer.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

import java.util.List;

public record HighlightBlockListPayload(List<BlockPos> blocks, String highlightStyle, String source) implements CustomPacketPayload {
    public static final Type<HighlightBlockListPayload> ID =
        new Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "highlight_block_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HighlightBlockListPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), HighlightBlockListPayload::blocks,
            ByteBufCodecs.STRING_UTF8, HighlightBlockListPayload::highlightStyle,
            ByteBufCodecs.STRING_UTF8, HighlightBlockListPayload::source,
            HighlightBlockListPayload::new
    );

    @Override public Type<? extends CustomPacketPayload> type() { return ID; }

    public String channelId() { return ID.id().toString(); }
}
