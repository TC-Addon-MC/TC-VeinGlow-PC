package com.tcveinminer.neoforge.network;

import com.tcveinminer.network.NetworkPacket;
import com.tcveinminer.network.PacketChannel;
import com.tcveinminer.network.payload.*;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;
import java.util.Optional;

public final class NeoForgePacketChannel implements PacketChannel {

    // ── S→C Payload wrappers ─────────────────────────────────────────────

    public record NeoForgeConfigSyncPayload(int maxBlocks, List<String> blacklistedBlocks) implements CustomPayload {
        public static final Id<NeoForgeConfigSyncPayload> ID = new Id<>(Identifier.of("tc_veinminer", "config_sync"));
        public static final PacketCodec<RegistryByteBuf, NeoForgeConfigSyncPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.INTEGER, NeoForgeConfigSyncPayload::maxBlocks,
                PacketCodecs.STRING.collect(PacketCodecs.toList()), NeoForgeConfigSyncPayload::blacklistedBlocks,
                NeoForgeConfigSyncPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record NeoForgeMiningStatePayload(int state, int broken, int target) implements CustomPayload {
        public static final Id<NeoForgeMiningStatePayload> ID = new Id<>(Identifier.of("tc_veinminer", "mining_state"));
        public static final PacketCodec<RegistryByteBuf, NeoForgeMiningStatePayload> CODEC = PacketCodec.tuple(
                PacketCodecs.INTEGER, NeoForgeMiningStatePayload::state,
                PacketCodecs.INTEGER, NeoForgeMiningStatePayload::broken,
                PacketCodecs.INTEGER, NeoForgeMiningStatePayload::target,
                NeoForgeMiningStatePayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record NeoForgeActivationConfirmPayload(boolean allowContinuous) implements CustomPayload {
        public static final Id<NeoForgeActivationConfirmPayload> ID = new Id<>(Identifier.of("tc_veinminer", "activation_confirm"));
        public static final PacketCodec<RegistryByteBuf, NeoForgeActivationConfirmPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.BOOL, NeoForgeActivationConfirmPayload::allowContinuous,
                NeoForgeActivationConfirmPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record NeoForgeLookedAtBlockPayload(BlockPos pos) implements CustomPayload {
        public static final Id<NeoForgeLookedAtBlockPayload> ID = new Id<>(Identifier.of("tc_veinminer", "looked_at_block"));
        public static final PacketCodec<RegistryByteBuf, NeoForgeLookedAtBlockPayload> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC, NeoForgeLookedAtBlockPayload::pos,
                NeoForgeLookedAtBlockPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record NeoForgeFilterResultPayload(boolean allowHighlight) implements CustomPayload {
        public static final Id<NeoForgeFilterResultPayload> ID = new Id<>(Identifier.of("tc_veinminer", "filter_result"));
        public static final PacketCodec<RegistryByteBuf, NeoForgeFilterResultPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.BOOL, NeoForgeFilterResultPayload::allowHighlight,
                NeoForgeFilterResultPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record NeoForgeHighlightBlockListPayload(List<BlockPos> blocks, String highlightStyle) implements CustomPayload {
        public static final Id<NeoForgeHighlightBlockListPayload> ID = new Id<>(Identifier.of("tc_veinminer", "highlight_block_list"));
        public static final PacketCodec<RegistryByteBuf, NeoForgeHighlightBlockListPayload> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC.collect(PacketCodecs.toList()), NeoForgeHighlightBlockListPayload::blocks,
                PacketCodecs.STRING, NeoForgeHighlightBlockListPayload::highlightStyle,
                NeoForgeHighlightBlockListPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record NeoForgeHighlightDeltaPayload(List<BlockPos> addedBlocks, List<BlockPos> removedBlocks, String highlightStyle, String source) implements CustomPayload {
        public static final Id<NeoForgeHighlightDeltaPayload> ID = new Id<>(Identifier.of("tc_veinminer", "highlight_delta"));
        public static final PacketCodec<RegistryByteBuf, NeoForgeHighlightDeltaPayload> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC.collect(PacketCodecs.toList()), NeoForgeHighlightDeltaPayload::addedBlocks,
                BlockPos.PACKET_CODEC.collect(PacketCodecs.toList()), NeoForgeHighlightDeltaPayload::removedBlocks,
                PacketCodecs.STRING, NeoForgeHighlightDeltaPayload::highlightStyle,
                PacketCodecs.STRING, NeoForgeHighlightDeltaPayload::source,
                NeoForgeHighlightDeltaPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ── C→S Payload wrappers ─────────────────────────────────────────────

    public record NeoForgeHoldKeyPayload(boolean isHolding, String shapeId, int maxBlocks, String equation, List<String> blacklist) implements CustomPayload {
        public static final Id<NeoForgeHoldKeyPayload> ID = new Id<>(Identifier.of("tc_veinminer", "hold_key"));
        public static final PacketCodec<RegistryByteBuf, NeoForgeHoldKeyPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.BOOL, NeoForgeHoldKeyPayload::isHolding,
                PacketCodecs.STRING, NeoForgeHoldKeyPayload::shapeId,
                PacketCodecs.INTEGER, NeoForgeHoldKeyPayload::maxBlocks,
                PacketCodecs.STRING, NeoForgeHoldKeyPayload::equation,
                PacketCodecs.STRING.collect(PacketCodecs.toList()), NeoForgeHoldKeyPayload::blacklist,
                NeoForgeHoldKeyPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record NeoForgeActivationRequestPayload(boolean active, Optional<BlockPos> targetPos) implements CustomPayload {
        public static final Id<NeoForgeActivationRequestPayload> ID = new Id<>(Identifier.of("tc_veinminer", "activation_request"));
        public static final PacketCodec<RegistryByteBuf, NeoForgeActivationRequestPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.BOOL, NeoForgeActivationRequestPayload::active,
                PacketCodecs.optional(BlockPos.PACKET_CODEC), NeoForgeActivationRequestPayload::targetPos,
                NeoForgeActivationRequestPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ── Implementation ──────────────────────────────────────────────────

    public void registerPackets(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("tc_veinminer");

        // C2S
        registrar.playToServer(NeoForgeHoldKeyPayload.ID, NeoForgeHoldKeyPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                com.tcveinminer.network.ServerNetworkHandler.handleHoldKeyPayload(
                    (ServerPlayerEntity) context.player(),
                    new HoldKeyData(payload.isHolding(), payload.shapeId(), payload.maxBlocks(), payload.equation(), payload.blacklist())
                );
            });
        });

        registrar.playToServer(NeoForgeActivationRequestPayload.ID, NeoForgeActivationRequestPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                com.tcveinminer.network.ServerNetworkHandler.handleActivationRequestPayload(
                    (ServerPlayerEntity) context.player(),
                    new ActivationRequestData(payload.active(), payload.targetPos())
                );
            });
        });

        
        if (net.neoforged.api.distmarker.FMLEnvironment.dist.isClient()) {
            com.tcveinminer.neoforge.client.network.NeoForgeClientPayloadHandler.registerClientHandlers(registrar);
        }

        // Wait, in NeoForge, playToClient requires the handler. We can pass a dummy handler if we are on server, but in common we just pass a proxy.
        // To be safe, we just let the client entrypoint register the client receivers, OR we can register them dynamically.
        // Let's just register playToClient here using a static method from ClientNetworkManager if possible, but that would crash on dedicated servers!
    }

    public void registerServerPackets() {
        // NeoForge requires registering via event. This method is empty because we hook the event directly.
    }

    @Override
    public void sendToPlayer(ServerPlayerEntity player, NetworkPacket packet) {
        PacketDistributor.sendToPlayer(player, toNeoForgePayload(packet));
    }

    @Override
    public void sendToAll(NetworkPacket packet, Iterable<ServerPlayerEntity> players) {
        CustomPayload p = toNeoForgePayload(packet);
        for (var player : players) {
            PacketDistributor.sendToPlayer(player, p);
        }
    }

    @Override
    public boolean canSendToPlayer(ServerPlayerEntity player) {
        // NeoForge implicitly checks this. Just return true for now.
        return true;
    }

    public static CustomPayload toNeoForgePayload(NetworkPacket packet) {
        return switch (packet) {
            case ConfigSyncData d -> new NeoForgeConfigSyncPayload(d.maxBlocks(), d.blacklistedBlocks());
            case MiningStateData d -> new NeoForgeMiningStatePayload(d.state(), d.broken(), d.target());
            case ActivationConfirmData d -> new NeoForgeActivationConfirmPayload(d.allowContinuous());
            case LookedAtBlockData d -> new NeoForgeLookedAtBlockPayload(d.pos().orElse(null)); // Need Optional mapping
            case FilterResultData d -> new NeoForgeFilterResultPayload(d.allowHighlight());
            case HighlightBlockListData d -> new NeoForgeHighlightBlockListPayload(d.blocks(), d.highlightStyle());
            case HighlightDeltaData d -> new NeoForgeHighlightDeltaPayload(d.addedBlocks(), d.removedBlocks(), d.highlightStyle(), d.source());
            case HoldKeyData d -> new NeoForgeHoldKeyPayload(d.isHolding(), d.shapeId(), d.maxBlocks(), d.equation(), d.blacklistedBlocks());
            case ActivationRequestData d -> new NeoForgeActivationRequestPayload(d.active(), d.targetPos());
        };
    }
}
