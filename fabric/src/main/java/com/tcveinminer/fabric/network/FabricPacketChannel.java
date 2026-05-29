package com.tcveinminer.fabric.network;

import com.tcveinminer.network.NetworkPacket;
import com.tcveinminer.network.PacketChannel;
import com.tcveinminer.network.payload.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

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

    record FabricConfigSyncPayload(int maxBlocks, List<String> blacklistedBlocks) implements CustomPayload {
        static final Id<FabricConfigSyncPayload> ID = new Id<>(Identifier.of("tc_veinminer", "config_sync"));
        static final PacketCodec<RegistryByteBuf, FabricConfigSyncPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.INTEGER, FabricConfigSyncPayload::maxBlocks,
                PacketCodecs.STRING.collect(PacketCodecs.toList()), FabricConfigSyncPayload::blacklistedBlocks,
                FabricConfigSyncPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    record FabricMiningStatePayload(int state, int broken, int target) implements CustomPayload {
        static final Id<FabricMiningStatePayload> ID = new Id<>(Identifier.of("tc_veinminer", "mining_state"));
        static final PacketCodec<RegistryByteBuf, FabricMiningStatePayload> CODEC = PacketCodec.tuple(
                PacketCodecs.INTEGER, FabricMiningStatePayload::state,
                PacketCodecs.INTEGER, FabricMiningStatePayload::broken,
                PacketCodecs.INTEGER, FabricMiningStatePayload::target,
                FabricMiningStatePayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    record FabricActivationConfirmPayload(boolean allowContinuous) implements CustomPayload {
        static final Id<FabricActivationConfirmPayload> ID = new Id<>(Identifier.of("tc_veinminer", "activation_confirm"));
        static final PacketCodec<RegistryByteBuf, FabricActivationConfirmPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.BOOL, FabricActivationConfirmPayload::allowContinuous,
                FabricActivationConfirmPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    record FabricLookedAtBlockPayload(BlockPos pos) implements CustomPayload {
        static final Id<FabricLookedAtBlockPayload> ID = new Id<>(Identifier.of("tc_veinminer", "looked_at_block"));
        static final PacketCodec<RegistryByteBuf, FabricLookedAtBlockPayload> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC, FabricLookedAtBlockPayload::pos,
                FabricLookedAtBlockPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    record FabricFilterResultPayload(boolean allowHighlight) implements CustomPayload {
        static final Id<FabricFilterResultPayload> ID = new Id<>(Identifier.of("tc_veinminer", "filter_result"));
        static final PacketCodec<RegistryByteBuf, FabricFilterResultPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.BOOL, FabricFilterResultPayload::allowHighlight,
                FabricFilterResultPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    record FabricHighlightBlockListPayload(List<BlockPos> blocks, String highlightStyle) implements CustomPayload {
        static final Id<FabricHighlightBlockListPayload> ID = new Id<>(Identifier.of("tc_veinminer", "highlight_block_list"));
        static final PacketCodec<RegistryByteBuf, FabricHighlightBlockListPayload> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC.collect(PacketCodecs.toList()), FabricHighlightBlockListPayload::blocks,
                PacketCodecs.STRING, FabricHighlightBlockListPayload::highlightStyle,
                FabricHighlightBlockListPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    record FabricHighlightDeltaPayload(List<BlockPos> addedBlocks, List<BlockPos> removedBlocks,
                                        String highlightStyle, String source) implements CustomPayload {
        static final Id<FabricHighlightDeltaPayload> ID = new Id<>(Identifier.of("tc_veinminer", "highlight_delta"));
        static final PacketCodec<RegistryByteBuf, FabricHighlightDeltaPayload> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC.collect(PacketCodecs.toList()), FabricHighlightDeltaPayload::addedBlocks,
                BlockPos.PACKET_CODEC.collect(PacketCodecs.toList()), FabricHighlightDeltaPayload::removedBlocks,
                PacketCodecs.STRING, FabricHighlightDeltaPayload::highlightStyle,
                PacketCodecs.STRING, FabricHighlightDeltaPayload::source,
                FabricHighlightDeltaPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ── C→S Payload wrappers ──────────────────────────────────────────────
    // These are the same as the originals in the old network/ package.
    // Kept here so all Fabric networking is in one place.

    public record FabricHoldKeyPayload(boolean isHolding, String shapeId, int maxBlocks,
                                        String equation, List<String> blacklist) implements CustomPayload {
        public static final Id<FabricHoldKeyPayload> ID = new Id<>(Identifier.of("tc_veinminer", "hold_key"));
        public static final PacketCodec<RegistryByteBuf, FabricHoldKeyPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.BOOL, FabricHoldKeyPayload::isHolding,
                PacketCodecs.STRING, FabricHoldKeyPayload::shapeId,
                PacketCodecs.INTEGER, FabricHoldKeyPayload::maxBlocks,
                PacketCodecs.STRING, FabricHoldKeyPayload::equation,
                PacketCodecs.STRING.collect(PacketCodecs.toList()), FabricHoldKeyPayload::blacklist,
                FabricHoldKeyPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record FabricActivationRequestPayload(boolean active, Optional<BlockPos> targetPos) implements CustomPayload {
        public static final Id<FabricActivationRequestPayload> ID = new Id<>(Identifier.of("tc_veinminer", "activation_request"));
        public static final PacketCodec<RegistryByteBuf, FabricActivationRequestPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.BOOL, FabricActivationRequestPayload::active,
                PacketCodecs.optional(BlockPos.PACKET_CODEC), FabricActivationRequestPayload::targetPos,
                FabricActivationRequestPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
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
        PayloadTypeRegistry.playS2C().register(FabricHighlightBlockListPayload.ID, FabricHighlightBlockListPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FabricHighlightDeltaPayload.ID, FabricHighlightDeltaPayload.CODEC);
    }

    @Override
    public void sendToPlayer(ServerPlayerEntity player, NetworkPacket packet) {
        ServerPlayNetworking.send(player, toFabricPayload(packet));
    }

    @Override
    public void sendToAll(NetworkPacket packet, Iterable<ServerPlayerEntity> players) {
        CustomPayload fabricPayload = toFabricPayload(packet);
        for (var player : players) {
            ServerPlayNetworking.send(player, fabricPayload);
        }
    }

    @Override
    public boolean canSendToPlayer(ServerPlayerEntity player) {
        return ServerPlayNetworking.canSend(player, FabricConfigSyncPayload.ID);
    }

    /** Adapter: common NetworkPacket → Fabric CustomPayload. */
    private CustomPayload toFabricPayload(NetworkPacket packet) {
        return switch (packet) {
            case ConfigSyncData d       -> new FabricConfigSyncPayload(d.maxBlocks(), d.blacklistedBlocks());
            case MiningStateData d      -> new FabricMiningStatePayload(d.state(), d.broken(), d.target());
            case ActivationConfirmData d-> new FabricActivationConfirmPayload(d.allowContinuous());
            case LookedAtBlockData d    -> new FabricLookedAtBlockPayload(d.pos());
            case FilterResultData d     -> new FabricFilterResultPayload(d.allowHighlight());
            case HighlightBlockListData d -> new FabricHighlightBlockListPayload(d.blocks(), d.highlightStyle());
            case HighlightDeltaData d   -> new FabricHighlightDeltaPayload(d.addedBlocks(), d.removedBlocks(), d.highlightStyle(), d.source());
            // C→S packets should never be sent S→C
            case HoldKeyData d          -> throw new IllegalArgumentException("HoldKeyData is C→S only");
            case ActivationRequestData d-> throw new IllegalArgumentException("ActivationRequestData is C→S only");
        };
    }
}
