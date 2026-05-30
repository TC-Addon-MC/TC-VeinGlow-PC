package com.tcveinminer.platform;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Platform abstraction layer.
 * <p>
 * Each loader module provides exactly one implementation, registered via
 * {@code META-INF/services/com.tcveinminer.platform.PlatformHelper}.
 * The correct implementation is loaded at runtime via {@link Services}.
 * <p>
 * The common module MUST NOT import Fabric API, NeoForge, or Forge classes.
 */
public interface PlatformHelper {

    enum Environment { CLIENT, SERVER }

    // ── Environment ───────────────────────────────────────────────────────

    Environment getEnvironment();

    default boolean isClientSide() { return getEnvironment() == Environment.CLIENT; }

    default boolean isServer() { return getEnvironment() == Environment.SERVER; }

    /** True only on a dedicated server (no integrated server / client). */
    boolean isDedicatedServer();

    // ── Platform Info ─────────────────────────────────────────────────────

    /** Returns "Fabric", "NeoForge", or "Forge". */
    String getPlatformName();

    boolean isModLoaded(String modId);

    String getModVersion(String modId);

    // ── Paths ─────────────────────────────────────────────────────────────

    Path getConfigDir();

    Path getGameDir();

    // ── Logging ───────────────────────────────────────────────────────────

    Logger getLogger(String name);

    // ── Player Utilities ──────────────────────────────────────────────────

    boolean isOperator(ServerPlayer player);
}
