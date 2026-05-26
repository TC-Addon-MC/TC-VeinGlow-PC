package com.tcveinminer.logic;

import com.tcveinminer.TCVeinMinerClient;
import com.tcveinminer.config.ClientConfig;
import com.tcveinminer.config.ClientConfigManager;
import com.tcveinminer.engine.strategy.CustomEquationStrategy;
import com.tcveinminer.engine.strategy.FilterModeManager;
import com.tcveinminer.engine.strategy.MiningStrategy;
import com.tcveinminer.engine.strategy.StrategyRegistry;
import com.tcveinminer.engine.traversal.OrientationContext;
import com.tcveinminer.util.ExpressionEvaluator;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.*;

public class BlockHighlighter {

    // ── Render Layer Cache ────────────────────────────────────────────────────
    private static float lastThickness = -1f;
    private static RenderLayer cachedSolidLayer;
    private static RenderLayer cachedXrayLayer;

    /**
     * Khởi tạo lại RenderLayer CHỈ KHI config thickness thay đổi.
     * Cách này giúp dynamic thickness hoạt động mà không tốn per-frame performance.
     */
    private static void updateRenderLayers(float thickness) {
        if (thickness != lastThickness || cachedSolidLayer == null) {
            lastThickness = thickness;

            // Layer solid: Bị che bởi block (Depth test LEQUAL)
            cachedSolidLayer = RenderLayer.of("tc_veinminer_lines_solid", VertexFormats.LINES, VertexFormat.DrawMode.LINES, 256, false, false,
                    RenderLayer.MultiPhaseParameters.builder()
                            .program(RenderPhase.LINES_PROGRAM)
                            .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(thickness)))
                            .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                            .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                            .target(RenderPhase.MAIN_TARGET) // Fix lỗi clip sai
                            .writeMaskState(RenderPhase.COLOR_MASK)
                            .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                            .cull(RenderPhase.DISABLE_CULLING)
                            .build(false)
            );

