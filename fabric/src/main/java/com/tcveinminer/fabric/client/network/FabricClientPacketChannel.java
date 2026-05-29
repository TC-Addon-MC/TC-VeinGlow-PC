package com.tcveinminer.fabric.client.network;

import com.tcveinminer.client.VeinGlowClient;
import com.tcveinminer.client.network.ClientPacketChannel;
import com.tcveinminer.network.NetworkPacket;
import com.tcveinminer.network.payload.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class FabricClientPacketChannel implements ClientPacketChannel {

    @Override
    public void sendToServer(NetworkPacket packet) {
        ClientPlayNetworking.send((net.minecraft.network.packet.CustomPayload) packet);
    }

    @Override
    public boolean canSend(Class<? extends NetworkPacket> packetClass) {
        if (packetClass == HoldKeyData.class) return ClientPlayNetworking.canSend(HoldKeyData.ID);
        if (packetClass == ActivationRequestData.class) return ClientPlayNetworking.canSend(ActivationRequestData.ID);
        return false;
    }

    @Override
    public void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncData.ID, (payload, context) -> {
            context.client().execute(() -> VeinGlowClient.handleConfigSync(payload));
        });

        ClientPlayNetworking.registerGlobalReceiver(MiningStateData.ID, (payload, context) -> {
            context.client().execute(() -> VeinGlowClient.handleMiningState(payload));
        });

        ClientPlayNetworking.registerGlobalReceiver(ActivationConfirmData.ID, (payload, context) -> {
            context.client().execute(() -> VeinGlowClient.handleActivationConfirm(payload));
        });

        ClientPlayNetworking.registerGlobalReceiver(LookedAtBlockData.ID, (payload, context) -> {
            context.client().execute(() -> VeinGlowClient.handleLookedAtBlock(payload));
        });

        ClientPlayNetworking.registerGlobalReceiver(FilterResultData.ID, (payload, context) -> {
            context.client().execute(() -> VeinGlowClient.handleFilterResult(payload));
        });

        ClientPlayNetworking.registerGlobalReceiver(HighlightDeltaData.ID, (payload, context) -> {
            context.client().execute(() -> VeinGlowClient.handleHighlightDelta(payload));
        });

        ClientPlayNetworking.registerGlobalReceiver(HighlightBlockListData.ID, (payload, context) -> {
            context.client().execute(() -> VeinGlowClient.handleHighlightBlockList(payload));
        });
    }
}
