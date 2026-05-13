package com.tcveinminer.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ModConfig {
    private static ModConfig INSTANCE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("tc_veinminer.json");

    public static ModConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    public static void load() {
        if (!Files.exists(PATH)) {
            INSTANCE = new ModConfig();
            save();
            return;
        }
        try (Reader r = Files.newBufferedReader(PATH)) {
            ModConfig c = GSON.fromJson(r, ModConfig.class);
            INSTANCE = c != null ? c : new ModConfig();
        } catch (Exception e) {
            INSTANCE = new ModConfig();
        }
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(PATH)) {
            GSON.toJson(INSTANCE, w);
        } catch (Exception ignored) {}
    }

    // Định nghĩa các chế độ hình dạng đào
    public enum MiningShape {
        SAME_BLOCK,
        SAME_TAG,
        ALL
    }

    // Cài đặt chung
    public boolean enabled            = true;
    public int     maxBlocks          = 64;
    public boolean requireSneak       = false;
    public boolean requireCorrectTool = true;
    public boolean consumeDurability  = true;
    public boolean diagonalMining     = false;
    public int     cooldownTicks      = 0;

    // Đã sửa: Chuyển từ String sang MiningShape Enum
    public MiningShape miningShape    = MiningShape.SAME_BLOCK;

    public boolean showHud            = true;

    // Danh sách block và công cụ
    public Set<String> blacklistedBlocks = new HashSet<>();
    public Map<String, Boolean> enabledTools = new LinkedHashMap<>(Map.of(
            "pickaxe", true,
            "axe", false,
            "shovel", false,
            "sword", false,
            "hand", false,
            "hoe", false
    ));
}