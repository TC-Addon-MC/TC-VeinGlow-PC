package com.tcveinminer.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import java.util.Optional;

public record ActivationRequestPayload(boolean active, Optional<BlockPos> targetPos) implements CustomPacketPayload {
    public static final Type<ActivationRequestPayload> ID =
        new Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "activation_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ActivationRequestPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ActivationRequestPayload::active,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), ActivationRequestPayload::targetPos,
            ActivationRequestPayload::new
    );

    @Override public Type<? extends CustomPacketPayload> type() { return ID; }

    public String channelId() { return ID.id().toString(); }
}
