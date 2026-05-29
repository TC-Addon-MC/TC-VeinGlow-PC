package com.tcveinminer.fabric.client;

import com.tcveinminer.client.VeinGlowClient;
import com.tcveinminer.client.network.ClientNetworkManager;
import com.tcveinminer.fabric.client.network.FabricClientPacketChannel;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class TCVeinMinerFabricClient implements ClientModInitializer {

    public static KeyBinding KEY_MINE;
    public static KeyBinding KEY_MENU;
    public static KeyBinding KEY_NEXT_SHAPE;
    public static KeyBinding KEY_PREV_SHAPE;
    public static KeyBinding KEY_QUICK_CYCLE;
    public static final KeyBinding[] KEY_QUICK_SELECT = new KeyBinding[9];

    @Override
    public void onInitializeClient() {
        FabricClientPacketChannel channel = new FabricClientPacketChannel();
        ClientNetworkManager.setChannel(channel);
        channel.registerReceivers();

        VeinGlowClient.init();

        KEY_MINE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tc_veinminer.mine", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.tc_veinminer"));
        KEY_MENU = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tc_veinminer.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.tc_veinminer"));
        KEY_NEXT_SHAPE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tc_veinminer.next_shape", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT, "key.categories.tc_veinminer"));
        KEY_PREV_SHAPE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tc_veinminer.prev_shape", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT, "key.categories.tc_veinminer"));
        KEY_QUICK_CYCLE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tc_veinminer.quick_cycle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.categories.tc_veinminer"));

        for (int i = 0; i < 9; i++) {
            KEY_QUICK_SELECT[i] = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.tc_veinminer.quick_select_" + (i + 1), InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_1 + i, "key.categories.tc_veinminer"));
        }

        HudRenderCallback.EVENT.register((ctx, tick) -> {
            new com.tcveinminer.client.hud.VeinMinerHudOverlay().onHudRender(ctx, tick);
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> VeinGlowClient.onJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> VeinGlowClient.onDisconnect());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.currentScreen == null) {
                while (KEY_NEXT_SHAPE.wasPressed()) VeinGlowClient.cycleShape(client, 1);
                while (KEY_PREV_SHAPE.wasPressed()) VeinGlowClient.cycleShape(client, -1);
                while (KEY_QUICK_CYCLE.wasPressed()) VeinGlowClient.cycleShape(client, 1);
                for (int i = 0; i < 9; i++) {
                    while (KEY_QUICK_SELECT[i].wasPressed()) VeinGlowClient.selectShapeByIndex(client, i);
                }
            }

            int menuKey = KeyBindingHelper.getBoundKeyOf(KEY_MENU).getCode();
            boolean menuKeyPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), menuKey);
            
            if (menuKeyPressed && client.currentScreen == null && client.player != null) {
                client.setScreen(new com.tcveinminer.client.gui.screens.RadialMenuScreen(null));
            }

            int mineKey = KeyBindingHelper.getBoundKeyOf(KEY_MINE).getCode();
            boolean mineKeyPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), mineKey);

            VeinGlowClient.onClientTick(client, mineKeyPressed, menuKeyPressed);
        });
    }
}
