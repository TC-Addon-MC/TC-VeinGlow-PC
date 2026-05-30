package com.tcveinminer.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ActivationConfirmPayload(boolean allowContinuous) implements CustomPayload {
    public static final Id<ActivationConfirmPayload> ID =
        new Id<>(ResourceLocation.parse("tc_veinminer", "activation_confirm"));

    public static final PacketCodec<RegistryByteBuf, ActivationConfirmPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, ActivationConfirmPayload::allowContinuous,
            ActivationConfirmPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
