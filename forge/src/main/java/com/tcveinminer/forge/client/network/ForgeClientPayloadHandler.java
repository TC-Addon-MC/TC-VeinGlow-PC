package com.tcveinminer.forge.client.network;

import com.tcveinminer.client.VeinGlowClient;
import com.tcveinminer.forge.network.ForgePacketChannel;
import com.tcveinminer.forge.network.ForgePacketChannel.*;
import com.tcveinminer.network.payload.*;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraftforge.network.NetworkProtocol;

public class ForgeClientPayloadHandler {
    
    public static void handleConfigSync(ForgeConfigSyncPayload payload) {
        VeinGlowClient.handleConfigSync(new ConfigSyncData(payload.maxBlocks(), payload.blacklistedBlocks()));
    }

    public static void handleMiningState(ForgeMiningStatePayload payload) {
        VeinGlowClient.handleMiningState(new MiningStateData(payload.state(), payload.broken(), payload.target()));
    }

    public static void handleActivationConfirm(ForgeActivationConfirmPayload payload) {
        VeinGlowClient.handleActivationConfirm(new ActivationConfirmData(payload.allowContinuous()));
    }

    public static void handleLookedAtBlock(ForgeLookedAtBlockPayload payload) {
        VeinGlowClient.handleLookedAtBlock(new LookedAtBlockData(payload.pos()));
    }

    public static void handleFilterResult(ForgeFilterResultPayload payload) {
        VeinGlowClient.handleFilterResult(new FilterResultData(payload.allowHighlight()));
    }

    public static void handleHighlightBlockList(ForgeHighlightBlockListPayload payload) {
        VeinGlowClient.handleHighlightBlockList(new HighlightBlockListData(
                payload.blocks(), payload.highlightStyle(), payload.source()));
    }

    public static void handleHighlightDelta(ForgeHighlightDeltaPayload payload) {
        VeinGlowClient.handleHighlightDelta(new HighlightDeltaData(
                payload.addedBlocks(), payload.removedBlocks(), payload.highlightStyle(), payload.source()));
    }
}
