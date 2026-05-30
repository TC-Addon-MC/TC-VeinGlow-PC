package com.tcveinminer.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record ConfigSyncPayload(
        int maxBlocks,
        List<String> blacklistedBlocks
) implements CustomPayload {

    public static final Id<ConfigSyncPayload> ID =
            new Id<>(ResourceLocation.parse("tc_veinminer", "config_sync"));

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
