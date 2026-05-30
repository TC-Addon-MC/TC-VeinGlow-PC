package com.tcveinminer.forge.client;

import com.tcveinminer.client.VeinGlowClient;
import com.tcveinminer.client.network.ClientNetworkManager;
import com.tcveinminer.forge.client.network.ForgeClientPacketChannel;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "tc_veinminer", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TCVeinMinerForgeClient {

    public static KeyMapping KEY_MINE;
    public static KeyMapping KEY_MENU;
    public static KeyMapping KEY_NEXT_SHAPE;
    public static KeyMapping KEY_PREV_SHAPE;
    public static KeyMapping KEY_QUICK_CYCLE;
    public static final KeyMapping[] KEY_QUICK_SELECT = new KeyMapping[9];

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ForgeClientPacketChannel channel = new ForgeClientPacketChannel();
        ClientNetworkManager.setChannel(channel);
        VeinGlowClient.init();

        MinecraftForge.EVENT_BUS.addListener(TCVeinMinerForgeClient::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(TCVeinMinerForgeClient::onClientPlayerLoggingIn);
        MinecraftForge.EVENT_BUS.addListener(TCVeinMinerForgeClient::onClientPlayerLoggingOut);
        MinecraftForge.EVENT_BUS.addListener(TCVeinMinerForgeClient::onRenderBlockHighlight);
        MinecraftForge.EVENT_BUS.addListener(TCVeinMinerForgeClient::onRenderLevelStage);

        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(
                net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> {
                    me.shedaniel.clothconfig2.api.ConfigBuilder builder = me.shedaniel.clothconfig2.api.ConfigBuilder
                            .create()
                            .setParentScreen(parent)
                            .setTitle(net.minecraft.network.chat.Component.translatable("title.tcveinminer.config"));
                    builder.setSavingRunnable(() -> {
                        com.tcveinminer.client.config.ClientConfigManager.save();
                        com.tcveinminer.config.ConfigManager.save();
                    });
                    me.shedaniel.clothconfig2.api.ConfigCategory general = builder.getOrCreateCategory(
                            net.minecraft.network.chat.Component.translatable("category.tcveinminer.general"));
                    me.shedaniel.clothconfig2.api.ConfigEntryBuilder entryBuilder = builder.entryBuilder();
                    general.addEntry(
                            entryBuilder
                                    .startBooleanToggle(
                                            net.minecraft.network.chat.Component
                                                    .translatable("option.tcveinminer.enable"),
                                            com.tcveinminer.config.ConfigManager.get().enabled)
                                    .setDefaultValue(true)
                                    .setSaveConsumer(
                                            newValue -> com.tcveinminer.config.ConfigManager.get().enabled = newValue)
                                    .build());
                    return builder.build();
                }));
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        KEY_MINE = new KeyMapping("key.tc_veinminer.mine", GLFW.GLFW_KEY_V, "key.categories.tc_veinminer");
        KEY_MENU = new KeyMapping("key.tc_veinminer.menu", GLFW.GLFW_KEY_G, "key.categories.tc_veinminer");
        KEY_NEXT_SHAPE = new KeyMapping("key.tc_veinminer.next_shape", GLFW.GLFW_KEY_RIGHT,
                "key.categories.tc_veinminer");
        KEY_PREV_SHAPE = new KeyMapping("key.tc_veinminer.prev_shape", GLFW.GLFW_KEY_LEFT,
                "key.categories.tc_veinminer");
        KEY_QUICK_CYCLE = new KeyMapping("key.tc_veinminer.quick_cycle", GLFW.GLFW_KEY_N,
                "key.categories.tc_veinminer");

        event.register(KEY_MINE);
        event.register(KEY_MENU);
        event.register(KEY_NEXT_SHAPE);
        event.register(KEY_PREV_SHAPE);
        event.register(KEY_QUICK_CYCLE);

        for (int i = 0; i < 9; i++) {
            KEY_QUICK_SELECT[i] = new KeyMapping("key.tc_veinminer.quick_select_" + (i + 1), GLFW.GLFW_KEY_1 + i,
                    "key.categories.tc_veinminer");
            event.register(KEY_QUICK_SELECT[i]);
        }
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        Minecraft client = Minecraft.getInstance();
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

        boolean menuKeyPressed = InputConstants.isKeyDown(client.getWindow().getWindow(),
                KEY_MENU.getDefaultKey().getValue());
        if (menuKeyPressed && client.screen == null && client.player != null) {
            com.tcveinminer.client.ClientApi.openRadialMenuRaw();
        }

        boolean mineKeyPressed = InputConstants.isKeyDown(client.getWindow().getWindow(),
                KEY_MINE.getDefaultKey().getValue());
        VeinGlowClient.onClientTick(mineKeyPressed, menuKeyPressed);
    }

    private static void onClientPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        VeinGlowClient.onJoin();
    }

    private static void onClientPlayerLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        VeinGlowClient.onDisconnect();
    }

    // ── Render highlight outline khi giữ phím V ──────────────────────────────
    private static void onRenderBlockHighlight(RenderHighlightEvent.Block event) {
        boolean keepDefault = com.tcveinminer.client.ClientApi.onDrawOutlineRaw(
                event.getPoseStack(),
                event.getCamera(),
                event.getMultiBufferSource());
        if (!keepDefault)
            event.setCanceled(true);
    }

    // ── Render fluid highlight (cầm Bucket) ──────────────────────────────────
    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
            return;
        com.tcveinminer.client.ClientApi.onDrawFluidHighlightRaw(
                event.getPoseStack(),
                event.getCamera(),
                net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource());
    }
}