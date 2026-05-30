package com.tcveinminer.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MiningStatePayload(int state, int broken, int target) implements CustomPayload {
    public static final Id<MiningStatePayload> ID = new Id<>(ResourceLocation.parse("tc_veinminer", "mining_state"));
    public static final PacketCodec<RegistryByteBuf, MiningStatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, MiningStatePayload::state,
            PacketCodecs.INTEGER, MiningStatePayload::broken,
            PacketCodecs.INTEGER, MiningStatePayload::target,
            MiningStatePayload::new
    );
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
