// File 2: RenderMesh.java
package com.tcveinminer.gui.screens.custom;

import java.util.ArrayList;
import java.util.List;

public class RenderMesh {
    public final List<int[]> voxels = new ArrayList<>();   // solid volume blocks [x,y,z]
    public final List<float[]> points = new ArrayList<>(); // surface/line sample points [x,y,z]
    public final List<int[]> lines = new ArrayList<>();    // explicit line segments [x1,y1,z1,x2,y2,z2]

    public void addVoxel(int x, int y, int z) { voxels.add(new int[]{x, y, z}); }
    public void addPoint(float x, float y, float z) { points.add(new float[]{x, y, z}); }
    public void addLine(int x1, int y1, int z1, int x2, int y2, int z2) { lines.add(new int[]{x1, y1, z1, x2, y2, z2}); }

    public boolean isEmpty() { return voxels.isEmpty() && points.isEmpty() && lines.isEmpty(); }
    public int size() { return voxels.size() + points.size() + lines.size(); }
}