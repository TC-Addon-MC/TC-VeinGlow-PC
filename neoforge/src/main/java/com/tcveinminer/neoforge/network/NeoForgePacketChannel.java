package com.tcveinminer.neoforge.network;

import com.tcveinminer.network.payload.NetworkPacket;
import com.tcveinminer.network.PacketChannel;
import com.tcveinminer.network.payload.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;
import java.util.Optional;

public final class NeoForgePacketChannel implements PacketChannel {

    // ── S→C Payload wrappers ─────────────────────────────────────────────

    public record NeoForgeConfigSyncPayload(int maxBlocks, List<String> blacklistedBlocks) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<NeoForgeConfigSyncPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "config_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, NeoForgeConfigSyncPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, NeoForgeConfigSyncPayload::maxBlocks,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), NeoForgeConfigSyncPayload::blacklistedBlocks,
                NeoForgeConfigSyncPayload::new);
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record NeoForgeMiningStatePayload(int state, int broken, int target) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<NeoForgeMiningStatePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "mining_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, NeoForgeMiningStatePayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, NeoForgeMiningStatePayload::state,
                ByteBufCodecs.INT, NeoForgeMiningStatePayload::broken,
                ByteBufCodecs.INT, NeoForgeMiningStatePayload::target,
                NeoForgeMiningStatePayload::new);
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record NeoForgeActivationConfirmPayload(boolean allowContinuous) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<NeoForgeActivationConfirmPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "activation_confirm"));
        public static final StreamCodec<RegistryFriendlyByteBuf, NeoForgeActivationConfirmPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, NeoForgeActivationConfirmPayload::allowContinuous,
                NeoForgeActivationConfirmPayload::new);
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record NeoForgeLookedAtBlockPayload(Optional<Long> pos) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<NeoForgeLookedAtBlockPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "looked_at_block"));
        public static final StreamCodec<RegistryFriendlyByteBuf, NeoForgeLookedAtBlockPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(ByteBufCodecs.VAR_LONG), NeoForgeLookedAtBlockPayload::pos,
                NeoForgeLookedAtBlockPayload::new);
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record NeoForgeFilterResultPayload(boolean allowHighlight) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<NeoForgeFilterResultPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "filter_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, NeoForgeFilterResultPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, NeoForgeFilterResultPayload::allowHighlight,
                NeoForgeFilterResultPayload::new);
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record NeoForgeHighlightBlockListPayload(List<Long> blocks,
                                                    String highlightStyle, String source) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<NeoForgeHighlightBlockListPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "highlight_block_list"));
        public static final StreamCodec<RegistryFriendlyByteBuf, NeoForgeHighlightBlockListPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()), NeoForgeHighlightBlockListPayload::blocks,
                ByteBufCodecs.STRING_UTF8, NeoForgeHighlightBlockListPayload::highlightStyle,
                ByteBufCodecs.STRING_UTF8, NeoForgeHighlightBlockListPayload::source,
                NeoForgeHighlightBlockListPayload::new);
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record NeoForgeHighlightDeltaPayload(List<Long> addedBlocks,
                                                List<Long> removedBlocks,
                                                String highlightStyle, String source) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<NeoForgeHighlightDeltaPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "highlight_delta"));
        public static final StreamCodec<RegistryFriendlyByteBuf, NeoForgeHighlightDeltaPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()), NeoForgeHighlightDeltaPayload::addedBlocks,
                ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()), NeoForgeHighlightDeltaPayload::removedBlocks,
                ByteBufCodecs.STRING_UTF8, NeoForgeHighlightDeltaPayload::highlightStyle,
                ByteBufCodecs.STRING_UTF8, NeoForgeHighlightDeltaPayload::source,
                NeoForgeHighlightDeltaPayload::new);
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ── C→S Payload wrappers ─────────────────────────────────────────────

    public record NeoForgeHoldKeyPayload(boolean isHolding, String shapeId, int maxBlocks,
                                         String equation, List<String> blacklist) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<NeoForgeHoldKeyPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "hold_key"));
        public static final StreamCodec<RegistryFriendlyByteBuf, NeoForgeHoldKeyPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, NeoForgeHoldKeyPayload::isHolding,
                ByteBufCodecs.STRING_UTF8, NeoForgeHoldKeyPayload::shapeId,
                ByteBufCodecs.INT, NeoForgeHoldKeyPayload::maxBlocks,
                ByteBufCodecs.STRING_UTF8, NeoForgeHoldKeyPayload::equation,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), NeoForgeHoldKeyPayload::blacklist,
                NeoForgeHoldKeyPayload::new);
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record NeoForgeActivationRequestPayload(boolean active,
                                                   Optional<Long> targetPos) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<NeoForgeActivationRequestPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "activation_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, NeoForgeActivationRequestPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, NeoForgeActivationRequestPayload::active,
                ByteBufCodecs.optional(ByteBufCodecs.VAR_LONG), NeoForgeActivationRequestPayload::targetPos,
                NeoForgeActivationRequestPayload::new);
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ── Implementation ──────────────────────────────────────────────────

    public void registerPackets(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("tc_veinminer");

        // C2S
        registrar.playToServer(NeoForgeHoldKeyPayload.TYPE, NeoForgeHoldKeyPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                com.tcveinminer.network.handlers.HoldKeyHandler.handleRaw(
                    context.player(),
                    new HoldKeyData(payload.isHolding(), payload.shapeId(), payload.maxBlocks(), payload.equation(), payload.blacklist())
                );
            });
        });

        registrar.playToServer(NeoForgeActivationRequestPayload.TYPE, NeoForgeActivationRequestPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                com.tcveinminer.network.handlers.ActivationRequestHandler.handleRaw(
                    context.player(),
                    new ActivationRequestData(payload.active(), payload.targetPos())
                );
            });
        });

        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            com.tcveinminer.neoforge.client.network.NeoForgeClientPayloadHandler.registerClientHandlers(registrar);
        }
    }

    public void registerServerPackets() {
        // NeoForge requires registering via event. This method is empty because we hook the event directly.
    }

    @Override
    public void sendToPlayer(Object player, NetworkPacket packet) {
        PacketDistributor.sendToPlayer((net.minecraft.server.level.ServerPlayer) player, toNeoForgePayload(packet));
    }

    @Override
    public void sendToAll(NetworkPacket packet, Iterable<?> players) {
        CustomPacketPayload p = toNeoForgePayload(packet);
        for (var player : players) {
            PacketDistributor.sendToPlayer((net.minecraft.server.level.ServerPlayer) player, p);
        }
    }

    @Override
    public boolean canSendToPlayer(Object player) {
        return true;
    }

    public static CustomPacketPayload toNeoForgePayload(NetworkPacket packet) {
        return switch (packet) {
            case ConfigSyncData d -> new NeoForgeConfigSyncPayload(d.maxBlocks(), d.blacklistedBlocks());
            case MiningStateData d -> new NeoForgeMiningStatePayload(d.state(), d.broken(), d.target());
            case ActivationConfirmData d -> new NeoForgeActivationConfirmPayload(d.allowContinuous());
            case LookedAtBlockData d -> new NeoForgeLookedAtBlockPayload(d.pos());
            case FilterResultData d -> new NeoForgeFilterResultPayload(d.allowHighlight());
            case HighlightBlockListData d -> new NeoForgeHighlightBlockListPayload(d.blocks(), d.highlightStyle(), d.source());
            case HighlightDeltaData d -> new NeoForgeHighlightDeltaPayload(d.addedBlocks(), d.removedBlocks(), d.highlightStyle(), d.source());
            case HoldKeyData d -> new NeoForgeHoldKeyPayload(d.isHolding(), d.shapeId(), d.maxBlocks(), d.equation(), d.blacklist());
            case ActivationRequestData d -> new NeoForgeActivationRequestPayload(d.active(), d.targetPos());
        };
    }
}
