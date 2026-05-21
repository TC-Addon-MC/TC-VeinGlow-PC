package com.tcveinminer.gui.screens;

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
    public Set<String> enabledShapes = new LinkedHashSet<>();
    public List<String> blacklist = new ArrayList<>();
    public Map<String, Boolean> enabledTools = new LinkedHashMap<>();
    public int colorR, colorG, colorB;
    public boolean colorRainbow, colorDisabled;
    public String hoveredShapeId;
}