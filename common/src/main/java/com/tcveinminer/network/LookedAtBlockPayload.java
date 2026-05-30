package com.tcveinminer.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

public record LookedAtBlockPayload(BlockPos pos) implements CustomPayload {
    public static final Id<LookedAtBlockPayload> ID =
        new Id<>(ResourceLocation.parse("tc_veinminer", "looked_at_block"));

    public static final PacketCodec<RegistryByteBuf, LookedAtBlockPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, LookedAtBlockPayload::pos,
            LookedAtBlockPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
