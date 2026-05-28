package com.tcveinminer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;

public class ConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("tc_veinminer");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("tc_veinminer.json");

    private static ModConfig instance = new ModConfig();

    public static ModConfig get() {
        return instance;
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
            if (loaded != null) {
                instance = loaded;
                // [FIX] Cập nhật các giá trị mặc định cho version mới nếu bị Gson set false do thiếu field
                boolean needsSave = false;
                if (!instance.enableBreakSkill && !instance.enableInteractSkill) {
                    instance.enableBreakSkill = true;
                    instance.enableInteractSkill = true;
                    instance.enableCropHarvestSkill = true;
                    instance.enableTreeCapitatorSkill = true;
                    instance.enableBucketSkill = true;
                    needsSave = true;
                }
                if (needsSave) save();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load tc_veinminer config, using defaults", e);
        }
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(instance, writer);
        } catch (Exception e) {
            LOGGER.error("Failed to save tc_veinminer config", e);
        }
    }
}
