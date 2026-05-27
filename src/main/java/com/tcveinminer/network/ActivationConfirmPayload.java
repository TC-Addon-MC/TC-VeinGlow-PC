package com.tcveinminer.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ActivationConfirmPayload(boolean allowContinuous) implements CustomPayload {
    public static final Id<ActivationConfirmPayload> ID =
        new Id<>(Identifier.of("tc_veinminer", "activation_confirm"));

    public static final PacketCodec<RegistryByteBuf, ActivationConfirmPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, ActivationConfirmPayload::allowContinuous,
            ActivationConfirmPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
