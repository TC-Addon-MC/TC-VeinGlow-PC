package com.tcveinminer.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ActivationConfirmPayload(boolean allowContinuous) implements CustomPacketPayload {
    public static final Type<ActivationConfirmPayload> ID =
        new Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "activation_confirm"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ActivationConfirmPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ActivationConfirmPayload::allowContinuous,
            ActivationConfirmPayload::new
    );

    @Override public Type<? extends CustomPacketPayload> type() { return ID; }

    public String channelId() { return ID.id().toString(); }
}
