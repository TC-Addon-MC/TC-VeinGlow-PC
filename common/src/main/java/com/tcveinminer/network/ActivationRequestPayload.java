package com.tcveinminer.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import java.util.Optional;

public record ActivationRequestPayload(boolean active, Optional<BlockPos> targetPos) implements CustomPayload {
    public static final Id<ActivationRequestPayload> ID =
        new Id<>(Identifier.of("tc_veinminer", "activation_request"));

    public static final PacketCodec<RegistryByteBuf, ActivationRequestPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, ActivationRequestPayload::active,
            PacketCodecs.optional(BlockPos.PACKET_CODEC), ActivationRequestPayload::targetPos,
            ActivationRequestPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
