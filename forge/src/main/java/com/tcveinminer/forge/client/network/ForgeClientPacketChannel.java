package com.tcveinminer.forge.client.network;

import com.tcveinminer.client.network.ClientPacketChannel;
import com.tcveinminer.network.payload.NetworkPacket;
import com.tcveinminer.forge.network.ForgePacketChannel;

public class ForgeClientPacketChannel implements ClientPacketChannel {

    @Override
    public void sendToServer(NetworkPacket packet) {
        ForgePacketChannel.CHANNEL.send(ForgePacketChannel.toForgePayload(packet), net.minecraftforge.network.PacketDistributor.SERVER.noArg());
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
