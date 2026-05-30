package com.tcveinminer.forge.client.network;

import com.tcveinminer.client.network.ClientPacketChannel;
import com.tcveinminer.network.payload.NetworkPacket;
import com.tcveinminer.forge.network.ForgePacketChannel;
import net.minecraftforge.network.PacketDistributor;

public class ForgeClientPacketChannel implements ClientPacketChannel {

    @Override
    public void sendToServer(NetworkPacket packet) {
        net.minecraft.client.Minecraft.getInstance().getConnection().send(new net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket(ForgePacketChannel.toForgePayload(packet)));
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
