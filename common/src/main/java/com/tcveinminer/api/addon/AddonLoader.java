package com.tcveinminer.api.addon;

import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Discovers and loads all {@link VeinMinerAddon} implementations via Java's ServiceLoader.
 * <p>
 * Addon JARs simply add a {@code META-INF/services/com.tcveinminer.api.addon.VeinMinerAddon}
 * file — no registration code needed on TC VeinGlow's side.
 */
public final class AddonLoader {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("tc_veinminer/addons");
    private static final List<VeinMinerAddon> LOADED = new ArrayList<>();
    public static final int CURRENT_API_VERSION = 1;

    /**
     * Discover all addons on the classpath and call {@link VeinMinerAddon#onAddonInit}.
     * Called once during mod initialization, after the platform is ready.
     */
    public static void loadAll(AddonContext context) {
        ServiceLoader<VeinMinerAddon> loader = ServiceLoader.load(VeinMinerAddon.class);
        for (VeinMinerAddon addon : loader) {
            if (addon.getRequiredApiVersion() > CURRENT_API_VERSION) {
                LOGGER.warn("Skipping addon '{}': requires API version {}, current is {}",
                        addon.getAddonId(), addon.getRequiredApiVersion(), CURRENT_API_VERSION);
                continue;
            }
            try {
                addon.onAddonInit(context);
                LOADED.add(addon);
                LOGGER.info("Loaded addon: {}", addon.getAddonId());
            } catch (Exception e) {
                LOGGER.error("Failed to initialize addon '{}', skipping: {}", addon.getAddonId(), e.getMessage(), e);
                // Faulting addon never crashes the host mod
            }
        }
        LOGGER.info("TC VeinGlow: {} addon(s) loaded.", LOADED.size());
    }

    /**
     * Called on server shutdown — allows addons to release resources.
     */
    public static void shutdownAll() {
        for (VeinMinerAddon addon : LOADED) {
            try {
                addon.onAddonShutdown();
            } catch (Exception e) {
                LOGGER.error("Error during addon '{}' shutdown: {}", addon.getAddonId(), e.getMessage(), e);
            }
        }
    }

    /** @return an unmodifiable view of all successfully loaded addons. */
    public static List<VeinMinerAddon> getLoaded() {
        return Collections.unmodifiableList(LOADED);
    }

    private AddonLoader() {}
}
