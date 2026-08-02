package com.tcveinminer.fabric.client;

import com.tcveinminer.client.VeinGlowClient;
import com.tcveinminer.client.logic.BlockHighlighter;
import com.tcveinminer.client.network.ClientNetworkManager;
import com.tcveinminer.fabric.client.network.FabricClientPacketChannel;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class TCVeinMinerFabricClient implements ClientModInitializer {

    public static KeyMapping KEY_MINE;
    public static KeyMapping KEY_MENU;
    public static KeyMapping KEY_NEXT_SHAPE;
    public static KeyMapping KEY_PREV_SHAPE;
    public static KeyMapping KEY_QUICK_CYCLE;
    public static final KeyMapping[] KEY_QUICK_SELECT = new KeyMapping[9];

    @Override
    public void onInitializeClient() {
        FabricClientPacketChannel channel = new FabricClientPacketChannel();
        ClientNetworkManager.setChannel(channel);
        channel.registerReceivers();

        VeinGlowClient.init();

        KEY_MINE = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.tc_veinminer.mine", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.tc_veinminer"));
        KEY_MENU = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.tc_veinminer.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.tc_veinminer"));
        VeinGlowClient.KEY_MENU = KEY_MENU;
        KEY_NEXT_SHAPE = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.tc_veinminer.next_shape", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT,
                "key.categories.tc_veinminer"));
        KEY_PREV_SHAPE = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.tc_veinminer.prev_shape", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT,
                "key.categories.tc_veinminer"));
        KEY_QUICK_CYCLE = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.tc_veinminer.quick_cycle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.categories.tc_veinminer"));

        for (int i = 0; i < 9; i++) {
            KEY_QUICK_SELECT[i] = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                    "key.tc_veinminer.quick_select_" + (i + 1), InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_1 + i,
                    "key.categories.tc_veinminer"));
        }

        HudRenderCallback.EVENT.register((ctx, tick) -> {
            new com.tcveinminer.client.hud.VeinMinerHudOverlay().onHudRender(ctx, tick);
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> VeinGlowClient.onJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> VeinGlowClient.onDisconnect());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.screen == null) {
                while (KEY_NEXT_SHAPE.consumeClick())
                    VeinGlowClient.cycleShape(1);
                while (KEY_PREV_SHAPE.consumeClick())
                    VeinGlowClient.cycleShape(-1);
                while (KEY_QUICK_CYCLE.consumeClick())
                    VeinGlowClient.cycleShape(1);
                for (int i = 0; i < 9; i++) {
                    while (KEY_QUICK_SELECT[i].consumeClick())
                        VeinGlowClient.selectShapeByIndex(i);
                }
            }

            int menuKey = KeyBindingHelper.getBoundKeyOf(KEY_MENU).getValue();
            boolean menuKeyPressed = InputConstants.isKeyDown(client.getWindow().getWindow(), menuKey);

            if (menuKeyPressed && client.screen == null && client.player != null) {
                client.setScreen(new com.tcveinminer.client.gui.screens.RadialMenuScreen(null));
            }

            int mineKey = KeyBindingHelper.getBoundKeyOf(KEY_MINE).getValue();
            boolean mineKeyPressed = InputConstants.isKeyDown(client.getWindow().getWindow(), mineKey);

            VeinGlowClient.onClientTick(mineKeyPressed, menuKeyPressed);
        });

        // ── Render highlight outline khi giữ phím V ──────────────────────────
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, hit) -> {
            if (hit == null)
                return true;
            return BlockHighlighter.onDrawOutline(
                    context.matrixStack(),
                    context.camera(),
                    context.consumers());
        });

        // ── Render fluid highlight (cầm Bucket) ──────────────────────────────
        WorldRenderEvents.LAST.register(context -> {
            if (context.consumers() == null)
                return;
            BlockHighlighter.onDrawFluidHighlight(
                    context.matrixStack(),
                    context.camera(),
                    context.consumers());
        });
    }
}