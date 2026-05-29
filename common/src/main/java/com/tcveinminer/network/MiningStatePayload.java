package com.tcveinminer.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record MiningStatePayload(int state, int broken, int target) implements CustomPayload {
    public static final Id<MiningStatePayload> ID = new Id<>(Identifier.of("tc_veinminer", "mining_state"));
    public static final PacketCodec<RegistryByteBuf, MiningStatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, MiningStatePayload::state,
            PacketCodecs.INTEGER, MiningStatePayload::broken,
            PacketCodecs.INTEGER, MiningStatePayload::target,
            MiningStatePayload::new
    );
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
