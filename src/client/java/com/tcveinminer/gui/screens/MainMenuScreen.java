package com.tcveinminer.gui.screens;

import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.gui.screens.tabs.*;
import com.tcveinminer.gui.widgets.AmberButton;
import com.tcveinminer.util.DrawHelper;
import com.tcveinminer.util.ThemeColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class MainMenuScreen extends Screen {

    private final int HDR_H     = 20;
    private final int TAB_H     = 20;
    private final int FOOTER_H  = 26;
    private final int PAD_X     = 10;
    private final int PAD_Y     = 8;

    private int W, H, px, py;
    private int currentTabIndex = 0;
    private static final String[] TABS = {"CHÍNH", "PHỤ", "ĐÀO", "LỌC", "MÀU"};

    private final Screen parent;
    private final MenuState state = new MenuState();
    private final MenuTab[] tabInstances = new MenuTab[] {
            new DashTab(), new GeneralTab(), new ShapesTab(), new FilterTab(), new ColorTab()
    };

    private boolean saveNotify;
    private long saveHideAt;

    public MainMenuScreen(Screen parent) {
        super(Text.literal("TC-VeinMiner Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.W = Math.max(320, Math.min(440, (int)(this.width * 0.65)));
        this.H = Math.max(220, Math.min(320, (int)(this.height * 0.75)));
        this.px = (this.width - this.W) / 2;
        this.py = (this.height - this.H) / 2;

        ModConfig cfg = ConfigManager.get();
        state.maxBlocks     = cfg.maxBlocks;
        state.showHud       = cfg.showHud;
        state.enabledShapes = new LinkedHashSet<>(cfg.enabledShapes);

        state.activationMode = fieldInt(cfg,  "activationMode",  1);
        state.showOutline    = fieldBool(cfg, "showOutline",      true);
        state.colorR         = fieldInt(cfg,  "colorR",           216);
        state.colorG         = fieldInt(cfg,  "colorG",           161);
        state.colorB         = fieldInt(cfg,  "colorB",           91);
        state.colorRainbow   = fieldBool(cfg, "colorRainbow",     false);
        state.colorDisabled  = fieldBool(cfg, "colorDisabled",    false);
        state.blacklist      = new ArrayList<>(fieldList(cfg,  "blacklistedBlocks"));
        state.enabledTools   = new LinkedHashMap<>(fieldToolMap(cfg));

        if (state.hoveredShapeId == null) state.hoveredShapeId = cfg.miningShape.name();

        rebuild();
    }

    private void rebuild() {
        clearChildren();
        int cx = px + PAD_X, cw = W - PAD_X * 2;
        int cy = py + HDR_H + TAB_H + PAD_Y;
        int ch = H - (HDR_H + TAB_H + FOOTER_H) - PAD_Y * 2;

        addDrawableChild(new AmberButton(px + W - 70, py + H - 22, 60, 16, Text.literal("LƯU"), btn -> { save(); triggerSave(); }));

        tabInstances[currentTabIndex].init(this, cx, cy, cw, ch);
    }

    public void rebuildMenu() {
        this.rebuild();
    }

    public <T extends Element & Drawable & Selectable> T addUIElement(T element) {
        return this.addDrawableChild(element);
    }

    public MenuState getState() { return state; }
    public TextRenderer getTextRenderer() { return this.textRenderer; }
    public int getPx() { return px; }
    public int getPy() { return py; }
    public int getW() { return W; }
    public int getH() { return H; }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        DrawHelper.drawPanel(ctx, px, py, W, H);
        DrawHelper.drawHeader(ctx, px, py, W, HDR_H);
        ctx.drawTextWithShadow(textRenderer, "TC VEINGLOW", px + W / 2 - textRenderer.getWidth("TC VEINGLOW") / 2, py + 6, ThemeColors.TEXT_TITLE);

        ctx.fill(px + 2, py + HDR_H, px + W - 2, py + HDR_H + TAB_H, ThemeColors.BG_PANEL_INSET);
        DrawHelper.drawSolidBorder(ctx, px + 2, py + HDR_H + TAB_H - 1, W - 4, 1, ThemeColors.BORDER_DEFAULT);
        int tabW = (W - 4) / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            int tx = px + 2 + i * tabW;
            if (i == currentTabIndex) ctx.fill(tx, py + HDR_H + TAB_H - 2, tx + tabW, py + HDR_H + TAB_H, ThemeColors.GOLD);
            int tc = (i == currentTabIndex) ? ThemeColors.GOLD : ThemeColors.TEXT_LABEL;
            int tw = textRenderer.getWidth(TABS[i]);
            ctx.drawTextWithShadow(textRenderer, TABS[i], tx + (tabW - tw) / 2, py + HDR_H + (TAB_H - 8) / 2, tc);
        }

        int fy = py + H - FOOTER_H;
        ctx.fill(px + 2, fy, px + W - 2, py + H - 2, ThemeColors.BG_PANEL_INSET);
        DrawHelper.drawSolidBorder(ctx, px + 2, fy, W - 4, 1, ThemeColors.BORDER_DEFAULT);

        int cx = px + PAD_X, cw = W - PAD_X * 2;
        int cy = py + HDR_H + TAB_H + PAD_Y;
        int ch = H - (HDR_H + TAB_H + FOOTER_H) - PAD_Y * 2;

        tabInstances[currentTabIndex].render(ctx, this, cx, cy, cw, ch, mouseX, mouseY, delta);

        super.render(ctx, mouseX, mouseY, delta);

        if (saveNotify && System.currentTimeMillis() < saveHideAt) {
            String msg = "LƯU THÀNH CÔNG!";
            int mw = textRenderer.getWidth(msg) + 24, mh = 18, mx = (width - mw) / 2, my = py - 22;
            ctx.fill(mx, my, mx + mw, my + mh, 0xE6091410);
            DrawHelper.drawSolidBorder(ctx, mx, my, mw, mh, ThemeColors.EMERALD_BORDER);
            ctx.drawTextWithShadow(textRenderer, msg, mx + (mw - textRenderer.getWidth(msg)) / 2, my + (mh - 8) / 2, ThemeColors.EMERALD_TEXT);
        } else saveNotify = false;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int tabBarY = py + HDR_H;
        if (my >= tabBarY && my <= tabBarY + TAB_H) {
            int tw = (W - 4) / TABS.length;
            for (int i = 0; i < TABS.length; i++) {
                int tx = px + 2 + i * tw;
                if (mx >= tx && mx <= tx + tw) {
                    currentTabIndex = i; rebuild(); return true;
                }
            }
        }
        if (tabInstances[currentTabIndex].mouseClicked(this, mx, my, btn)) return true;
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (tabInstances[currentTabIndex].mouseScrolled(this, mx, my, h, v)) return true;
        return super.mouseScrolled(mx, my, h, v);
    }

    @Override
    public boolean keyPressed(int kc, int sc, int mod) {
        if (kc == 256) { client.setScreen(parent); return true; }
        return super.keyPressed(kc, sc, mod);
    }

    private void save() {
        ModConfig cfg = ConfigManager.get();
        cfg.maxBlocks = state.maxBlocks;
        cfg.showHud = state.showHud;
        cfg.enabledShapes = state.enabledShapes;
        setF(cfg, "activationMode",     state.activationMode);
        setF(cfg, "showOutline",        state.showOutline);
        setF(cfg, "colorR",             state.colorR);
        setF(cfg, "colorG",             state.colorG);
        setF(cfg, "colorB",             state.colorB);
        setF(cfg, "colorRainbow",       state.colorRainbow);
        setF(cfg, "colorDisabled",      state.colorDisabled);
        setF(cfg, "blacklistedBlocks",  state.blacklist);
        setF(cfg, "enabledTools",       state.enabledTools);
        ConfigManager.save();
    }

    private void triggerSave() { saveNotify = true; saveHideAt = System.currentTimeMillis() + 2500; }

    private static int fieldInt(ModConfig c, String n, int d) { try{return (int)ModConfig.class.getField(n).get(c);}catch(Exception e){return d;} }
    private static boolean fieldBool(ModConfig c, String n, boolean d) { try{return (boolean)ModConfig.class.getField(n).get(c);}catch(Exception e){return d;} }
    @SuppressWarnings("unchecked")
    private static List<String> fieldList(ModConfig c, String n) { try{Object v=ModConfig.class.getField(n).get(c); return v instanceof List<?> l?new ArrayList<>((List<String>)l):new ArrayList<>();}catch(Exception e){return new ArrayList<>();} }
    @SuppressWarnings("unchecked")
    private static Map<String,Boolean> fieldToolMap(ModConfig c) {
        try { Object v=ModConfig.class.getField("enabledTools").get(c); if(v instanceof Map<?,?> m) return new LinkedHashMap<>((Map<String,Boolean>)m); } catch(Exception ignored){}
        Map<String,Boolean> m=new LinkedHashMap<>(); m.put("all",true);
        for(String k:List.of("hand","item","pickaxe","axe","shovel","sword","hoe")) m.put(k,false);
        return m;
    }
    private static void setF(ModConfig c, String n, Object v) { try{ModConfig.class.getField(n).set(c,v);}catch(Exception ignored){} }

    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext c, int mx, int my, float d) {}
}