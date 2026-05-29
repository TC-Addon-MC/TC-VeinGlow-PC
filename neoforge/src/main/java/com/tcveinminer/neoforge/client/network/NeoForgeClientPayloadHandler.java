package com.tcveinminer.neoforge.client.network;

import com.tcveinminer.client.VeinGlowClient;
import com.tcveinminer.neoforge.network.NeoForgePacketChannel.*;
import com.tcveinminer.network.payload.*;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NeoForgeClientPayloadHandler {
    
    public static void registerClientHandlers(PayloadRegistrar registrar) {
        registrar.playToClient(NeoForgeConfigSyncPayload.ID, NeoForgeConfigSyncPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> VeinGlowClient.handleConfigSync(new ConfigSyncData(payload.maxBlocks(), payload.blacklistedBlocks())));
        });

        registrar.playToClient(NeoForgeMiningStatePayload.ID, NeoForgeMiningStatePayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> VeinGlowClient.handleMiningState(new MiningStateData(payload.state(), payload.broken(), payload.target())));
        });

        registrar.playToClient(NeoForgeActivationConfirmPayload.ID, NeoForgeActivationConfirmPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> VeinGlowClient.handleActivationConfirm(new ActivationConfirmData(payload.allowContinuous())));
        });

        registrar.playToClient(NeoForgeLookedAtBlockPayload.ID, NeoForgeLookedAtBlockPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> VeinGlowClient.handleLookedAtBlock(new LookedAtBlockData(java.util.Optional.ofNullable(payload.pos()))));
        });

        registrar.playToClient(NeoForgeFilterResultPayload.ID, NeoForgeFilterResultPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> VeinGlowClient.handleFilterResult(new FilterResultData(payload.allowHighlight())));
        });

        registrar.playToClient(NeoForgeHighlightBlockListPayload.ID, NeoForgeHighlightBlockListPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> VeinGlowClient.handleHighlightBlockList(new HighlightBlockListData(payload.blocks(), payload.highlightStyle())));
        });

        registrar.playToClient(NeoForgeHighlightDeltaPayload.ID, NeoForgeHighlightDeltaPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> VeinGlowClient.handleHighlightDelta(new HighlightDeltaData(payload.addedBlocks(), payload.removedBlocks(), payload.highlightStyle(), payload.source())));
        });
    }
}
