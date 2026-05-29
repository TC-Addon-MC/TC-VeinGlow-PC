package com.tcveinminer.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

public record ConfigSyncPayload(
        int maxBlocks,
        List<String> blacklistedBlocks
) implements CustomPayload {

    public static final Id<ConfigSyncPayload> ID =
            new Id<>(Identifier.of("tc_veinminer", "config_sync"));

    public static final PacketCodec<RegistryByteBuf, ConfigSyncPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.INTEGER,
                    ConfigSyncPayload::maxBlocks,

                    PacketCodecs.STRING.collect(PacketCodecs.toList()),
                    ConfigSyncPayload::blacklistedBlocks,

                    ConfigSyncPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
