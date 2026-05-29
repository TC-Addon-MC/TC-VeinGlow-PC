package com.tcveinminer.client.network;

import com.tcveinminer.network.NetworkPacket;

/**
 * Platform-agnostic client network manager.
 * Sends packets to the server.
 */
public final class ClientNetworkManager {

    private static ClientPacketChannel channel;

    public static void setChannel(ClientPacketChannel channel) {
        ClientNetworkManager.channel = channel;
    }

    public static boolean canSend(Class<? extends NetworkPacket> packetClass) {
        if (channel == null) return false;
        return channel.canSend(packetClass);
    }

    public static void sendToServer(NetworkPacket packet) {
        if (channel != null) {
            channel.sendToServer(packet);
        }
    }

    private ClientNetworkManager() {}
}
