package com.tcveinminer.neoforge.client.network;

import com.tcveinminer.client.network.ClientPacketChannel;
import com.tcveinminer.network.payload.NetworkPacket;
import com.tcveinminer.neoforge.network.NeoForgePacketChannel;
import net.neoforged.neoforge.network.PacketDistributor;

public class NeoForgeClientPacketChannel implements ClientPacketChannel {

    @Override
    public void sendToServer(NetworkPacket packet) {
        PacketDistributor.sendToServer(NeoForgePacketChannel.toNeoForgePayload(packet));
    }

    @Override
    public boolean canSend(Class<? extends NetworkPacket> packetClass) {
        // Assume NeoForge handles capabilities properly for registered payloads
        return true; 
    }

    @Override
    public void registerReceivers() {
        // Handled in NeoForgePacketChannel.registerPackets for NeoForge
    }
}

