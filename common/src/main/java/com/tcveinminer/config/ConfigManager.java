package com.tcveinminer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tcveinminer.platform.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;

/**
 * Manages loading and saving the server-side {@link ModConfig}.
 * <p>
 * Formerly used {@code FabricLoader.getInstance().getConfigDir()} directly.
 * Now delegates to {@link Services#PLATFORM()} so the path resolution
 * is loader-agnostic.
 */
public final class ConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("tc_veinminer");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ModConfig instance = new ModConfig();

    // Resolved lazily so Services is available when first called
    private static Path configPath() {
        return Services.PLATFORM().getConfigDir().resolve("tc_veinminer.json");
    }

    public static ModConfig get() {
        return instance;
    }

    public static void load() {
        Path path = configPath();
        if (!Files.exists(path)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
            if (loaded != null) {
                instance = loaded;

                // [FIX] Re-apply defaults for fields that Gson may zero-out when missing
                boolean needsSave = false;
                if (!instance.enableBreakSkill && !instance.enableInteractSkill) {
                    instance.enableBreakSkill = true;
                    instance.enableInteractSkill = true;
                    instance.enableCropHarvestSkill = true;
                    instance.enableTreeCapitatorSkill = true;
                    instance.enableBucketSkill = true;
                    needsSave = true;
                }

                // [AUTO-UPDATE VERSION]
                if (instance.version == null || !instance.version.equals(ModConfig.CURRENT_VERSION)) {
                    instance.version = ModConfig.CURRENT_VERSION;
                    needsSave = true;
                }

                if (needsSave) save();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load tc_veinminer config, using defaults", e);
        }
    }

    public static void save() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(instance, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save tc_veinminer config", e);
        }
    }

    private ConfigManager() {}
}
