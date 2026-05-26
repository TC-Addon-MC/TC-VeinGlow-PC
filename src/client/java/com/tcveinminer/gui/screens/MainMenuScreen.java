package com.tcveinminer.gui.screens;

import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.config.ClientConfig;
import com.tcveinminer.config.ClientConfigManager;
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
import java.util.Set;
import net.minecraft.util.Identifier;
public class MainMenuScreen extends Screen {

    private final int HDR_H     = 20;
    private final int TAB_H     = 30; // Tăng chiều cao để nút tab trông thoáng hơn giống trong ảnh
    private final int FOOTER_H  = 36; // Điều chỉnh lại footer để chứa nút LƯU cho đẹp
    private final int PAD_X     = 10;
    private final int PAD_Y     = 8;

    private int W, H, px, py;
    private int currentTabIndex = 0;
    // Cập nhật tên tab giống hệt trong ảnh
    private static final String[] TABS = {"BẢNG CHÍNH", "CẤU HÌNH PHỤ", "CHẾ ĐỘ ĐÀO", "BỘ LỌC", "CÀI ĐẶT MÀU"};

    private final Screen parent;
    private final MenuState state = new MenuState();
    private final MenuTab[] tabInstances = new MenuTab[] {
            new DashTab(), new GeneralTab(), new ShapesTab(), new FilterTab(), new ColorTab()
    };
    private boolean stateLoaded;

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

        if (!stateLoaded) {
        ModConfig cfg = ConfigManager.get();
        if (state.selectedShapeId == null) state.selectedShapeId = cfg.miningShape.name();

        // --- Load từ ClientConfig (nguồn dữ liệu chính) ---
        ClientConfig ccfg = ClientConfigManager.instance;
        if (ccfg.currentShape != null && !ccfg.currentShape.isBlank()) {
            state.selectedShapeId = ccfg.currentShape;
        }
        state.showOutline         = ccfg.showOutline;
        state.colorR              = ccfg.colorR;
        state.colorG              = ccfg.colorG;
        state.colorB              = ccfg.colorB;
        state.colorRainbow        = ccfg.colorRainbow;
        state.colorDisabled       = ccfg.colorDisabled;
        state.showHud             = ccfg.showHud;
        state.activationMode      = ccfg.activationMode;
        state.maxBlocks           = Math.min(ccfg.clientMaxBlocks, ccfg.serverMaxBlocks);
        state.enabledShapes       = new LinkedHashSet<>(ccfg.enabledShapes);
        state.enabledTools        = new LinkedHashMap<>(ccfg.enabledTools);
        state.blacklist           = new LinkedHashSet<>(ccfg.personalBlacklist.stream()
                .map(Identifier::of).toList());
        state.requireCorrectTool  = ccfg.requireCorrectTool;
        state.customShapeEquation = ccfg.customShapeEquation;
        state.customShapes        = new ArrayList<>(ccfg.customShapes);
        normalizeShapeState();
        stateLoaded = true;
        }

