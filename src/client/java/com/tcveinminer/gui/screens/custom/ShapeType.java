// File: ShapeType.java
package com.tcveinminer.gui.screens.custom;

import net.minecraft.text.Text;

public enum ShapeType {
    FINITE_VOLUME("gui.tcveinminer.shape.finite"),
    SURFACE("gui.tcveinminer.shape.surface"),
    LINE("gui.tcveinminer.shape.line"),
    INFINITE("gui.tcveinminer.shape.infinite"),
    EMPTY("gui.tcveinminer.shape.empty");

    private final String translationKey;
    ShapeType(String translationKey) { this.translationKey = translationKey; }
    public String getDisplayName() { return Text.translatable(translationKey).getString(); }
}