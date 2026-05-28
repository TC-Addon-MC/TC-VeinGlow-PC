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
    public boolean enableBucketSkill;
    public boolean enableCropHarvestSkill;
    public boolean enableTreeCapitatorSkill;
    public boolean enableInteractSkill;
    public boolean enableBreakSkill;
    public boolean enableToolSwapSkill;
    public boolean enableToolProtectSkill;
    public int toolProtectThreshold = 10;
    public boolean showHud;
    public boolean showOutline;
    public boolean preventMiningNearFluids;
    public float outlineThickness;
    public Set<String> enabledShapes = new LinkedHashSet<>();
    public Set<Identifier> blacklist = new LinkedHashSet<>();
    public int colorR, colorG, colorB;
    public int outlineAlpha = 204; // 0-255, mặc định ~80%
    public boolean colorRainbow, colorDisabled;

    /**
     * Danh sách màu cho chế độ multi-color outline.
     * Mỗi phần tử là int[3] = {R, G, B}.
     * Empty = dùng single color (colorR/G/B).
     */
    public List<int[]> colorList = new ArrayList<>();

    /** Nếu true: các màu chạy động theo thời gian dọc theo viền. */
    public boolean enableFlowAnimation = true;
    public float segmentLength = 2.0f;
    public float flowSmoothness = 0.5f;
    public float colorTransitionTime = 1.0f;
    public String selectedShapeId;

    // ---- Từ ClientConfig ----
    public boolean requireCorrectTool;
    public String customShapeEquation;

    /** Danh sách custom shapes — đồng bộ với ClientConfig.customShapes */
    public List<ClientConfig.CustomShapeEntry> customShapes = new ArrayList<>();
}
