package com.tcveinminer.network;

import com.tcveinminer.network.payload.NetworkPacket;

/**
 * Central network dispatcher for the common module.
 * <p>
 * The engine calls {@code NetworkManager.sendToPlayer(player, packet)} instead of
 * calling {@code ServerPlayNetworking}, {@code PacketDistributor}, or {@code SimpleChannel} directly.
 * <p>
 * The loader module calls {@link #setChannel(PacketChannel)} during initialization to inject
 * the correct backend. This happens before any game events fire, so the channel is always
 * set by the time the engine needs it.
 *
 * <h3>Thread safety</h3>
 * {@link #setChannel} is called once on the main thread during mod init.
 * {@link #sendToPlayer} and {@link #sendToAll} are called from the server tick thread.
 * No synchronization is needed because initialization always completes before game ticks run.
 */
public final class NetworkManager {

    private static PacketChannel channel;

    /**
     * Called by the loader module during initialization to set the networking backend.
     * Must be called before any world loads.
     */
    public static void setChannel(PacketChannel impl) {
        if (channel != null) {
            throw new IllegalStateException(
                    "[TC VeinGlow] NetworkManager.setChannel() called more than once. " +
                    "Only one loader module should be present at runtime."
            );
        }
        channel = impl;
    }

    /**
     * Send a packet from the server to a specific player.
     *
     * @throws IllegalStateException if {@link #setChannel} was not called yet.
     */
    public static void sendToPlayer(Object player, NetworkPacket packet) {
        requireChannel();
        channel.sendToPlayer(player, packet);
    }

    /**
     * Send a packet to all players in the given collection.
     */
    public static void sendToAll(NetworkPacket packet, Iterable<?> players) {
        requireChannel();
        channel.sendToAll(packet, players);
    }

    /**
     * Check whether a specific player's client has the mod installed.
     */
    public static boolean canSendToPlayer(Object player) {
        requireChannel();
        return channel.canSendToPlayer(player);
    }

    private static void requireChannel() {
        if (channel == null) {
            throw new IllegalStateException(
                    "[TC VeinGlow] NetworkManager is not initialized. " +
                    "Make sure the loader module calls NetworkManager.setChannel() during mod init."
            );
        }
    }

    private NetworkManager() {}
}
