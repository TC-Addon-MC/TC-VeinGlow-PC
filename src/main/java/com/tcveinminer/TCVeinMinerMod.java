package com.tcveinminer;

import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.logic.VeinMinerLogic;
import com.tcveinminer.network.HoldKeyPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TCVeinMinerMod implements ModInitializer {
    // Lưu danh sách những ai đang đè phím V
    public static final Set<UUID> playersHoldingV = new HashSet<>();

    @Override
    public void onInitialize() {
        ConfigManager.load();

        // Đăng ký Packet
        PayloadTypeRegistry.playC2S().register(HoldKeyPayload.ID, HoldKeyPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(HoldKeyPayload.ID, (payload, context) -> {
            UUID uuid = context.player().getUuid();
            if (payload.isHolding()) playersHoldingV.add(uuid);
            else playersHoldingV.remove(uuid);
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, be) ->
                VeinMinerLogic.onBreak(player, world, pos, state));
    }
}