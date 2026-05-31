package com.tcveinminer.client.gui.screens;

import com.tcveinminer.client.gui.widgets.AmberButton;
import com.tcveinminer.client.hud.style.*;
import com.tcveinminer.client.util.ThemeColors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.EnumSet;

public class HudConfigScreen extends Screen {

    private final MainMenuScreen parent;
    private final MenuState state;
    
    private final int W = 200;
    private int H = 220;
    private final int px = 8;
    private final int py = 8;

    private boolean isDraggingHud = false;
    private boolean isDraggingToast = false;

    // The set of styles that have fixed locked positions (no drag allowed)
    private static final EnumSet<HudStyle> LOCKED_STYLES = EnumSet.of(
            HudStyle.ARC, HudStyle.CIRCLE, HudStyle.ORBITAL, HudStyle.CROSSHAIR_TAG,
            HudStyle.ACTION_BAR, HudStyle.HOTBAR, HudStyle.COMPASS, HudStyle.PROGRESS_BAR,
            HudStyle.CHAT_LINE, HudStyle.TOOLTIP
    );

    // Renderers
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

    public HudConfigScreen(MainMenuScreen parent) {
        super(Component.translatable("gui.tcveinminer.hudconfig.title"));
        this.parent = parent;
        this.state = parent.getState();
    }

