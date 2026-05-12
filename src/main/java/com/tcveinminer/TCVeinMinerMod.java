package com.tcveinminer;

import com.tcveinminer.config.ModConfig;
import com.tcveinminer.logic.VeinMinerLogic;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

public class TCVeinMinerMod implements ModInitializer {

    @Override
    public void onInitialize() {
        ModConfig.load();
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, be) ->
            VeinMinerLogic.onBreak(player, world, pos, state));
    }
}
