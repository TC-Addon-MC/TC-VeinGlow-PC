package com.tcveinminer.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

import java.util.List;

public record HighlightDeltaPayload(List<BlockPos> addedBlocks, List<BlockPos> removedBlocks, String highlightStyle, String source) implements CustomPacketPayload {
    public static final Type<HighlightDeltaPayload> ID =
        new Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "highlight_delta"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HighlightDeltaPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), HighlightDeltaPayload::addedBlocks,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), HighlightDeltaPayload::removedBlocks,
            ByteBufCodecs.STRING_UTF8, HighlightDeltaPayload::highlightStyle,
            ByteBufCodecs.STRING_UTF8, HighlightDeltaPayload::source,
            HighlightDeltaPayload::new
    );

    @Override public Type<? extends CustomPacketPayload> type() { return ID; }

    public String channelId() { return ID.id().toString(); }
}
