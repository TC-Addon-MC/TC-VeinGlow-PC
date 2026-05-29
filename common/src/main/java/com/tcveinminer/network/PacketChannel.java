package com.tcveinminer.network;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Networking backend abstraction.
 * <p>
 * Each loader module provides one implementation:
 * <ul>
 *   <li>Fabric → {@code FabricPacketChannel} (uses {@code ServerPlayNetworking})</li>
 *   <li>NeoForge → {@code NeoForgePacketChannel} (uses {@code PacketDistributor})</li>
 *   <li>Forge → {@code ForgePacketChannel} (uses {@code SimpleChannel})</li>
 * </ul>
 * The common module MUST NOT call any of those directly.
 * All sending goes through {@link NetworkManager}.
 */
public interface PacketChannel {

    /**
     * Register all packet types with the loader's networking system.
     * Called once during mod initialization.
     */
    void registerServerPackets();

    /**
     * Send a packet from the server to a specific client player.
     *
     * @param player the recipient
     * @param packet the data packet (a {@link NetworkPacket} record)
     */
    void sendToPlayer(ServerPlayerEntity player, NetworkPacket packet);

    /**
     * Send a packet to all players in the given iterable.
     */
    void sendToAll(NetworkPacket packet, Iterable<ServerPlayerEntity> players);

    /**
     * Check whether the given player's connection supports receiving this channel's packets.
     * Returns true if the mod is installed on the client, false for vanilla clients.
     */
    boolean canSendToPlayer(ServerPlayerEntity player);
}
