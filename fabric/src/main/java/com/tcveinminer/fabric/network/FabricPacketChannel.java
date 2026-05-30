package com.tcveinminer.fabric.network;

import com.tcveinminer.network.PacketChannel;
import com.tcveinminer.network.payload.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Optional;

/**
 * Fabric implementation of {@link PacketChannel}.
 * <p>
 * Uses Fabric's {@code PayloadTypeRegistry} + {@code ServerPlayNetworking}.
 * All Fabric networking imports are contained in this file.
 */
public final class FabricPacketChannel implements PacketChannel {

    // ── S→C Payload wrappers (inner records per payload type) ─────────────

    public record FabricConfigSyncPayload(int maxBlocks, List<String> blacklistedBlocks) implements CustomPacketPayload {
        public static final Type<FabricConfigSyncPayload> ID = new Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "config_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FabricConfigSyncPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, FabricConfigSyncPayload::maxBlocks,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), FabricConfigSyncPayload::blacklistedBlocks,
                FabricConfigSyncPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record FabricMiningStatePayload(int state, int broken, int target) implements CustomPacketPayload {
        public static final Type<FabricMiningStatePayload> ID = new Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "mining_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FabricMiningStatePayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, FabricMiningStatePayload::state,
                ByteBufCodecs.INT, FabricMiningStatePayload::broken,
                ByteBufCodecs.INT, FabricMiningStatePayload::target,
                FabricMiningStatePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record FabricActivationConfirmPayload(boolean allowContinuous) implements CustomPacketPayload {
        public static final Type<FabricActivationConfirmPayload> ID = new Type<>(
                ResourceLocation.fromNamespaceAndPath("tc_veinminer", "activation_confirm"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FabricActivationConfirmPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, FabricActivationConfirmPayload::allowContinuous,
                FabricActivationConfirmPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record FabricLookedAtBlockPayload(Optional<BlockPos> pos) implements CustomPacketPayload {
        public static final Type<FabricLookedAtBlockPayload> ID = new Type<>(
                ResourceLocation.fromNamespaceAndPath("tc_veinminer", "looked_at_block"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FabricLookedAtBlockPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(BlockPos.STREAM_CODEC), FabricLookedAtBlockPayload::pos,
                FabricLookedAtBlockPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record FabricFilterResultPayload(boolean allowHighlight) implements CustomPacketPayload {
        public static final Type<FabricFilterResultPayload> ID = new Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "filter_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FabricFilterResultPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, FabricFilterResultPayload::allowHighlight,
                FabricFilterResultPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record FabricHighlightBlockListPayload(List<BlockPos> blocks, String highlightStyle, String source)
            implements CustomPacketPayload {
        public static final Type<FabricHighlightBlockListPayload> ID = new Type<>(
                ResourceLocation.fromNamespaceAndPath("tc_veinminer", "highlight_block_list"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FabricHighlightBlockListPayload> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), FabricHighlightBlockListPayload::blocks,
                ByteBufCodecs.STRING_UTF8, FabricHighlightBlockListPayload::highlightStyle,
                ByteBufCodecs.STRING_UTF8, FabricHighlightBlockListPayload::source,
                FabricHighlightBlockListPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record FabricHighlightDeltaPayload(List<BlockPos> addedBlocks, List<BlockPos> removedBlocks,
            String highlightStyle, String source) implements CustomPacketPayload {
        public static final Type<FabricHighlightDeltaPayload> ID = new Type<>(
                ResourceLocation.fromNamespaceAndPath("tc_veinminer", "highlight_delta"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FabricHighlightDeltaPayload> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), FabricHighlightDeltaPayload::addedBlocks,
                BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), FabricHighlightDeltaPayload::removedBlocks,
                ByteBufCodecs.STRING_UTF8, FabricHighlightDeltaPayload::highlightStyle,
                ByteBufCodecs.STRING_UTF8, FabricHighlightDeltaPayload::source,
                FabricHighlightDeltaPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    // ── C→S Payload wrappers ──────────────────────────────────────────────
    // These are the same as the originals in the old network/ package.
    // Kept here so all Fabric networking is in one place.

    public record FabricHoldKeyPayload(boolean isHolding, String shapeId, int maxBlocks,
            String equation, List<String> blacklist) implements CustomPacketPayload {
        public static final Type<FabricHoldKeyPayload> ID = new Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "hold_key"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FabricHoldKeyPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, FabricHoldKeyPayload::isHolding,
                ByteBufCodecs.STRING_UTF8, FabricHoldKeyPayload::shapeId,
                ByteBufCodecs.INT, FabricHoldKeyPayload::maxBlocks,
                ByteBufCodecs.STRING_UTF8, FabricHoldKeyPayload::equation,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), FabricHoldKeyPayload::blacklist,
                FabricHoldKeyPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record FabricActivationRequestPayload(boolean active, Optional<BlockPos> targetPos)
            implements CustomPacketPayload {
        public static final Type<FabricActivationRequestPayload> ID = new Type<>(
                ResourceLocation.fromNamespaceAndPath("tc_veinminer", "activation_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FabricActivationRequestPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, FabricActivationRequestPayload::active,
                ByteBufCodecs.optional(BlockPos.STREAM_CODEC), FabricActivationRequestPayload::targetPos,
                FabricActivationRequestPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    // ── PacketChannel implementation ──────────────────────────────────────

    @Override
    public void registerServerPackets() {
        // C→S
        PayloadTypeRegistry.playC2S().register(FabricHoldKeyPayload.ID, FabricHoldKeyPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(FabricActivationRequestPayload.ID, FabricActivationRequestPayload.CODEC);

        // S→C
        PayloadTypeRegistry.playS2C().register(FabricConfigSyncPayload.ID, FabricConfigSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FabricMiningStatePayload.ID, FabricMiningStatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FabricActivationConfirmPayload.ID, FabricActivationConfirmPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FabricLookedAtBlockPayload.ID, FabricLookedAtBlockPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FabricFilterResultPayload.ID, FabricFilterResultPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FabricHighlightBlockListPayload.ID,
                FabricHighlightBlockListPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FabricHighlightDeltaPayload.ID, FabricHighlightDeltaPayload.CODEC);
    }

    @Override
    public void sendToPlayer(Object player, NetworkPacket packet) {
        ServerPlayNetworking.send((ServerPlayer) player, toFabricPayload(packet));
    }

    @Override
    public void sendToAll(NetworkPacket packet, Iterable<?> players) {
        CustomPacketPayload fabricPayload = toFabricPayload(packet);
        for (var player : players) {
            ServerPlayNetworking.send((ServerPlayer) player, fabricPayload);
        }
    }

    @Override
    public boolean canSendToPlayer(Object player) {
        return ServerPlayNetworking.canSend((ServerPlayer) player, FabricConfigSyncPayload.ID);
    }

    /** Adapter: common NetworkPacket → Fabric CustomPacketPayload. */
    private CustomPacketPayload toFabricPayload(NetworkPacket packet) {
        return switch (packet) {

            case ConfigSyncData d ->
                new FabricConfigSyncPayload(
                        d.maxBlocks(),
                        d.blacklistedBlocks());

            case MiningStateData d ->
                new FabricMiningStatePayload(
                        d.state(),
                        d.broken(),
                        d.target());

            case ActivationConfirmData d ->
                new FabricActivationConfirmPayload(
                        d.allowContinuous());

            case LookedAtBlockData d ->
                new FabricLookedAtBlockPayload(
                        d.pos().map(pos -> BlockPos.of(pos)));

            case FilterResultData d ->
                new FabricFilterResultPayload(
                        d.allowHighlight());

            case HighlightBlockListData d ->
                new FabricHighlightBlockListPayload(
                        d.blocks()
                                .stream()
                                .map(pos -> BlockPos.of(pos))
                                .toList(),
                        d.highlightStyle(),
                        d.source());

            case HighlightDeltaData d ->
                new FabricHighlightDeltaPayload(
                        d.addedBlocks()
                                .stream()
                                .map(pos -> BlockPos.of(pos))
                                .toList(),

                        d.removedBlocks()
                                .stream()
                                .map(pos -> BlockPos.of(pos))
                                .toList(),

                        d.highlightStyle(),
                        d.source());

            // C→S packets should never be sent S→C
            case HoldKeyData d ->
                throw new IllegalArgumentException("HoldKeyData is C→S only");

            case ActivationRequestData d ->
                throw new IllegalArgumentException("ActivationRequestData is C→S only");
        };
    }
}
