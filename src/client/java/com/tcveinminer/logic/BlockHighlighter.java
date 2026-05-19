package com.tcveinminer.logic;

import com.tcveinminer.TCVeinMinerClient;
import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.engine.strategy.MiningStrategy;
import com.tcveinminer.engine.strategy.StrategyRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.*;

/**
 * BlockHighlighter: render outline blocks sẽ bị đào.
 * Preview được throttle: chỉ re-scan khi target block thay đổi.
 */
public class BlockHighlighter {

    private static final RenderLayer LINES_NO_DEPTH = RenderLayer.of(
            "tc_veinminer_lines_no_depth",
            VertexFormats.LINES,
            VertexFormat.DrawMode.LINES,
            256, false, false,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.LINES_PROGRAM)
                    .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(2.5)))
                    .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .target(RenderPhase.ITEM_ENTITY_TARGET)
                    .writeMaskState(RenderPhase.COLOR_MASK)
                    .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .build(false)
    );

    // Throttle: cache kết quả preview
    private static BlockPos lastTarget = null;
    private static String lastStrategyId = null;
    private static Set<BlockPos> cachedHighlight = Collections.emptySet();

    public static void register() {
        WorldRenderEvents.BLOCK_OUTLINE.register(BlockHighlighter::onDrawOutline);
    }

    private static boolean onDrawOutline(WorldRenderContext context, WorldRenderContext.BlockOutlineContext outlineContext) {
        if (!TCVeinMinerClient.holdKeyDown) return true;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return true;

        Set<BlockPos> toHighlight = resolveHighlightSet(client);
        if (toHighlight.isEmpty()) return true;

        drawOutlines(context, client, toHighlight);
        return false;
    }

    private static Set<BlockPos> resolveHighlightSet(MinecraftClient client) {
        HitResult hit = client.crosshairTarget;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            lastTarget = null;
            return Collections.emptySet();
        }

        BlockPos targetPos = ((BlockHitResult) hit).getBlockPos();
        String strategyId = ConfigManager.get().miningShape.strategyId;

        // Cache: chỉ re-scan khi target hoặc strategy thay đổi
        if (targetPos.equals(lastTarget) && strategyId.equals(lastStrategyId)) {
            return cachedHighlight;
        }

        lastTarget = targetPos;
        lastStrategyId = strategyId;

        BlockState targetState = client.world.getBlockState(targetPos);
        if (targetState.isAir()) {
            cachedHighlight = Collections.emptySet();
            return cachedHighlight;
        }

        MiningStrategy strategy = StrategyRegistry.get(strategyId);
        List<BlockPos> preview = strategy.collectBlocks(
                client.world, targetPos, targetState,
                ConfigManager.get().maxBlocks - 1
        );

        cachedHighlight = new HashSet<>(preview);
        cachedHighlight.add(targetPos);
        return cachedHighlight;
    }

    private static void drawOutlines(WorldRenderContext context, MinecraftClient client,
                                     Set<BlockPos> blockSet) {
        Map<Long, EdgeData> edgeCount = new HashMap<>();
        for (BlockPos pos : blockSet) {
            Box box = client.world.getBlockState(pos).getOutlineShape(client.world, pos).getBoundingBox();
            addAllEdges(edgeCount, pos, box);
        }

        MatrixStack matrices = context.matrixStack();
        Vec3d cameraPos = context.camera().getPos();
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f mat = matrices.peek().getPositionMatrix();

        VertexConsumer solid = context.consumers().getBuffer(RenderLayer.getLines());
        VertexConsumer xray  = context.consumers().getBuffer(LINES_NO_DEPTH);

        for (EdgeData ed : edgeCount.values()) {
            if (ed.count != 1) continue;
            solid.vertex(mat, ed.ax, ed.ay, ed.az).color(0f, 1f, 1f, 1f).normal(ed.nx, ed.ny, ed.nz);
            solid.vertex(mat, ed.bx, ed.by, ed.bz).color(0f, 1f, 1f, 1f).normal(ed.nx, ed.ny, ed.nz);
            xray.vertex(mat, ed.ax, ed.ay, ed.az).color(0f, 1f, 1f, 0.4f).normal(ed.nx, ed.ny, ed.nz);
            xray.vertex(mat, ed.bx, ed.by, ed.bz).color(0f, 1f, 1f, 0.4f).normal(ed.nx, ed.ny, ed.nz);
        }
        matrices.pop();
    }

    private static void addAllEdges(Map<Long, EdgeData> edgeCount, BlockPos pos, Box box) {
        int bx2 = pos.getX()*2, by2 = pos.getY()*2, bz2 = pos.getZ()*2;
        int x0 = bx2+(int)(box.minX*2), x1 = bx2+(int)(box.maxX*2);
        int y0 = by2+(int)(box.minY*2), y1 = by2+(int)(box.maxY*2);
        int z0 = bz2+(int)(box.minZ*2), z1 = bz2+(int)(box.maxZ*2);
        addEdge(edgeCount, pos, box, x0,y1,z0, x1,y1,z0);
        addEdge(edgeCount, pos, box, x0,y1,z1, x1,y1,z1);
        addEdge(edgeCount, pos, box, x0,y0,z0, x1,y0,z0);
        addEdge(edgeCount, pos, box, x0,y0,z1, x1,y0,z1);
        addEdge(edgeCount, pos, box, x0,y0,z0, x0,y1,z0);
        addEdge(edgeCount, pos, box, x1,y0,z0, x1,y1,z0);
        addEdge(edgeCount, pos, box, x0,y0,z1, x0,y1,z1);
        addEdge(edgeCount, pos, box, x1,y0,z1, x1,y1,z1);
        addEdge(edgeCount, pos, box, x0,y1,z0, x0,y1,z1);
        addEdge(edgeCount, pos, box, x1,y1,z0, x1,y1,z1);
        addEdge(edgeCount, pos, box, x0,y0,z0, x0,y0,z1);
        addEdge(edgeCount, pos, box, x1,y0,z0, x1,y0,z1);
    }

    private static void addEdge(Map<Long, EdgeData> map, BlockPos pos, Box box,
                                 int ax2, int ay2, int az2, int bx2, int by2, int bz2) {
        long key = edgeKey(ax2,ay2,az2,bx2,by2,bz2);
        EdgeData ed = map.get(key);
        if (ed == null) {
            float ax=ax2/2f, ay=ay2/2f, az=az2/2f;
            float bx=bx2/2f, by=by2/2f, bz=bz2/2f;
            float cx=pos.getX()+(float)(box.minX+box.maxX)/2f;
            float cy=pos.getY()+(float)(box.minY+box.maxY)/2f;
            float cz=pos.getZ()+(float)(box.minZ+box.maxZ)/2f;
            float mx=(ax+bx)/2f, my=(ay+by)/2f, mz=(az+bz)/2f;
            float nx=mx-cx, ny=my-cy, nz=mz-cz;
            float len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
            if (len>0){nx/=len;ny/=len;nz/=len;}
            map.put(key, new EdgeData(ax,ay,az,bx,by,bz,nx,ny,nz));
        } else {
            ed.count++;
        }
    }

    private static long edgeKey(int ax2,int ay2,int az2,int bx2,int by2,int bz2){
        long pa=packPoint(ax2,ay2,az2), pb=packPoint(bx2,by2,bz2);
        if(pa>pb){long t=pa;pa=pb;pb=t;}
        return pa*1_000_000_007L+pb;
    }
    private static long packPoint(int x2,int y2,int z2){
        return ((long)(x2+4096)*8193L+(y2+4096))*8193L+(z2+4096);
    }

    private static class EdgeData {
        float ax,ay,az,bx,by,bz,nx,ny,nz; int count=1;
        EdgeData(float ax,float ay,float az,float bx,float by,float bz,float nx,float ny,float nz){
            this.ax=ax;this.ay=ay;this.az=az;this.bx=bx;this.by=by;this.bz=bz;
            this.nx=nx;this.ny=ny;this.nz=nz;
        }
    }
}
