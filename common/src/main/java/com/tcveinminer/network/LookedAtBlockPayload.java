package com.tcveinminer.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record LookedAtBlockPayload(BlockPos pos) implements CustomPayload {
    public static final Id<LookedAtBlockPayload> ID =
        new Id<>(Identifier.of("tc_veinminer", "looked_at_block"));

    public static final PacketCodec<RegistryByteBuf, LookedAtBlockPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, LookedAtBlockPayload::pos,
            LookedAtBlockPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
