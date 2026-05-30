package com.tcveinminer.forge.network;

import com.tcveinminer.network.payload.NetworkPacket;
import com.tcveinminer.network.PacketChannel;
import com.tcveinminer.network.payload.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkProtocol;
import net.minecraftforge.network.SimpleChannel;

import java.util.List;
import java.util.Optional;

public final class ForgePacketChannel implements PacketChannel {

    public static final SimpleChannel CHANNEL = ChannelBuilder.named(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "main"))
            .networkProtocolVersion(1)
            .simpleChannel();

    // ── S→C Payload wrappers ─────────────────────────────────────────────

    public record ForgeConfigSyncPayload(int maxBlocks, List<String> blacklistedBlocks) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ForgeConfigSyncPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "config_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ForgeConfigSyncPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, ForgeConfigSyncPayload::maxBlocks,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ForgeConfigSyncPayload::blacklistedBlocks,
                ForgeConfigSyncPayload::new);
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ForgeMiningStatePayload(int state, int broken, int target) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ForgeMiningStatePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "mining_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ForgeMiningStatePayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, ForgeMiningStatePayload::state,
                ByteBufCodecs.INT, ForgeMiningStatePayload::broken,
                ByteBufCodecs.INT, ForgeMiningStatePayload::target,
                ForgeMiningStatePayload::new);
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ForgeActivationConfirmPayload(boolean allowContinuous) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ForgeActivationConfirmPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "activation_confirm"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ForgeActivationConfirmPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, ForgeActivationConfirmPayload::allowContinuous,
                ForgeActivationConfirmPayload::new);
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ForgeLookedAtBlockPayload(Optional<Long> pos) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ForgeLookedAtBlockPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "looked_at_block"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ForgeLookedAtBlockPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(ByteBufCodecs.VAR_LONG), ForgeLookedAtBlockPayload::pos,
                ForgeLookedAtBlockPayload::new);
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ForgeFilterResultPayload(boolean allowHighlight) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ForgeFilterResultPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "filter_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ForgeFilterResultPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, ForgeFilterResultPayload::allowHighlight,
                ForgeFilterResultPayload::new);
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ForgeHighlightBlockListPayload(List<Long> blocks,
                                                    String highlightStyle, String source) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ForgeHighlightBlockListPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "highlight_block_list"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ForgeHighlightBlockListPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()), ForgeHighlightBlockListPayload::blocks,
                ByteBufCodecs.STRING_UTF8, ForgeHighlightBlockListPayload::highlightStyle,
                ByteBufCodecs.STRING_UTF8, ForgeHighlightBlockListPayload::source,
                ForgeHighlightBlockListPayload::new);
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ForgeHighlightDeltaPayload(List<Long> addedBlocks,
                                                List<Long> removedBlocks,
                                                String highlightStyle, String source) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ForgeHighlightDeltaPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "highlight_delta"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ForgeHighlightDeltaPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()), ForgeHighlightDeltaPayload::addedBlocks,
                ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()), ForgeHighlightDeltaPayload::removedBlocks,
                ByteBufCodecs.STRING_UTF8, ForgeHighlightDeltaPayload::highlightStyle,
                ByteBufCodecs.STRING_UTF8, ForgeHighlightDeltaPayload::source,
                ForgeHighlightDeltaPayload::new);
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ── C→S Payload wrappers ─────────────────────────────────────────────

    public record ForgeHoldKeyPayload(boolean isHolding, String shapeId, int maxBlocks,
                                         String equation, List<String> blacklist) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ForgeHoldKeyPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "hold_key"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ForgeHoldKeyPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, ForgeHoldKeyPayload::isHolding,
                ByteBufCodecs.STRING_UTF8, ForgeHoldKeyPayload::shapeId,
                ByteBufCodecs.INT, ForgeHoldKeyPayload::maxBlocks,
                ByteBufCodecs.STRING_UTF8, ForgeHoldKeyPayload::equation,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ForgeHoldKeyPayload::blacklist,
                ForgeHoldKeyPayload::new);
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ForgeActivationRequestPayload(boolean active,
                                                   Optional<Long> targetPos) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ForgeActivationRequestPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tc_veinminer", "activation_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ForgeActivationRequestPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, ForgeActivationRequestPayload::active,
                ByteBufCodecs.optional(ByteBufCodecs.VAR_LONG), ForgeActivationRequestPayload::targetPos,
                ForgeActivationRequestPayload::new);
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ── Implementation ──────────────────────────────────────────────────

    public void registerPackets() {
        // C2S
        CHANNEL.messageBuilder(ForgeHoldKeyPayload.class, NetworkProtocol.PLAY)
            .codec(ForgeHoldKeyPayload.CODEC)
            .direction(PacketFlow.SERVERBOUND)
            .consumerMainThread((payload, context) -> {
                com.tcveinminer.network.handlers.HoldKeyHandler.handleRaw(
                    context.getSender(),
                    new HoldKeyData(payload.isHolding(), payload.shapeId(), payload.maxBlocks(), payload.equation(), payload.blacklist())
                );
            }).add();

        CHANNEL.messageBuilder(ForgeActivationRequestPayload.class, NetworkProtocol.PLAY)
            .codec(ForgeActivationRequestPayload.CODEC)
            .direction(PacketFlow.SERVERBOUND)
            .consumerMainThread((payload, context) -> {
                com.tcveinminer.network.handlers.ActivationRequestHandler.handleRaw(
                    context.getSender(),
                    new ActivationRequestData(payload.active(), payload.targetPos())
                );
            }).add();

        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            com.tcveinminer.forge.client.network.ForgeClientPayloadHandler.registerClientHandlers();
        }
    }

    @Override
    public void registerServerPackets() {
        // Handled entirely by init/ChannelBuilder
    }

    @Override
    public void sendToPlayer(Object player, NetworkPacket packet) {
        ((net.minecraft.server.level.ServerPlayer) player).connection.send(new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(toForgePayload(packet)));
    }

    @Override
    public void sendToAll(NetworkPacket packet, Iterable<?> players) {
        CustomPacketPayload p = toForgePayload(packet);
        var vanillaPacket = new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(p);
        for (var player : players) {
            ((net.minecraft.server.level.ServerPlayer) player).connection.send(vanillaPacket);
        }
    }

    @Override
    public boolean canSendToPlayer(Object player) {
        return true;
    }

    public static CustomPacketPayload toForgePayload(NetworkPacket packet) {
        return switch (packet) {
            case ConfigSyncData d -> new ForgeConfigSyncPayload(d.maxBlocks(), d.blacklistedBlocks());
            case MiningStateData d -> new ForgeMiningStatePayload(d.state(), d.broken(), d.target());
            case ActivationConfirmData d -> new ForgeActivationConfirmPayload(d.allowContinuous());
            case LookedAtBlockData d -> new ForgeLookedAtBlockPayload(d.pos());
            case FilterResultData d -> new ForgeFilterResultPayload(d.allowHighlight());
            case HighlightBlockListData d -> new ForgeHighlightBlockListPayload(d.blocks(), d.highlightStyle(), d.source());
            case HighlightDeltaData d -> new ForgeHighlightDeltaPayload(d.addedBlocks(), d.removedBlocks(), d.highlightStyle(), d.source());
            case HoldKeyData d -> new ForgeHoldKeyPayload(d.isHolding(), d.shapeId(), d.maxBlocks(), d.equation(), d.blacklist());
            case ActivationRequestData d -> new ForgeActivationRequestPayload(d.active(), d.targetPos());
        };
    }
}
