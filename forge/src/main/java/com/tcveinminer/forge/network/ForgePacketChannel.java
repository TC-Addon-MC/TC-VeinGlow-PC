package com.tcveinminer.forge.network;

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

import java.util.List;
import java.util.Optional;

public final class ForgePacketChannel implements PacketChannel {

    public record ForgeConfigSyncPayload(int maxBlocks, List<String> blacklistedBlocks) implements CustomPayload {
        public static final Id<ForgeConfigSyncPayload> ID = new Id<>(Identifier.of("tc_veinminer", "config_sync"));
        public static final PacketCodec<RegistryByteBuf, ForgeConfigSyncPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.INTEGER, ForgeConfigSyncPayload::maxBlocks,
                PacketCodecs.STRING.collect(PacketCodecs.toList()), ForgeConfigSyncPayload::blacklistedBlocks,
                ForgeConfigSyncPayload::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ForgeMiningStatePayload(int state, int broken, int target) implements CustomPayload {
        public static final Id<ForgeMiningStatePayload> ID = new Id<>(Identifier.of("tc_veinminer", "mining_state"));
        public static final PacketCodec<RegistryByteBuf, ForgeMiningStatePayload> CODEC = PacketCodec.tuple(
                PacketCodecs.INTEGER, ForgeMiningStatePayload::state,
                PacketCodecs.INTEGER, ForgeMiningStatePayload::broken,
                PacketCodecs.INTEGER, ForgeMiningStatePayload::target,
                ForgeMiningStatePayload::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ForgeActivationConfirmPayload(boolean allowContinuous) implements CustomPayload {
        public static final Id<ForgeActivationConfirmPayload> ID = new Id<>(
                Identifier.of("tc_veinminer", "activation_confirm"));
        public static final PacketCodec<RegistryByteBuf, ForgeActivationConfirmPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.BOOL, ForgeActivationConfirmPayload::allowContinuous,
                ForgeActivationConfirmPayload::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ForgeLookedAtBlockPayload(BlockPos pos) implements CustomPayload {
        public static final Id<ForgeLookedAtBlockPayload> ID = new Id<>(
                Identifier.of("tc_veinminer", "looked_at_block"));
        public static final PacketCodec<RegistryByteBuf, ForgeLookedAtBlockPayload> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC, ForgeLookedAtBlockPayload::pos,
                ForgeLookedAtBlockPayload::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ForgeFilterResultPayload(boolean allowHighlight) implements CustomPayload {
        public static final Id<ForgeFilterResultPayload> ID = new Id<>(Identifier.of("tc_veinminer", "filter_result"));
        public static final PacketCodec<RegistryByteBuf, ForgeFilterResultPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.BOOL, ForgeFilterResultPayload::allowHighlight,
                ForgeFilterResultPayload::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ForgeHighlightBlockListPayload(List<BlockPos> blocks, String highlightStyle)
            implements CustomPayload {
        public static final Id<ForgeHighlightBlockListPayload> ID = new Id<>(
                Identifier.of("tc_veinminer", "highlight_block_list"));
        public static final PacketCodec<RegistryByteBuf, ForgeHighlightBlockListPayload> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC.collect(PacketCodecs.toList()), ForgeHighlightBlockListPayload::blocks,
                PacketCodecs.STRING, ForgeHighlightBlockListPayload::highlightStyle,
                ForgeHighlightBlockListPayload::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ForgeHighlightDeltaPayload(List<BlockPos> addedBlocks, List<BlockPos> removedBlocks,
            String highlightStyle, String source) implements CustomPayload {
        public static final Id<ForgeHighlightDeltaPayload> ID = new Id<>(
                Identifier.of("tc_veinminer", "highlight_delta"));
        public static final PacketCodec<RegistryByteBuf, ForgeHighlightDeltaPayload> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC.collect(PacketCodecs.toList()), ForgeHighlightDeltaPayload::addedBlocks,
                BlockPos.PACKET_CODEC.collect(PacketCodecs.toList()), ForgeHighlightDeltaPayload::removedBlocks,
                PacketCodecs.STRING, ForgeHighlightDeltaPayload::highlightStyle,
                PacketCodecs.STRING, ForgeHighlightDeltaPayload::source,
                ForgeHighlightDeltaPayload::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ForgeHoldKeyPayload(boolean isHolding, String shapeId, int maxBlocks, String equation,
            List<String> blacklist) implements CustomPayload {
        public static final Id<ForgeHoldKeyPayload> ID = new Id<>(Identifier.of("tc_veinminer", "hold_key"));
        public static final PacketCodec<RegistryByteBuf, ForgeHoldKeyPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.BOOL, ForgeHoldKeyPayload::isHolding,
                PacketCodecs.STRING, ForgeHoldKeyPayload::shapeId,
                PacketCodecs.INTEGER, ForgeHoldKeyPayload::maxBlocks,
                PacketCodecs.STRING, ForgeHoldKeyPayload::equation,
                PacketCodecs.STRING.collect(PacketCodecs.toList()), ForgeHoldKeyPayload::blacklist,
                ForgeHoldKeyPayload::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ForgeActivationRequestPayload(boolean active, Optional<BlockPos> targetPos) implements CustomPayload {
        public static final Id<ForgeActivationRequestPayload> ID = new Id<>(
                Identifier.of("tc_veinminer", "activation_request"));
        public static final PacketCodec<RegistryByteBuf, ForgeActivationRequestPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.BOOL, ForgeActivationRequestPayload::active,
                PacketCodecs.optional(BlockPos.PACKET_CODEC), ForgeActivationRequestPayload::targetPos,
                ForgeActivationRequestPayload::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public void registerPackets(final net.minecraftforge.event.network.RegisterPayloadHandlersEvent event) {
        final net.minecraftforge.network.registration.PayloadRegistrar registrar = event.registrar("tc_veinminer");

        registrar.playToServer(ForgeHoldKeyPayload.ID, ForgeHoldKeyPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                com.tcveinminer.network.ServerNetworkHandler.handleHoldKeyPayload(
                        (ServerPlayerEntity) context.player(),
                        new HoldKeyData(payload.isHolding(), payload.shapeId(), payload.maxBlocks(), payload.equation(),
                                payload.blacklist()));
            });
        });

        registrar.playToServer(ForgeActivationRequestPayload.ID, ForgeActivationRequestPayload.CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        com.tcveinminer.network.ServerNetworkHandler.handleActivationRequestPayload(
                                (ServerPlayerEntity) context.player(),
                                new ActivationRequestData(payload.active(), payload.targetPos()));
                    });
                });
    }

    @Override
    public void registerServerPackets() {
    }

    @Override
    public void sendToPlayer(ServerPlayerEntity player, NetworkPacket packet) {
        player.networkHandler
                .sendPacket(new net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket(toForgePayload(packet)));
    }

    @Override
    public void sendToAll(NetworkPacket packet, Iterable<ServerPlayerEntity> players) {
        net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket p = new net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket(
                toForgePayload(packet));
        for (var player : players) {
            player.networkHandler.sendPacket(p);
        }
    }

    @Override
    public boolean canSendToPlayer(ServerPlayerEntity player) {
        return true;
    }

    public static CustomPayload toForgePayload(NetworkPacket packet) {
        return switch (packet) {
            case ConfigSyncData d -> new ForgeConfigSyncPayload(d.maxBlocks(), d.blacklistedBlocks());
            case MiningStateData d -> new ForgeMiningStatePayload(d.state(), d.broken(), d.target());
            case ActivationConfirmData d -> new ForgeActivationConfirmPayload(d.allowContinuous());
            case LookedAtBlockData d -> new ForgeLookedAtBlockPayload(d.pos().orElse(null));
            case FilterResultData d -> new ForgeFilterResultPayload(d.allowHighlight());
            case HighlightBlockListData d -> new ForgeHighlightBlockListPayload(d.blocks(), d.highlightStyle());
            case HighlightDeltaData d ->
                new ForgeHighlightDeltaPayload(d.addedBlocks(), d.removedBlocks(), d.highlightStyle(), d.source());
            case HoldKeyData d ->
                new ForgeHoldKeyPayload(d.isHolding(), d.shapeId(), d.maxBlocks(), d.equation(), d.blacklistedBlocks());
            case ActivationRequestData d -> new ForgeActivationRequestPayload(d.active(), d.targetPos());
        };
    }
}