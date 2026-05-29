package com.tcveinminer.network;

/**
 * Marker interface for all TC VeinGlow network packets.
 * <p>
 * Common data records implement this interface. Loader-specific wrapper classes
 * (e.g., {@code FabricConfigSyncPayload}) convert these records into the
 * loader's native packet format.
 * <p>
 * Using a sealed interface here allows exhaustive pattern matching in loader adapters.
 */
public sealed interface NetworkPacket
        permits
            com.tcveinminer.network.payload.HoldKeyData,
            com.tcveinminer.network.payload.ConfigSyncData,
            com.tcveinminer.network.payload.MiningStateData,
            com.tcveinminer.network.payload.ActivationRequestData,
            com.tcveinminer.network.payload.ActivationConfirmData,
            com.tcveinminer.network.payload.LookedAtBlockData,
            com.tcveinminer.network.payload.FilterResultData,
            com.tcveinminer.network.payload.HighlightBlockListData,
            com.tcveinminer.network.payload.HighlightDeltaData {

    /** Unique channel identifier, e.g. {@code "tc_veinminer:hold_key"}. */
    String channelId();
}
