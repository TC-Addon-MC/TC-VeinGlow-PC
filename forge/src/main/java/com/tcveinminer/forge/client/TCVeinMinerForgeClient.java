package com.tcveinminer.forge.client;

import com.tcveinminer.client.VeinGlowClient;
import com.tcveinminer.client.network.ClientNetworkManager;
import com.tcveinminer.forge.client.network.ForgeClientPacketChannel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "tc_veinminer", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TCVeinMinerForgeClient {

    public static KeyBinding KEY_MINE;
    public static KeyBinding KEY_MENU;
    public static KeyBinding KEY_NEXT_SHAPE;
    public static KeyBinding KEY_PREV_SHAPE;
    public static KeyBinding KEY_QUICK_CYCLE;
    public static final KeyBinding[] KEY_QUICK_SELECT = new KeyBinding[9];

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ForgeClientPacketChannel channel = new ForgeClientPacketChannel();
        ClientNetworkManager.setChannel(channel);
        VeinGlowClient.init();

        MinecraftForge.EVENT_BUS.addListener(TCVeinMinerForgeClient::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(TCVeinMinerForgeClient::onClientPlayerLoggingIn);
        MinecraftForge.EVENT_BUS.addListener(TCVeinMinerForgeClient::onClientPlayerLoggingOut);
        MinecraftForge.EVENT_BUS.addListener(TCVeinMinerForgeClient::onRenderGui);

        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> {
                    me.shedaniel.clothconfig2.api.ConfigBuilder builder = me.shedaniel.clothconfig2.api.ConfigBuilder.create()
                            .setParentScreen(parent)
                            .setTitle(net.minecraft.text.Text.translatable("title.tcveinminer.config"));
                    builder.setSavingRunnable(() -> {
                        com.tcveinminer.client.config.ClientConfigManager.save();
                        com.tcveinminer.config.ConfigManager.save();
                    });
                    me.shedaniel.clothconfig2.api.ConfigCategory general = builder.getOrCreateCategory(net.minecraft.text.Text.translatable("category.tcveinminer.general"));
                    me.shedaniel.clothconfig2.api.ConfigEntryBuilder entryBuilder = builder.entryBuilder();
                    general.addEntry(entryBuilder.startBooleanToggle(net.minecraft.text.Text.translatable("option.tcveinminer.enable"), com.tcveinminer.config.ConfigManager.get().enable)
                            .setDefaultValue(true)
                            .setSaveConsumer(newValue -> com.tcveinminer.config.ConfigManager.get().enable = newValue)
                            .build());
                    return builder.build();
                }));
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        KEY_MINE = new KeyBinding("key.tc_veinminer.mine", GLFW.GLFW_KEY_V, "key.categories.tc_veinminer");
        KEY_MENU = new KeyBinding("key.tc_veinminer.menu", GLFW.GLFW_KEY_G, "key.categories.tc_veinminer");
        KEY_NEXT_SHAPE = new KeyBinding("key.tc_veinminer.next_shape", GLFW.GLFW_KEY_RIGHT, "key.categories.tc_veinminer");
        KEY_PREV_SHAPE = new KeyBinding("key.tc_veinminer.prev_shape", GLFW.GLFW_KEY_LEFT, "key.categories.tc_veinminer");
        KEY_QUICK_CYCLE = new KeyBinding("key.tc_veinminer.quick_cycle", GLFW.GLFW_KEY_N, "key.categories.tc_veinminer");

        event.register(KEY_MINE);
        event.register(KEY_MENU);
        event.register(KEY_NEXT_SHAPE);
        event.register(KEY_PREV_SHAPE);
        event.register(KEY_QUICK_CYCLE);

        for (int i = 0; i < 9; i++) {
            KEY_QUICK_SELECT[i] = new KeyBinding("key.tc_veinminer.quick_select_" + (i + 1), GLFW.GLFW_KEY_1 + i, "key.categories.tc_veinminer");
            event.register(KEY_QUICK_SELECT[i]);
        }
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.currentScreen == null) {
            while (KEY_NEXT_SHAPE.wasPressed()) VeinGlowClient.cycleShape(client, 1);
            while (KEY_PREV_SHAPE.wasPressed()) VeinGlowClient.cycleShape(client, -1);
            while (KEY_QUICK_CYCLE.wasPressed()) VeinGlowClient.cycleShape(client, 1);
            for (int i = 0; i < 9; i++) {
                while (KEY_QUICK_SELECT[i].wasPressed()) VeinGlowClient.selectShapeByIndex(client, i);
            }
        }

        boolean menuKeyPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), KEY_MENU.getDefaultKey().getCode());
        if (menuKeyPressed && client.currentScreen == null && client.player != null) {
            client.setScreen(new com.tcveinminer.client.gui.screens.RadialMenuScreen(null));
        }

        boolean mineKeyPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), KEY_MINE.getDefaultKey().getCode());
        VeinGlowClient.onClientTick(client, mineKeyPressed, menuKeyPressed);
    }

    private static void onClientPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        VeinGlowClient.onJoin();
    }

    private static void onClientPlayerLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        VeinGlowClient.onDisconnect();
    }

    private static void onRenderGui(RenderGuiEvent.Post event) {
        new com.tcveinminer.client.hud.VeinMinerHudOverlay().onHudRender(event.getGuiGraphics(), event.getPartialTick());
    }
}
