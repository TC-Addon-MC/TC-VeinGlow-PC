// File: ShapeType.java
package com.tcveinminer.gui.screens.custom;

public enum ShapeType {
    FINITE_VOLUME("Khối hữu hạn"),
    SURFACE("Mặt"),
    LINE("Đường"),
    INFINITE("Vô hạn"),
    EMPTY("Rỗng");

    private final String displayName;
    ShapeType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}