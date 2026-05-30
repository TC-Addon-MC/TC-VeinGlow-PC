package com.tcveinminer.neoforge.client.network;

import com.tcveinminer.client.VeinGlowClient;
import com.tcveinminer.neoforge.network.NeoForgePacketChannel.*;
import com.tcveinminer.network.payload.*;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NeoForgeClientPayloadHandler {
    
    public static void registerClientHandlers(PayloadRegistrar registrar) {
        registrar.playToClient(NeoForgeConfigSyncPayload.TYPE, NeoForgeConfigSyncPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> VeinGlowClient.handleConfigSync(new ConfigSyncData(payload.maxBlocks(), payload.blacklistedBlocks())));
        });

        registrar.playToClient(NeoForgeMiningStatePayload.TYPE, NeoForgeMiningStatePayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> VeinGlowClient.handleMiningState(new MiningStateData(payload.state(), payload.broken(), payload.target())));
        });

        registrar.playToClient(NeoForgeActivationConfirmPayload.TYPE, NeoForgeActivationConfirmPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> VeinGlowClient.handleActivationConfirm(new ActivationConfirmData(payload.allowContinuous())));
        });

        registrar.playToClient(NeoForgeLookedAtBlockPayload.TYPE, NeoForgeLookedAtBlockPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> VeinGlowClient.handleLookedAtBlock(new LookedAtBlockData(payload.pos())));
        });

        registrar.playToClient(NeoForgeFilterResultPayload.TYPE, NeoForgeFilterResultPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> VeinGlowClient.handleFilterResult(new FilterResultData(payload.allowHighlight())));
        });

        registrar.playToClient(NeoForgeHighlightBlockListPayload.TYPE, NeoForgeHighlightBlockListPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> VeinGlowClient.handleHighlightBlockList(new HighlightBlockListData(
                    payload.blocks(), payload.highlightStyle(), payload.source())));
        });

        registrar.playToClient(NeoForgeHighlightDeltaPayload.TYPE, NeoForgeHighlightDeltaPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> VeinGlowClient.handleHighlightDelta(new HighlightDeltaData(
                    payload.addedBlocks(), payload.removedBlocks(), payload.highlightStyle(), payload.source())));
        });
    }
}
