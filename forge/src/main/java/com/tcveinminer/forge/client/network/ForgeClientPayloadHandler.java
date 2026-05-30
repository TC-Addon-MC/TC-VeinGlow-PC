package com.tcveinminer.forge.client.network;

import com.tcveinminer.client.VeinGlowClient;
import com.tcveinminer.forge.network.ForgePacketChannel;
import com.tcveinminer.forge.network.ForgePacketChannel.*;
import com.tcveinminer.network.payload.*;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraftforge.network.NetworkProtocol;

public class ForgeClientPayloadHandler {
    
    public static void registerClientHandlers() {
        ForgePacketChannel.CHANNEL.messageBuilder(ForgeConfigSyncPayload.class, NetworkProtocol.PLAY)
            .codec(ForgeConfigSyncPayload.CODEC)
            .direction(PacketFlow.CLIENTBOUND)
            .consumerMainThread((payload, context) -> {
                VeinGlowClient.handleConfigSync(new ConfigSyncData(payload.maxBlocks(), payload.blacklistedBlocks()));
            }).add();

        ForgePacketChannel.CHANNEL.messageBuilder(ForgeMiningStatePayload.class, NetworkProtocol.PLAY)
            .codec(ForgeMiningStatePayload.CODEC)
            .direction(PacketFlow.CLIENTBOUND)
            .consumerMainThread((payload, context) -> {
                VeinGlowClient.handleMiningState(new MiningStateData(payload.state(), payload.broken(), payload.target()));
            }).add();

        ForgePacketChannel.CHANNEL.messageBuilder(ForgeActivationConfirmPayload.class, NetworkProtocol.PLAY)
            .codec(ForgeActivationConfirmPayload.CODEC)
            .direction(PacketFlow.CLIENTBOUND)
            .consumerMainThread((payload, context) -> {
                VeinGlowClient.handleActivationConfirm(new ActivationConfirmData(payload.allowContinuous()));
            }).add();

        ForgePacketChannel.CHANNEL.messageBuilder(ForgeLookedAtBlockPayload.class, NetworkProtocol.PLAY)
            .codec(ForgeLookedAtBlockPayload.CODEC)
            .direction(PacketFlow.CLIENTBOUND)
            .consumerMainThread((payload, context) -> {
                VeinGlowClient.handleLookedAtBlock(new LookedAtBlockData(payload.pos()));
            }).add();

        ForgePacketChannel.CHANNEL.messageBuilder(ForgeFilterResultPayload.class, NetworkProtocol.PLAY)
            .codec(ForgeFilterResultPayload.CODEC)
            .direction(PacketFlow.CLIENTBOUND)
            .consumerMainThread((payload, context) -> {
                VeinGlowClient.handleFilterResult(new FilterResultData(payload.allowHighlight()));
            }).add();

        ForgePacketChannel.CHANNEL.messageBuilder(ForgeHighlightBlockListPayload.class, NetworkProtocol.PLAY)
            .codec(ForgeHighlightBlockListPayload.CODEC)
            .direction(PacketFlow.CLIENTBOUND)
            .consumerMainThread((payload, context) -> {
                VeinGlowClient.handleHighlightBlockList(new HighlightBlockListData(
                        payload.blocks(), payload.highlightStyle(), payload.source()));
            }).add();

        ForgePacketChannel.CHANNEL.messageBuilder(ForgeHighlightDeltaPayload.class, NetworkProtocol.PLAY)
            .codec(ForgeHighlightDeltaPayload.CODEC)
            .direction(PacketFlow.CLIENTBOUND)
            .consumerMainThread((payload, context) -> {
                VeinGlowClient.handleHighlightDelta(new HighlightDeltaData(
                        payload.addedBlocks(), payload.removedBlocks(), payload.highlightStyle(), payload.source()));
            }).add();
    }
}
