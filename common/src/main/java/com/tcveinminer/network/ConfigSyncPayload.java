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
) implements CustomPacketPayload {

    public static final Type<ConfigSyncPayload> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "config_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSyncPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    ConfigSyncPayload::maxBlocks,

                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                    ConfigSyncPayload::blacklistedBlocks,

                    ConfigSyncPayload::new
            );

    @Override public Type<? extends CustomPacketPayload> type() { return ID; }

    public String channelId() { return ID.id().toString(); }
}
