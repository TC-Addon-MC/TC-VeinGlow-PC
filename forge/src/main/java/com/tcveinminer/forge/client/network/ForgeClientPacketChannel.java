package com.tcveinminer.forge.client.network;

import com.tcveinminer.client.network.ClientPacketChannel;
import com.tcveinminer.network.NetworkPacket;
import com.tcveinminer.forge.network.ForgePacketChannel;


public class ForgeClientPacketChannel implements ClientPacketChannel {

    @Override
    public void sendToServer(NetworkPacket packet) {
        net.minecraft.client.MinecraftClient.getInstance().getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket(ForgePacketChannel.toForgePayload(packet)));
    }

    @Override
    public boolean canSend(Class<? extends NetworkPacket> packetClass) {
        return true; 
    }

    @Override
    public void registerReceivers() {
        // Handled in ForgePacketChannel.registerPackets for Forge
    }
}
