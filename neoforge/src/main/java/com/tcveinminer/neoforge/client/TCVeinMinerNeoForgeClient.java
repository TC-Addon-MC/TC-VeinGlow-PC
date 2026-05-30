package com.tcveinminer.neoforge.client;

import com.tcveinminer.client.VeinGlowClient;
import com.tcveinminer.client.network.ClientNetworkManager;
import com.tcveinminer.neoforge.client.network.NeoForgeClientPacketChannel;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = "tc_veinminer", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TCVeinMinerNeoForgeClient {

    public static KeyMapping KEY_MINE;
    public static KeyMapping KEY_MENU;
    public static KeyMapping KEY_NEXT_SHAPE;
    public static KeyMapping KEY_PREV_SHAPE;
    public static KeyMapping KEY_QUICK_CYCLE;
    public static final KeyMapping[] KEY_QUICK_SELECT = new KeyMapping[9];

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        NeoForgeClientPacketChannel channel = new NeoForgeClientPacketChannel();
        ClientNetworkManager.setChannel(channel);
        VeinGlowClient.init();

        NeoForge.EVENT_BUS.addListener(TCVeinMinerNeoForgeClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(TCVeinMinerNeoForgeClient::onClientPlayerLoggingIn);
        NeoForge.EVENT_BUS.addListener(TCVeinMinerNeoForgeClient::onClientPlayerLoggingOut);
        NeoForge.EVENT_BUS.addListener(TCVeinMinerNeoForgeClient::onRenderGui);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        KEY_MINE = new KeyMapping("key.tc_veinminer.mine", GLFW.GLFW_KEY_V, "key.categories.tc_veinminer");
        KEY_MENU = new KeyMapping("key.tc_veinminer.menu", GLFW.GLFW_KEY_G, "key.categories.tc_veinminer");
        KEY_NEXT_SHAPE = new KeyMapping("key.tc_veinminer.next_shape", GLFW.GLFW_KEY_RIGHT, "key.categories.tc_veinminer");
        KEY_PREV_SHAPE = new KeyMapping("key.tc_veinminer.prev_shape", GLFW.GLFW_KEY_LEFT, "key.categories.tc_veinminer");
        KEY_QUICK_CYCLE = new KeyMapping("key.tc_veinminer.quick_cycle", GLFW.GLFW_KEY_N, "key.categories.tc_veinminer");

        event.register(KEY_MINE);
        event.register(KEY_MENU);
        event.register(KEY_NEXT_SHAPE);
        event.register(KEY_PREV_SHAPE);
        event.register(KEY_QUICK_CYCLE);

        for (int i = 0; i < 9; i++) {
            KEY_QUICK_SELECT[i] = new KeyMapping("key.tc_veinminer.quick_select_" + (i + 1), GLFW.GLFW_KEY_1 + i, "key.categories.tc_veinminer");
            event.register(KEY_QUICK_SELECT[i]);
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.screen == null) {
            while (KEY_NEXT_SHAPE.consumeClick()) VeinGlowClient.cycleShape(1);
            while (KEY_PREV_SHAPE.consumeClick()) VeinGlowClient.cycleShape(-1);
            while (KEY_QUICK_CYCLE.consumeClick()) VeinGlowClient.cycleShape(1);
            for (int i = 0; i < 9; i++) {
                while (KEY_QUICK_SELECT[i].consumeClick()) VeinGlowClient.selectShapeByIndex(i);
            }
        }

        boolean menuKeyPressed = InputConstants.isKeyDown(
                client.getWindow().getWindow(),
                KEY_MENU.getDefaultKey().getValue());
        if (menuKeyPressed && client.screen == null && client.player != null) {
            com.tcveinminer.client.ClientApi.openRadialMenuRaw();
        }

        boolean mineKeyPressed = InputConstants.isKeyDown(
                client.getWindow().getWindow(),
                KEY_MINE.getDefaultKey().getValue());
        VeinGlowClient.onClientTick(mineKeyPressed, menuKeyPressed);
    }

    private static void onClientPlayerLoggingIn(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) {
        VeinGlowClient.onJoin();
    }

    private static void onClientPlayerLoggingOut(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        VeinGlowClient.onDisconnect();
    }

    private static void onRenderGui(RenderGuiEvent.Post event) {
        // VeinMinerHudOverlay uses Yarn DrawContext internally (from common module).
        // NeoForge's GuiGraphics is the Mojang-mapped equivalent (remapped at runtime).
        // We call it via the remapped interface — this works because Architectury Loom
        // remaps common classes at compile time for NeoForge.
        com.tcveinminer.client.ClientApi.onHudRenderRaw(
                event.getGuiGraphics(),
                event.getPartialTick());
    }
}
