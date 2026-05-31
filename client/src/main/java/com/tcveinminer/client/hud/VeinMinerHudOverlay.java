package com.tcveinminer.client.hud;

import com.tcveinminer.client.VeinGlowClient;
import com.tcveinminer.client.config.ClientConfigManager;
import com.tcveinminer.client.hud.style.*;
import com.tcveinminer.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.network.chat.Component;

public class VeinMinerHudOverlay {

    private final IHudRenderer pillRenderer = new PillHudRenderer();
    private final IHudRenderer arcRenderer = new ArcHudRenderer();
    private final IHudRenderer minimalRenderer = new MinimalHudRenderer();
    private final IHudRenderer sideBadgeRenderer = new SideBadgeHudRenderer();
    private final IHudRenderer iconOnlyRenderer = new IconOnlyHudRenderer();
    private final IHudRenderer circleRenderer = new CircleHudRenderer();
    private final IHudRenderer orbitalRenderer = new OrbitalHudRenderer();
    private final IHudRenderer crosshairTagRenderer = new CrosshairTagHudRenderer();
    private final IHudRenderer actionBarRenderer = new ActionBarHudRenderer();
    private final IHudRenderer hotbarRenderer = new HotbarHudRenderer();
    private final IHudRenderer compassRenderer = new CompassHudRenderer();
    private final IHudRenderer progressBarRenderer = new ProgressBarHudRenderer();
    private final IHudRenderer cornerAccentRenderer = new CornerAccentHudRenderer();
    private final IHudRenderer chatLineRenderer = new ChatLineHudRenderer();
    private final IHudRenderer tooltipRenderer = new TooltipHudRenderer();

    public void onHudRender(GuiGraphics ctx, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.options == null)
            return;

        if (client.options.hideGui)
            return;
        if (client.screen != null)
            return;

        if (!ClientConfigManager.instance.showHud)
            return;

        boolean holding = VeinGlowClient.holdKeyDown;

        String currentShapeId = ClientConfigManager.instance.currentShape;
        String modeLabel = currentShapeId;
        try {
            ModConfig.MiningShape shape = ModConfig.MiningShape.valueOf(currentShapeId);
            modeLabel = Component.translatable("tc_veinminer.mode." + shape.name()).getString();
        } catch (IllegalArgumentException | NullPointerException ignored) {
            for (com.tcveinminer.client.config.ClientConfig.CustomShapeEntry entry : ClientConfigManager.instance.customShapes) {
                if (currentShapeId.equals(entry.strategyId)) {
                    modeLabel = entry.name;
                    break;
                }
            }
            if (modeLabel.equals(currentShapeId) && currentShapeId.startsWith("custom:")) {
                String rawName = currentShapeId.substring(7);
                String[] words = rawName.replace("_", " ").split(" ");
                StringBuilder sb = new StringBuilder();
                for (String w : words) {
                    if (!w.isEmpty()) {
                        sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
                    }
                }
                modeLabel = sb.toString().trim();
            }
        }

        int actMode = ClientConfigManager.instance.activationMode;
        String keyName = VeinGlowClient.KEY_MINE == null ? "V"
                : VeinGlowClient.KEY_MINE.getTranslatedKeyMessage().getString();

        String keyHint = switch (actMode) {
            case 2 -> Component.translatable("hud.tcveinminer.hint.hold_sneak", keyName).getString();
            case 3 -> Component.translatable("hud.tcveinminer.hint.toggle", keyName).getString();
            case 4 -> Component.translatable("hud.tcveinminer.hint.toggle_sneak", keyName).getString();
            default -> Component.translatable("hud.tcveinminer.hint.hold", keyName).getString();
        };

        HudAnchor anchor = ClientConfigManager.instance.hudAnchor;
        int customX = ClientConfigManager.instance.hudPositionX;
        int customY = ClientConfigManager.instance.hudPositionY;

        int w = ctx.guiWidth();
        int h = ctx.guiHeight();

        int x = 0;
        int y = 0;

        switch (anchor) {
            case TOP_LEFT -> {
                x = 10;
                y = 10;
            }
            case TOP_CENTER -> {
                x = w / 2;
                y = 10;
            }
            case TOP_RIGHT -> {
                x = w - 10;
                y = 10;
            }
            case MIDDLE_LEFT -> {
                x = 10;
                y = h / 2;
            }
            case MIDDLE_RIGHT -> {
                x = w - 10;
                y = h / 2;
            }
            case BOTTOM_LEFT -> {
                x = 10;
                y = h - 24;
            }
            case BOTTOM_RIGHT -> {
                x = w - 10;
                y = h - 24;
            }
            case BOTTOM_CENTER -> {
                x = w / 2;
                y = h - 24;
            }
            case CUSTOM -> {
                x = customX;
                y = customY;
            }
        }

        if (anchor != HudAnchor.CUSTOM) {
            x += customX;
            y += customY;
        }

        IHudRenderer renderer = switch (ClientConfigManager.instance.hudStyle) {
            case ARC -> arcRenderer;
            case CIRCLE -> circleRenderer;
            case ORBITAL -> orbitalRenderer;
            case CROSSHAIR_TAG -> crosshairTagRenderer;
            case ACTION_BAR -> actionBarRenderer;
            case HOTBAR -> hotbarRenderer;
            case COMPASS -> compassRenderer;
            case PROGRESS_BAR -> progressBarRenderer;
            case CORNER_ACCENT -> cornerAccentRenderer;
            case CHAT_LINE -> chatLineRenderer;
            case TOOLTIP -> tooltipRenderer;
            case MINIMAL -> minimalRenderer;
            case SIDE_BADGE -> sideBadgeRenderer;
            case ICON_ONLY -> iconOnlyRenderer;
            default -> pillRenderer;
        };

        renderer.render(ctx, x, y, anchor, VeinGlowClient.isMining, holding, modeLabel, keyHint);

        // Render toasts attached to HUD
        ToastRenderer.renderAttached(ctx, ClientConfigManager.instance.hudStyle);
    }
}
