package com.tcveinminer.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import java.util.Optional;

public record ActivationRequestPayload(boolean active, Optional<BlockPos> targetPos) implements CustomPayload {
    public static final Id<ActivationRequestPayload> ID =
        new Id<>(ResourceLocation.parse("tc_veinminer", "activation_request"));

    public static final PacketCodec<RegistryByteBuf, ActivationRequestPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, ActivationRequestPayload::active,
            PacketCodecs.optional(BlockPos.PACKET_CODEC), ActivationRequestPayload::targetPos,
            ActivationRequestPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
