package com.tcveinminer.logic;

import com.tcveinminer.TCVeinMinerClient;
import com.tcveinminer.config.ConfigManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class BlockHighlighter {

    public static void register() {
        WorldRenderEvents.BLOCK_OUTLINE.register(BlockHighlighter::onDrawOutline);
    }

    private static boolean onDrawOutline(WorldRenderContext context, WorldRenderContext.BlockOutlineContext outlineContext) {
        if (!TCVeinMinerClient.holdKeyDown) return true;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return true;

        HitResult hit = client.crosshairTarget;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return true;

        BlockPos targetPos = ((BlockHitResult) hit).getBlockPos();
        BlockState targetState = client.world.getBlockState(targetPos);

        List<BlockPos> blocksToMine = VeinMinerLogic.bfs(client.world, targetPos, targetState, ConfigManager.get());
        if (blocksToMine.isEmpty()) return true;

        MatrixStack matrices = context.matrixStack();
        VertexConsumer vertexConsumer = context.consumers().getBuffer(RenderLayer.getLines());
        Vec3d cameraPos = context.camera().getPos();

        float r = 1.0f, g = 0.2f, b = 0.2f, a = 0.8f;

        // Vẽ outline cho TẤT CẢ block trong vein, kể cả block đang nhìn
        for (BlockPos pos : blocksToMine) {
            matrices.push();
            matrices.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);

            Box box = client.world.getBlockState(pos).getOutlineShape(client.world, pos).getBoundingBox();

            net.minecraft.client.render.WorldRenderer.drawBox(
                    matrices, vertexConsumer,
                    box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ,
                    r, g, b, a
            );

            matrices.pop();
        }

        // Return false để suppress outline mặc định của Minecraft (tránh merge)
        return false;
    }
}
