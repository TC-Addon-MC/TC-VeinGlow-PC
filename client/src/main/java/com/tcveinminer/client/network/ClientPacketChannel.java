package com.tcveinminer.client.network;

import com.tcveinminer.network.payload.NetworkPacket;

/**
 * Platform-specific client packet channel abstraction.
 */
public interface ClientPacketChannel {
    
    /** Send a packet to the server */
    void sendToServer(NetworkPacket packet);

    /** Check if a packet can be sent */
    boolean canSend(Class<? extends NetworkPacket> packetClass);

    /** Register client-side packet receivers */
    void registerReceivers();
}
