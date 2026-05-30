package com.tcveinminer.forge;

import com.tcveinminer.api.addon.AddonLoader;
import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.api.addon.AddonContextImpl;
import com.tcveinminer.forge.event.ForgeEventBridge;
import com.tcveinminer.forge.network.ForgePacketChannel;
import com.tcveinminer.forge.registry.ForgeCommandRegistrar;
import com.tcveinminer.network.NetworkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;

/**
 * Forge server-side mod entrypoint.
 * Wires platform, networking, events, commands, and addons via abstractions.
 */
@Mod("tc_veinminer")
public final class TCVeinMinerForge {

    public TCVeinMinerForge(IEventBus modBus, ModContainer modContainer) {
        // 1. Load config
        ConfigManager.load();

        // 2. Networking
        ForgePacketChannel channel = new ForgePacketChannel();
        NetworkManager.setChannel(channel);
        channel.registerPackets();

        // 3. Game event listeners on the FORGE game bus
        MinecraftForge.EVENT_BUS.register(new ForgeEventBridge.GameEventListener());

        // 4. Commands
        ForgeCommandRegistrar.register();

        // 5. Addons
        modBus.addListener((FMLCommonSetupEvent event) ->
                event.enqueueWork(() -> AddonLoader.loadAll(new AddonContextImpl()))
        );

        // 6. Config screen (Forge 52+ via IExtensionPoint on ModContainer)
        modBus.addListener((FMLClientSetupEvent event) -> {
            modContainer.registerExtensionPoint(
                    net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> {
                        me.shedaniel.clothconfig2.api.ConfigBuilder builder = me.shedaniel.clothconfig2.api.ConfigBuilder.create()
                                .setParentScreen(parent)
                                .setTitle(net.minecraft.network.chat.Component.translatable("title.tcveinminer.config"));
                        builder.setSavingRunnable(() -> {
                            com.tcveinminer.client.config.ClientConfigManager.save();
                            com.tcveinminer.config.ConfigManager.save();
                        });
                        me.shedaniel.clothconfig2.api.ConfigCategory general = builder.getOrCreateCategory(
                                net.minecraft.network.chat.Component.translatable("category.tcveinminer.general"));
                        me.shedaniel.clothconfig2.api.ConfigEntryBuilder entryBuilder = builder.entryBuilder();
                        general.addEntry(entryBuilder.startBooleanToggle(
                                        net.minecraft.network.chat.Component.translatable("option.tcveinminer.enable"),
                                        com.tcveinminer.config.ConfigManager.get().enabled)
                                .setDefaultValue(true)
                                .setSaveConsumer(newValue -> com.tcveinminer.config.ConfigManager.get().enabled = newValue)
                                .build());
                        return builder.build();
                    })
            );
        });
    }
}