            // Layer xray: Render xuyên block (Depth test ALWAYS)
            cachedXrayLayer = RenderLayer.of("tc_veinminer_lines_xray", VertexFormats.LINES, VertexFormat.DrawMode.LINES, 256, false, false,
                    RenderLayer.MultiPhaseParameters.builder()
                            .program(RenderPhase.LINES_PROGRAM)
                            .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(thickness)))
                            .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                            .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                            .target(RenderPhase.MAIN_TARGET) // Fix lỗi clip sai
                            .writeMaskState(RenderPhase.COLOR_MASK)
                            .depthTest(RenderPhase.ALWAYS_DEPTH_TEST) // Xuyên tường
                            .cull(RenderPhase.DISABLE_CULLING)
                            .build(false)
            );
        }
    }

    // ── Cache Logic ───────────────────────────────────────────────────────────
    private static final float YAW_BUCKET   = 5.0f;
    private static final float PITCH_BUCKET = 5.0f;

    private static BlockPos      lastTarget = null;
    private static String        lastStrategyId = null;
    private static Direction     lastHitFace = null;
    private static int           lastYawBucket = Integer.MIN_VALUE;
    private static int           lastPitchBucket = Integer.MIN_VALUE;
    private static Item          lastItem = null;
    private static int           lastCustomShapesHash = 0;

    private static Set<BlockPos> cachedHighlight = Collections.emptySet();
    private static boolean       isTargetInvalid = false;

    private static BlockPos lockedPreviewTarget = null;
    public static void register() {
        WorldRenderEvents.BLOCK_OUTLINE.register(BlockHighlighter::onDrawOutline);
    }

    private static boolean onDrawOutline(WorldRenderContext context, WorldRenderContext.BlockOutlineContext outlineCtx) {
        if (!TCVeinMinerClient.holdKeyDown) {
            lockedPreviewTarget = null;
            invalidateCache();
            return true;
        }
        if (!ClientConfigManager.instance.showOutline) return true;

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
            lockedPreviewTarget = null;
            invalidateCache();
            return Collections.emptySet();
        }

        BlockHitResult bhr = (BlockHitResult) hit;
        BlockPos targetPos = bhr.getBlockPos();
        lockedPreviewTarget = targetPos;
        String strategyId   = ClientConfigManager.instance.currentShape;

        PlayerEntity player = client.player;
        float pitch = player.getPitch();
        Direction hitFace = pitch > 60f  ? Direction.UP
                : pitch < -60f ? Direction.DOWN
                : OrientationContext.facingFromYaw(player.getYaw()).getOpposite();

        int yawBucket   = (int)(player.getYaw()   / YAW_BUCKET);
        int pitchBucket = (int)(player.getPitch()  / PITCH_BUCKET);
        Item currentItem = player.getMainHandStack().getItem();
        int customHash = customShapesHash();

        if (targetPos.equals(lastTarget)
                && strategyId.equals(lastStrategyId)
                && hitFace == lastHitFace
                && yawBucket   == lastYawBucket
                && pitchBucket == lastPitchBucket
                && currentItem == lastItem
                && customHash == lastCustomShapesHash) {
            return cachedHighlight;
        }

        lastTarget      = targetPos;
        lastStrategyId  = strategyId;
        lastHitFace     = hitFace;
        lastYawBucket   = yawBucket;
        lastPitchBucket = pitchBucket;
        lastItem        = currentItem;
        lastCustomShapesHash = customHash;

        BlockState targetState = client.world.getBlockState(targetPos);
        if (targetState.isAir()) {
            isTargetInvalid = false;
        cachedHighlight = Collections.emptySet();
            return cachedHighlight;
        }

        OrientationContext ctx = OrientationContext.of(
                hitFace,
                OrientationContext.facingFromYaw(player.getYaw())
        );

        MiningStrategy strategy = resolveStrategy(strategyId);
        int maxBlocks = ClientConfigManager.instance.getEffectiveMaxBlocks() - 1;

        // 1. Khởi tạo Cache và Filter cho Client Preview
        FilterModeManager.FilterCache cache = new FilterModeManager.FilterCache();
        FilterModeManager.BlockFilter filter;

        // Chọn bộ lọc an toàn cho preview phía Client tùy theo MiningMode
        switch (strategy.getModeType()) {
            case TREE_CAPITATOR -> filter = FilterModeManager.Presets.TREE_CAPITATOR(maxBlocks);
            case TUNNEL, SHAPE  -> filter = FilterModeManager.Composite.and(
                    FilterModeManager.Presets.BASE_SAFETY,
                    FilterModeManager.Filters.maxVisited(maxBlocks),
                    FilterModeManager.Filters.sameBlock(),
                    FilterModeManager.Filters.harvestableByTool()
            );
            default -> filter = FilterModeManager.Presets.VEIN_ORE(maxBlocks);
        }

        // 2. Đóng gói MiningRequest
        MiningStrategy.MiningRequest req = new MiningStrategy.MiningRequest(
                client.world, player, player.getMainHandStack(),
                targetPos, targetState, maxBlocks, ctx, filter, cache,
                new HashSet<>(ClientConfigManager.instance.personalBlacklist),
                ClientConfigManager.instance.requireCorrectTool
        );

        // 3. Lấy danh sách preview thông qua Filter mới
        List<BlockPos> preview = strategy.collectBlocks(req);

        // Kiểm tra xem block mục tiêu có thực sự đào được không
        FilterModeManager.FilterContext fCtxTarget = new FilterModeManager.FilterContext(
                client.world, player, player.getMainHandStack(), targetPos, targetPos,
                targetState, targetState, Direction.UP, 0, 0, 0, strategy.getModeType(), cache,
                new HashSet<>(ClientConfigManager.instance.personalBlacklist),
                ClientConfigManager.instance.requireCorrectTool
        );
        isTargetInvalid = !filter.test(fCtxTarget);

        if (isTargetInvalid) {
            cachedHighlight = Collections.singleton(targetPos);
        } else {
            cachedHighlight = new HashSet<>(preview);
            cachedHighlight.add(targetPos);
        }
        return cachedHighlight;
    }

    private static void invalidateCache() {
        lastTarget      = null;
        lastStrategyId  = null;
        lastHitFace     = null;
        lastYawBucket   = Integer.MIN_VALUE;
        lastPitchBucket = Integer.MIN_VALUE;
        lastItem        = null;
        lastCustomShapesHash = 0;
        isTargetInvalid = false;
        cachedHighlight = Collections.emptySet();
    }

    private static int customShapesHash() {
        int hash = 1;
        for (ClientConfig.CustomShapeEntry entry : ClientConfigManager.instance.customShapes) {
            hash = 31 * hash + Objects.hash(entry.strategyId, entry.name, entry.equation);
        }
        return hash;
    }

    private static MiningStrategy resolveStrategy(String strategyId) {
        if (strategyId != null && strategyId.startsWith("custom:")) {
            String equation = ClientConfigManager.instance.customShapes.stream()
                    .filter(entry -> strategyId.equals(entry.strategyId))
                    .map(entry -> entry.equation)
                    .findFirst()
                    .orElse("");
            ExpressionEvaluator evaluator = new ExpressionEvaluator(equation);
            if (evaluator.isValid()) {
                return new CustomEquationStrategy(strategyId, evaluator);
            }
            return StrategyRegistry.get("FACE");
        }
        return StrategyRegistry.get(strategyId);
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private static void drawOutlines(WorldRenderContext context, MinecraftClient client, Set<BlockPos> blockSet) {
        Map<Long, EdgeData> edgeCount = new HashMap<>();
        for (BlockPos pos : blockSet) {
            var shape = client.world.getBlockState(pos).getOutlineShape(client.world, pos);
            if (shape.isEmpty()) continue;
            Box box = shape.getBoundingBox();
            addAllEdges(edgeCount, pos, box);
        }

        ClientConfig cfg = ClientConfigManager.instance;

        // Đảm bảo layer render được cập nhật độ dày mới nhất
        updateRenderLayers(cfg.outlineThickness);

        // Đọc màu và tính toán Alpha
        float[] rgb = isTargetInvalid ? new float[]{0.937f, 0.267f, 0.267f} : resolveColor(cfg);
        float r = rgb[0], g = rgb[1], b = rgb[2];
        float alpha = Math.max(0f, Math.min(1f, cfg.outlineAlpha));
        float alphaXray = alpha * 0.25f; // Giảm alpha của Xray xuống để nhìn có chiều sâu hơn

        MatrixStack matrices = context.matrixStack();
        Vec3d cameraPos = context.camera().getPos();

        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f mat = matrices.peek().getPositionMatrix();

        VertexConsumer solid = context.consumers().getBuffer(cachedSolidLayer);
        VertexConsumer xray  = context.consumers().getBuffer(cachedXrayLayer);

        for (EdgeData ed : edgeCount.values()) {
            if (ed.count != 1) continue;
            xray.vertex(mat, ed.ax, ed.ay, ed.az).color(r, g, b, alphaXray).normal(ed.nx, ed.ny, ed.nz);
            xray.vertex(mat, ed.bx, ed.by, ed.bz).color(r, g, b, alphaXray).normal(ed.nx, ed.ny, ed.nz);
            solid.vertex(mat, ed.ax, ed.ay, ed.az).color(r, g, b, alpha).normal(ed.nx, ed.ny, ed.nz);
            solid.vertex(mat, ed.bx, ed.by, ed.bz).color(r, g, b, alpha).normal(ed.nx, ed.ny, ed.nz);
        }

        if (isTargetInvalid) {
            for (BlockPos pos : blockSet) {
                var shape = client.world.getBlockState(pos).getOutlineShape(client.world, pos);
                if (shape.isEmpty()) continue;
                Box box = shape.getBoundingBox();
                float x0 = (float)(pos.getX() + box.minX), x1 = (float)(pos.getX() + box.maxX);
                float y0 = (float)(pos.getY() + box.minY), y1 = (float)(pos.getY() + box.maxY);
                float z0 = (float)(pos.getZ() + box.minZ), z1 = (float)(pos.getZ() + box.maxZ);
                
                // Vẽ thanh chéo trên các mặt (Dựa theo bounding box)
                // Mặt trên
                xray.vertex(mat, x0, y1, z0).color(r, g, b, alphaXray).normal(0,1,0);
                xray.vertex(mat, x1, y1, z1).color(r, g, b, alphaXray).normal(0,1,0);
                solid.vertex(mat, x0, y1, z0).color(r, g, b, alpha).normal(0,1,0);
                solid.vertex(mat, x1, y1, z1).color(r, g, b, alpha).normal(0,1,0);
                // Các mặt khác nếu cần, nhưng thường mặt trên là đủ nhận diện
            }
        }
        matrices.pop();
    }

    // ── Color Utilities ───────────────────────────────────────────────────────

    private static float[] resolveColor(ClientConfig cfg) {
        if (cfg.colorDisabled) return new float[]{0.5f, 0.5f, 0.5f};

        if (cfg.colorRainbow) {
            float hue = (System.currentTimeMillis() % 4000) / 4000f; // Chỉnh nhẹ chu kỳ mượt hơn
            return hsvToRgb(hue, 1f, 1f);
        }

        return new float[]{cfg.colorR / 255f, cfg.colorG / 255f, cfg.colorB / 255f};
    }

    private static float[] hsvToRgb(float h, float s, float v) {
        int i = (int)(h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        return switch (i % 6) {
            case 0 -> new float[]{v, t, p};
            case 1 -> new float[]{q, v, p};
            case 2 -> new float[]{p, v, t};
            case 3 -> new float[]{p, q, v};
            case 4 -> new float[]{t, p, v};
            default -> new float[]{v, p, q};
        };
    }

    // ── Edge Merging Logic ────────────────────────────────────────────────────

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
            if(len>0){nx/=len;ny/=len;nz/=len;}
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
        EdgeData(float ax,float ay,float az,float bx,float by,float bz,
                 float nx,float ny,float nz){
            this.ax=ax;this.ay=ay;this.az=az;
            this.bx=bx;this.by=by;this.bz=bz;
            this.nx=nx;this.ny=ny;this.nz=nz;
        }
    }
}