        rebuild();
    }

    private void rebuild() {
        clearChildren();
        int cx = px + PAD_X, cw = W - PAD_X * 2;
        // Tính toán lại không gian nội dung (Content Area): Nằm giữa Header và Tab bar
        int cy = py + HDR_H + PAD_Y;
        int ch = H - HDR_H - TAB_H - FOOTER_H - PAD_Y * 2;

        addDrawableChild(new AmberButton(px + W - 90, py + H - 28, 80, 20, Text.literal("LƯU CẤU HÌNH"), btn -> { save(); triggerSave(); }));
        addDrawableChild(new AmberButton(px + W - 20, py + 2, 18, 16, Text.literal("X"), btn -> client.setScreen(parent)));

        tabInstances[currentTabIndex].init(this, cx, cy, cw, ch);
    }

    public void rebuildMenu() {
        normalizeShapeState();
        this.rebuild();
    }

    public void syncShapeStateToClientConfig() {
        normalizeShapeState();

        ClientConfig ccfg = ClientConfigManager.instance;
        ccfg.currentShape = state.selectedShapeId;
        ccfg.enabledShapes = new LinkedHashSet<>(state.enabledShapes);
        ccfg.customShapes = new ArrayList<>(state.customShapes);
        ClientConfigManager.save();

        try {
            ConfigManager.get().miningShape = ModConfig.MiningShape.valueOf(state.selectedShapeId);
            ConfigManager.save();
        } catch (IllegalArgumentException ignored) {
            // Custom shapes only exist in ClientConfig.
        }
    }

    private void normalizeShapeState() {
        Set<String> availableShapes = new LinkedHashSet<>();
        for (ModConfig.MiningShape shape : ModConfig.MiningShape.values()) {
            availableShapes.add(shape.name());
        }
        for (ClientConfig.CustomShapeEntry entry : state.customShapes) {
            if (entry.strategyId != null && !entry.strategyId.isBlank()) {
                availableShapes.add(entry.strategyId);
            }
        }

        state.enabledShapes.removeIf(shapeId -> !availableShapes.contains(shapeId));
        if (state.enabledShapes.isEmpty()) {
            state.enabledShapes.add("FACE");
        }

        if (state.selectedShapeId == null
                || state.selectedShapeId.isBlank()
                || !availableShapes.contains(state.selectedShapeId)
                || !state.enabledShapes.contains(state.selectedShapeId)) {
            String oldShapeId = state.selectedShapeId;
            String newShapeId = state.enabledShapes.iterator().next();
            System.out.println("[TCVeinMiner] Shape normalized: " + oldShapeId + " -> " + newShapeId);
            state.selectedShapeId = newShapeId;
        }
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

        // Bỏ bớt header rườm rà nếu muốn tối giản, hoặc cứ giữ lại
        ctx.drawTextWithShadow(textRenderer, "TC VEINGLOW", px + W / 2 - textRenderer.getWidth("TC VEINGLOW") / 2, py + 6, ThemeColors.TEXT_TITLE);

        // --- VẼ TAB BAR ---
        int tabBarY = py + H - FOOTER_H - TAB_H;
        DrawHelper.drawSolidBorder(ctx, px + 2, tabBarY, W - 4, 1, DrawHelper.BORDER_MODERN); // Đường viền ngăn cách mảnh

        int tabW = (W - 4) / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            int tx = px + 2 + i * tabW;

            if (i == currentTabIndex) {
                // Viền trắng bao quanh tab đang chọn (mỏng và tinh tế hơn)
                DrawHelper.drawSolidBorder(ctx, tx + 2, tabBarY + 4, tabW - 4, TAB_H - 8, 0xFFFFFFFF);
            }

            int tc = (i == currentTabIndex) ? ThemeColors.GOLD : ThemeColors.TEXT_LABEL;
            int tw = textRenderer.getWidth(TABS[i]);
            ctx.drawTextWithShadow(textRenderer, TABS[i], tx + (tabW - tw) / 2, tabBarY + (TAB_H - 8) / 2, tc);
        }

        // --- VẼ FOOTER ---
        int fy = py + H - FOOTER_H;
        ctx.fill(px + 2, fy, px + W - 2, py + H - 2, ThemeColors.BG_PANEL_INSET);
        DrawHelper.drawSolidBorder(ctx, px + 2, fy, W - 4, 1, ThemeColors.BORDER_DEFAULT);

        // --- VẼ NỘI DUNG TỪNG TAB ---
        int cx = px + PAD_X, cw = W - PAD_X * 2;
        int cy = py + HDR_H + PAD_Y;
        int ch = H - HDR_H - TAB_H - FOOTER_H - PAD_Y * 2;

        tabInstances[currentTabIndex].render(ctx, this, cx, cy, cw, ch, mouseX, mouseY, delta);

        super.render(ctx, mouseX, mouseY, delta);

        // Thông báo lưu
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
        // Cập nhật vùng nhận sự kiện click chuột cho tab bar mới
        int tabBarY = py + H - FOOTER_H - TAB_H;
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
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (tabInstances[currentTabIndex].mouseDragged(this, mx, my, btn, dx, dy)) return true;
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (tabInstances[currentTabIndex].mouseReleased(this, mx, my, btn)) return true;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int kc, int sc, int mod) {
        if (kc == 256) { client.setScreen(parent); return true; }
        return super.keyPressed(kc, sc, mod);
    }

    private void save() {
        // Sync selectedShapeId → ModConfig (vẫn cần cho logic server)
        ModConfig cfg = ConfigManager.get();
        try { cfg.miningShape = ModConfig.MiningShape.valueOf(state.selectedShapeId); } catch (Exception ignored) {}
        ConfigManager.save();

        // --- Ghi vào ClientConfig (nguồn dữ liệu chính) ---
        ClientConfig ccfg = ClientConfigManager.instance;
        ccfg.showOutline         = state.showOutline;
        ccfg.colorR              = state.colorR;
        ccfg.colorG              = state.colorG;
        ccfg.colorB              = state.colorB;
        ccfg.colorRainbow        = state.colorRainbow;
        ccfg.colorDisabled       = state.colorDisabled;
        ccfg.showHud             = state.showHud;
        ccfg.activationMode      = state.activationMode;
        ccfg.clientMaxBlocks     = state.maxBlocks;
        ccfg.currentShape        = state.selectedShapeId;
        ccfg.enabledShapes       = state.enabledShapes;
        ccfg.enabledTools        = state.enabledTools;
        ccfg.personalBlacklist   = new ArrayList<>(state.blacklist.stream()
                .map(Identifier::toString).toList());
        ccfg.requireCorrectTool  = state.requireCorrectTool;
        ccfg.customShapeEquation = state.customShapeEquation;
        ccfg.customShapes        = new ArrayList<>(state.customShapes);
        ClientConfigManager.save();
    }

    private void triggerSave() { saveNotify = true; saveHideAt = System.currentTimeMillis() + 2500; }

    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext c, int mx, int my, float d) {}
}
