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
        if (!Files.exists(PATH)) { INSTANCE = new ModConfig(); save(); return; }
        try (Reader r = Files.newBufferedReader(PATH)) {
            ModConfig c = GSON.fromJson(r, ModConfig.class);
            INSTANCE = c != null ? c : new ModConfig();
        } catch (Exception e) { INSTANCE = new ModConfig(); }
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(PATH)) { GSON.toJson(INSTANCE, w); }
        catch (Exception ignored) {}
    }

    public enum MiningShape {
        FACE        ("Standard (Face)",        "⬛", true),
        EDGES       ("Standard V2 (Edges)",    "🔷", true),
        CORNERS     ("Standard V3 (Corners)",  "💎", true),
        TALL_1x2    ("1×2 (Tall)",             "🧱", false),
        STAIR_UP    ("Stair Up",               "⬆", false),
        STAIR_DOWN  ("Stair Down",             "⬇", false),
        AREA_3x3    ("3×3 Area",               "🟦", false);

        public final String label;
        public final String icon;
        public final boolean enabledByDefault;

        MiningShape(String label, String icon, boolean enabledByDefault) {
            this.label = label;
            this.icon = icon;
            this.enabledByDefault = enabledByDefault;
        }
    }

    // Cài đặt chung
    public boolean enabled = true;
    public int     maxBlocks           = 64;
    public boolean requireSneak        = false;
    public boolean requireCorrectTool  = true;
    public boolean consumeDurability   = true;
    public boolean diagonalMining      = false;
    public int     cooldownTicks       = 0;
    public boolean showHud             = true;

    // Shape hiện tại đang dùng
    public MiningShape miningShape = MiningShape.FACE;

    // Các shape người dùng đã bật (hiện trong radial menu)
    public Set<String> enabledShapes = new LinkedHashSet<>(List.of("FACE", "EDGES", "CORNERS"));

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