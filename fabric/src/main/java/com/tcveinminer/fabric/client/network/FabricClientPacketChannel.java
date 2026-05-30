package com.tcveinminer.fabric.client.network;

import com.tcveinminer.client.VeinGlowClient;
import com.tcveinminer.client.network.ClientPacketChannel;
import com.tcveinminer.network.payload.NetworkPacket;
import com.tcveinminer.network.payload.*;
import com.tcveinminer.fabric.network.FabricPacketChannel.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;

public class FabricClientPacketChannel implements ClientPacketChannel {

    @Override
    public void sendToServer(NetworkPacket packet) {
        ClientPlayNetworking.send(toFabricPayload(packet));
    }

    @Override
    public boolean canSend(Class<? extends NetworkPacket> packetClass) {
        if (packetClass == HoldKeyData.class) return ClientPlayNetworking.canSend(FabricHoldKeyPayload.ID);
        if (packetClass == ActivationRequestData.class) return ClientPlayNetworking.canSend(FabricActivationRequestPayload.ID);
        return false;
    }

    @Override
    public void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(FabricConfigSyncPayload.ID, (payload, context) -> {
            context.client().execute(() -> VeinGlowClient.handleConfigSync(new ConfigSyncData(payload.maxBlocks(), payload.blacklistedBlocks())));
        });

        ClientPlayNetworking.registerGlobalReceiver(FabricMiningStatePayload.ID, (payload, context) -> {
            context.client().execute(() -> VeinGlowClient.handleMiningState(new MiningStateData(payload.state(), payload.broken(), payload.target())));
        });

        ClientPlayNetworking.registerGlobalReceiver(FabricActivationConfirmPayload.ID, (payload, context) -> {
            context.client().execute(() -> VeinGlowClient.handleActivationConfirm(new ActivationConfirmData(payload.allowContinuous())));
        });

        ClientPlayNetworking.registerGlobalReceiver(FabricLookedAtBlockPayload.ID, (payload, context) -> {
            context.client().execute(() -> VeinGlowClient.handleLookedAtBlock(new LookedAtBlockData(
                    payload.pos().map(pos -> pos.asLong())
            )));
        });

        ClientPlayNetworking.registerGlobalReceiver(FabricFilterResultPayload.ID, (payload, context) -> {
            context.client().execute(() -> VeinGlowClient.handleFilterResult(new FilterResultData(payload.allowHighlight())));
        });

        ClientPlayNetworking.registerGlobalReceiver(FabricHighlightDeltaPayload.ID, (payload, context) -> {
            context.client().execute(() -> VeinGlowClient.handleHighlightDelta(new HighlightDeltaData(
                    payload.addedBlocks().stream().map(pos -> pos.asLong()).toList(),
                    payload.removedBlocks().stream().map(pos -> pos.asLong()).toList(),
                    payload.highlightStyle(),
                    payload.source()
            )));
        });

        ClientPlayNetworking.registerGlobalReceiver(FabricHighlightBlockListPayload.ID, (payload, context) -> {
            context.client().execute(() -> VeinGlowClient.handleHighlightBlockList(new HighlightBlockListData(
                    payload.blocks().stream().map(pos -> pos.asLong()).toList(),
                    payload.highlightStyle(),
                    payload.source()
            )));
        });
    }

    private static net.minecraft.network.protocol.common.custom.CustomPacketPayload toFabricPayload(NetworkPacket packet) {
        return switch (packet) {
            case HoldKeyData d -> new FabricHoldKeyPayload(d.isHolding(), d.shapeId(), d.maxBlocks(), d.equation(), d.blacklist());
            case ActivationRequestData d -> new FabricActivationRequestPayload(
                    d.active(),
                    d.targetPos().map(pos -> BlockPos.of(pos))
            );
            default -> throw new IllegalArgumentException("Cannot send S2C packet from client");
        };
    }
}