    private String formatEnumName(String name) {
        String[] words = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    @Override
    protected void init() {
        int cx = px + 10;
        int cy = py + 30;

        addRenderableWidget(new AmberButton(cx, cy, 180, 20, Component.translatable("gui.tcveinminer.hudconfig.style").append(formatEnumName(state.hudStyle.name())), btn -> {
            HudStyle[] styles = HudStyle.values();
            state.hudStyle = styles[(state.hudStyle.ordinal() + 1) % styles.length];
            btn.setMessage(Component.translatable("gui.tcveinminer.hudconfig.style").append(formatEnumName(state.hudStyle.name())));
        }));
        
        cy += 24;
        cy += 16; // space for scale label
        
        // Scale [-] and [+]
        addRenderableWidget(new AmberButton(cx, cy, 20, 20, Component.literal("-"), btn -> {
            state.hudScale = Mth.clamp(state.hudScale - 0.25f, 0.5f, 2.0f);
        }));
        addRenderableWidget(new AmberButton(cx + 160, cy, 20, 20, Component.literal("+"), btn -> {
            state.hudScale = Mth.clamp(state.hudScale + 0.25f, 0.5f, 2.0f);
        }));

        cy += 24;
        cy += 16; // space for opacity label

        // Opacity [-] and [+]
        addRenderableWidget(new AmberButton(cx, cy, 20, 20, Component.literal("-"), btn -> {
            state.hudOpacity = Mth.clamp(state.hudOpacity - 0.1f, 0.1f, 1.0f);
        }));
        addRenderableWidget(new AmberButton(cx + 160, cy, 20, 20, Component.literal("+"), btn -> {
            state.hudOpacity = Mth.clamp(state.hudOpacity + 0.1f, 0.1f, 1.0f);
        }));

        cy += 24;

        addRenderableWidget(new AmberButton(cx, cy, 180, 20, Component.translatable("gui.tcveinminer.button.reset_defaults"), btn -> {
            state.hudScale = 1.0f;
            state.hudOpacity = 1.0f;
            state.hudPositionX = 30;
            state.hudPositionY = 30;
            state.hudAnchor = HudAnchor.TOP_LEFT;
            state.toastPositionX = 0;
            state.toastPositionY = -40;
            state.toastAnchor = HudAnchor.BOTTOM_CENTER;
        }));

        cy += 24;
        
        addRenderableWidget(new AmberButton(cx, cy, 180, 20, Component.literal("Toast Notification: " + (state.showToast ? "ON" : "OFF")), btn -> {
            state.showToast = !state.showToast;
            btn.setMessage(Component.literal("Toast Notification: " + (state.showToast ? "ON" : "OFF")));
        }));

        cy += 24;

        addRenderableWidget(new AmberButton(cx, cy, 180, 20, Component.translatable("gui.tcveinminer.button.close"), btn -> {
            saveAndClose();
        }));
        
        H = cy + 28 - py;
    }
    
    private void saveAndClose() {
        com.tcveinminer.client.config.ClientConfigManager.instance.hudStyle = state.hudStyle;
        com.tcveinminer.client.config.ClientConfigManager.instance.hudScale = state.hudScale;
        com.tcveinminer.client.config.ClientConfigManager.instance.hudOpacity = state.hudOpacity;
        com.tcveinminer.client.config.ClientConfigManager.instance.hudPositionX = state.hudPositionX;
        com.tcveinminer.client.config.ClientConfigManager.instance.hudPositionY = state.hudPositionY;
        com.tcveinminer.client.config.ClientConfigManager.instance.hudAnchor = state.hudAnchor;
        com.tcveinminer.client.config.ClientConfigManager.instance.showToast = state.showToast;
        com.tcveinminer.client.config.ClientConfigManager.instance.toastPositionX = state.toastPositionX;
        com.tcveinminer.client.config.ClientConfigManager.instance.toastPositionY = state.toastPositionY;
        com.tcveinminer.client.config.ClientConfigManager.instance.toastAnchor = state.toastAnchor;
        com.tcveinminer.client.config.ClientConfigManager.save();
        
        minecraft.setScreen(parent);
        parent.rebuildMenu();
    }
    
    @Override
    public void onClose() {
        saveAndClose();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float pt) {
        // transparent — thấy game thật
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        // Render background toolbar
        ctx.fill(px, py, px + W, py + H, 0xCC111111);
        
        // Title
        ctx.drawString(font, Component.translatable("gui.tcveinminer.hudconfig.title").getString(), px + 10, py + 10, ThemeColors.GOLD, true);
        
        int cx = px + 10;
        int cy = py + 30; // Style button
        
        cy += 24;
        // Space for Scale label
        String scaleStr = String.format("%.2fx", state.hudScale);
        ctx.drawString(font, Component.translatable("gui.tcveinminer.hudconfig.scale").getString() + scaleStr, cx + 24, cy + 4, ThemeColors.TEXT_DIM, true);
        cy += 16;
        
        // Bar for Scale
        int barW = 132;
        int barX = cx + 24;
        int barY = cy + 6;
        ctx.fill(barX, barY, barX + barW, barY + 8, 0xFF444444);
        float scalePct = (state.hudScale - 0.5f) / 1.5f;
        ctx.fill(barX, barY, barX + (int)(barW * scalePct), barY + 8, ThemeColors.GOLD);

        cy += 24;
        // Space for Opacity label
        String opacityStr = String.format("%d%%", (int)(state.hudOpacity * 100));
        ctx.drawString(font, Component.translatable("gui.tcveinminer.hudconfig.opacity").getString() + opacityStr, cx + 24, cy + 4, ThemeColors.TEXT_DIM, true);
        cy += 16;
        
        // Bar for Opacity
        barY = cy + 6;
        ctx.fill(barX, barY, barX + barW, barY + 8, 0xFF444444);
        float opacityPct = (state.hudOpacity - 0.1f) / 0.9f;
        ctx.fill(barX, barY, barX + (int)(barW * opacityPct), barY + 8, ThemeColors.GOLD);

        super.render(ctx, mouseX, mouseY, delta);

        renderHudPlaceholder(ctx);
        renderToastPlaceholder(ctx);
    }
    
    private void renderHudPlaceholder(GuiGraphics ctx) {
        boolean isLocked = LOCKED_STYLES.contains(state.hudStyle);
        int[] pos = getRealPos();
        int realX = pos[0];
        int realY = pos[1];

        IHudRenderer renderer = switch (state.hudStyle) {
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

        String modeLabel = "Sphere R=4";
        String keyHint = "Hold V";

        ctx.pose().pushPose();
        
        float scale = isLocked ? 1.0f : state.hudScale;
        
        if (scale != 1.0f) {
            ctx.pose().scale(scale, scale, 1.0f);
        }
        
        int renderX = isLocked ? realX : (int)(realX / scale);
        int renderY = isLocked ? realY : (int)(realY / scale);
        
        // Render real HUD sample
        renderer.render(ctx, renderX, renderY, HudAnchor.CUSTOM, true, true, modeLabel, keyHint);
        
        ctx.pose().popPose();

        // Kích thước ước chừng vùng chọn để kéo và hiển thị viền
        int textW = 100;
        int textH = 20;
        int phWidth = (int)(textW * scale);
        int phHeight = (int)(textH * scale);
        
        int x1 = realX - phWidth/2;
        int y1 = realY - phHeight/2;

        if (isLocked) {
            String label = Component.translatable("gui.tcveinminer.hudconfig.locked").getString();
            ctx.drawString(font, label, realX - font.width(label)/2, y1 - 12, ThemeColors.TEXT_DIM, true);
        } else {
            // Vẽ viền nhấp nháy cho vùng chọn
            long time = System.currentTimeMillis();
            boolean blink = (time / 500) % 2 == 0;
            int borderColor = blink ? ThemeColors.GOLD : 0xFFFFAA00;
            ctx.renderOutline(x1, y1, phWidth, phHeight, borderColor);
            
            String label = Component.translatable("gui.tcveinminer.hudconfig.drag").getString();
            ctx.drawString(font, label, realX - font.width(label)/2, y1 - 12, ThemeColors.GOLD, true);
        }
    }
    
    private void renderToastPlaceholder(GuiGraphics ctx) {
        if (!state.showToast) return;
        int[] pos = getRealToastPos();
        int realX = pos[0];
        int realY = pos[1];
        
        String msg = "Đào xong 64 khối";
        int msgW = font.width(msg);
        
        int drawX = realX;
        if (state.toastAnchor == HudAnchor.TOP_CENTER || state.toastAnchor == HudAnchor.BOTTOM_CENTER || state.toastAnchor == HudAnchor.CUSTOM) {
            drawX -= msgW / 2;
        } else if (state.toastAnchor == HudAnchor.TOP_RIGHT || state.toastAnchor == HudAnchor.MIDDLE_RIGHT || state.toastAnchor == HudAnchor.BOTTOM_RIGHT) {
            drawX -= msgW;
        }
        
        ctx.drawString(font, msg, drawX, realY, ThemeColors.TEXT_VALUE, true);
        
        int phWidth = msgW + 10;
        int phHeight = 16;
        int x1 = drawX - 5;
        int y1 = realY - 4;
        
        long time = System.currentTimeMillis();
        boolean blink = (time / 500) % 2 == 0;
        int borderColor = blink ? ThemeColors.EMERALD_BORDER : 0xFF00AA55;
        ctx.renderOutline(x1, y1, phWidth, phHeight, borderColor);
        
        String label = "[Kéo thả Toast]";
        ctx.drawString(font, label, realX - font.width(label)/2, y1 - 12, ThemeColors.EMERALD_TEXT, true);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check toast first
        if (state.showToast) {
            int[] pos = getRealToastPos();
            int realX = pos[0];
            int realY = pos[1];
            String msg = "Đào xong 64 khối";
            int msgW = font.width(msg);
            
            int drawX = realX;
            if (state.toastAnchor == HudAnchor.TOP_CENTER || state.toastAnchor == HudAnchor.BOTTOM_CENTER || state.toastAnchor == HudAnchor.CUSTOM) {
                drawX -= msgW / 2;
            } else if (state.toastAnchor == HudAnchor.TOP_RIGHT || state.toastAnchor == HudAnchor.MIDDLE_RIGHT || state.toastAnchor == HudAnchor.BOTTOM_RIGHT) {
                drawX -= msgW;
            }
            
            if (mouseX >= drawX - 5 && mouseX <= drawX + msgW + 5 &&
                mouseY >= realY - 4 && mouseY <= realY + 12) {
                isDraggingToast = true;
                return true;
            }
        }

        if (!LOCKED_STYLES.contains(state.hudStyle)) {
            int[] pos = getRealPos();
            int realX = pos[0];
            int realY = pos[1];
            
            int phWidth = (int)(100 * state.hudScale);
            int phHeight = (int)(20 * state.hudScale);
            
            if (mouseX >= realX - phWidth/2 && mouseX <= realX + phWidth/2 &&
                mouseY >= realY - phHeight/2 && mouseY <= realY + phHeight/2) {
                isDraggingHud = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingToast) {
            state.toastAnchor = HudAnchor.CUSTOM;
            state.toastPositionX = (int)mouseX;
            state.toastPositionY = (int)mouseY;
            return true;
        }
        if (isDraggingHud && !LOCKED_STYLES.contains(state.hudStyle)) {
            state.hudAnchor = HudAnchor.CUSTOM;
            state.hudPositionX = (int)mouseX;
            state.hudPositionY = (int)mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingHud = false;
        isDraggingToast = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    private int[] getRealPos() {
        boolean isLocked = LOCKED_STYLES.contains(state.hudStyle);
        int screenW = this.minecraft.getWindow().getGuiScaledWidth();
        int screenH = this.minecraft.getWindow().getGuiScaledHeight();
        
        int realX = state.hudPositionX;
        int realY = state.hudPositionY;

        if (isLocked) {
            switch (state.hudStyle) {
                case ARC, CIRCLE, ORBITAL, CROSSHAIR_TAG -> { realX = screenW/2; realY = screenH/2; }
                case ACTION_BAR -> { realX = screenW/2; realY = screenH - 45; }
                case HOTBAR -> { realX = screenW/2; realY = screenH - 22; }
                case COMPASS -> { realX = screenW/2; realY = 20; }
                case PROGRESS_BAR -> { realX = screenW/2; realY = screenH - 10; }
                case CHAT_LINE -> { realX = 20; realY = screenH - 40; }
                case TOOLTIP -> { realX = screenW/2; realY = screenH/2; }
                default -> { realX = screenW/2; realY = screenH/2; }
            }
        } else if (state.hudAnchor != HudAnchor.CUSTOM) {
            switch (state.hudAnchor) {
                case TOP_LEFT -> { realX += 30; realY += 30; }
                case TOP_CENTER -> { realX += screenW/2; realY += 30; }
                case TOP_RIGHT -> { realX += screenW - 30; realY += 30; }
                case MIDDLE_LEFT -> { realX += 30; realY += screenH/2; }
                case MIDDLE_RIGHT -> { realX += screenW - 30; realY += screenH/2; }
                case BOTTOM_LEFT -> { realX += 30; realY += screenH - 30; }
                case BOTTOM_RIGHT -> { realX += screenW - 30; realY += screenH - 30; }
                case BOTTOM_CENTER -> { realX += screenW/2; realY += screenH - 40; }
                case CUSTOM -> {}
            }
        }
        return new int[]{realX, realY};
    }
    
    private int[] getRealToastPos() {
        int screenW = this.minecraft.getWindow().getGuiScaledWidth();
        int screenH = this.minecraft.getWindow().getGuiScaledHeight();
        
        int realX = state.toastPositionX;
        int realY = state.toastPositionY;

        if (state.toastAnchor != HudAnchor.CUSTOM) {
            switch (state.toastAnchor) {
                case TOP_LEFT -> { realX += 30; realY += 30; }
                case TOP_CENTER -> { realX += screenW/2; realY += 30; }
                case TOP_RIGHT -> { realX += screenW - 30; realY += 30; }
                case MIDDLE_LEFT -> { realX += 30; realY += screenH/2; }
                case MIDDLE_RIGHT -> { realX += screenW - 30; realY += screenH/2; }
                case BOTTOM_LEFT -> { realX += 30; realY += screenH - 30; }
                case BOTTOM_RIGHT -> { realX += screenW - 30; realY += screenH - 30; }
                case BOTTOM_CENTER -> { realX += screenW/2; realY += screenH - 40; }
                case CUSTOM -> {}
            }
        }
        return new int[]{realX, realY};
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
