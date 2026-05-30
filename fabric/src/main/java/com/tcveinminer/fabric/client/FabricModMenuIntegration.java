package com.tcveinminer.fabric.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.tcveinminer.client.config.ClientConfigManager;
import com.tcveinminer.config.ConfigManager;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public class FabricModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.translatable("title.tcveinminer.config"));

            builder.setSavingRunnable(() -> {
                ClientConfigManager.save();
                ConfigManager.save();
            });

            ConfigCategory general = builder.getOrCreateCategory(Component.translatable("category.tcveinminer.general"));
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.tcveinminer.enable"), ConfigManager.get().enabled)
                    .setDefaultValue(true)
                    .setSaveConsumer(newValue -> ConfigManager.get().enabled = newValue)
                    .build());

            return builder.build();
        };
    }
}
