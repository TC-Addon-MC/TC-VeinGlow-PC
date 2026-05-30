package com.tcveinminer.client.network;

import com.tcveinminer.network.payload.NetworkPacket;

/**
 * Platform-agnostic client network manager.
 * Sends packets to the server.
 */
public final class ClientNetworkManager {

    private static ClientPacketChannel channel;

    /**
     * Set the networking backend for the client.
     */
    public static void setChannel(ClientPacketChannel impl) {
        ClientNetworkManager.channel = impl;
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
