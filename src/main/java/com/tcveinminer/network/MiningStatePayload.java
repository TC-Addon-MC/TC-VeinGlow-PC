package com.tcveinminer.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record MiningStatePayload(boolean isMining) implements CustomPayload {
    public static final Id<MiningStatePayload> ID = new Id<>(Identifier.of("tc_veinminer", "mining_state"));
    public static final PacketCodec<RegistryByteBuf, MiningStatePayload> CODEC = PacketCodec.tuple(PacketCodecs.BOOL, MiningStatePayload::isMining, MiningStatePayload::new);
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
