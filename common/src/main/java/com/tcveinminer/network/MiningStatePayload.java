package com.tcveinminer.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MiningStatePayload(int state, int broken, int target) implements CustomPacketPayload {
    public static final Type<MiningStatePayload> ID = new Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "mining_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MiningStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, MiningStatePayload::state,
            ByteBufCodecs.INT, MiningStatePayload::broken,
            ByteBufCodecs.INT, MiningStatePayload::target,
            MiningStatePayload::new
    );
    @Override public Type<? extends CustomPacketPayload> type() { return ID; }

    public String channelId() { return ID.id().toString(); }
}
