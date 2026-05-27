package com.tcveinminer.gui.screens;

import com.tcveinminer.config.ClientConfig;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MenuState {
    public int maxBlocks;
    public int activationMode;
    public boolean showHud;
    public boolean showOutline;
    public float outlineThickness;
    public Set<String> enabledShapes = new LinkedHashSet<>();
    public Set<Identifier> blacklist = new LinkedHashSet<>();
    public int colorR, colorG, colorB;
    public boolean colorRainbow, colorDisabled;
    public String selectedShapeId;

    // ---- Từ ClientConfig ----
    public boolean requireCorrectTool;
    public String customShapeEquation;

    /** Danh sách custom shapes — đồng bộ với ClientConfig.customShapes */
    public List<ClientConfig.CustomShapeEntry> customShapes = new ArrayList<>();
}
