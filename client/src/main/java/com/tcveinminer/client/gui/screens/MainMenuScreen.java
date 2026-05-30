package com.tcveinminer.client.gui.screens;

import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.client.config.ClientConfig;
import com.tcveinminer.client.config.ClientConfigManager;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.client.gui.screens.tabs.*;
import com.tcveinminer.client.gui.widgets.AmberButton;
import com.tcveinminer.client.util.DrawHelper;
import com.tcveinminer.client.util.ThemeColors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.TabOrderedElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public class MainMenuScreen extends Screen {

    private final int HDR_H = 20;
    private final int TAB_H = 24; // Chiều cao thanh điều hướng thu gọn lại
    private final int FOOTER_H = 36;
    private final int PAD_X = 10;
    private final int PAD_Y = 8;
    private float animatedTabX = -1; // Vị trí X mượt của thanh chỉ hướng
    private long tabSwitchTime = 0; // Thời điểm bấm chuyển tab
    private int W, H, px, py;
    private int currentTabIndex = 0;
    private static final String[] TAB_KEYS = {
            "gui.tcveinminer.tab.dash",
            "gui.tcveinminer.tab.general",
            "gui.tcveinminer.tab.shapes",
            "gui.tcveinminer.tab.filter",
            "gui.tcveinminer.tab.color",
            "gui.tcveinminer.tab.skills"
    };

    private final Screen parent;
    private final MenuState state = new MenuState();
    private final MenuTab[] tabInstances = new MenuTab[] {
            new DashTab(), new GeneralTab(), new ShapesTab(), new FilterTab(), new ColorTab(), new SkillsTab()
    };
    private boolean stateLoaded;

    private boolean saveNotify;
    private long saveHideAt;

    public MainMenuScreen(Screen parent) {
        super(Component.literal("TC-VeinMiner Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.W = Math.max(380, Math.min(460, (int) (this.width * 0.7)));
        this.H = Math.max(240, Math.min(320, (int) (this.height * 0.75)));
        this.px = (this.width - this.W) / 2;
        this.py = (this.height - this.H) / 2;

        if (!stateLoaded) {
            ModConfig cfg = ConfigManager.get();
            if (state.selectedShapeId == null)
                state.selectedShapeId = cfg.miningShape.name();
            state.preventMiningNearFluids = cfg.preventMiningNearFluids;
            state.enableBucketSkill = cfg.enableBucketSkill;
            state.enableCropHarvestSkill = cfg.enableCropHarvestSkill;
            state.enableTreeCapitatorSkill = cfg.enableTreeCapitatorSkill;
            state.enableInteractSkill = cfg.enableInteractSkill;
            state.enableBreakSkill = cfg.enableBreakSkill;
            state.enableToolSwapSkill = cfg.enableToolSwapSkill;
            state.enableToolProtectSkill = cfg.enableToolProtectSkill;
            state.toolProtectThreshold = cfg.toolProtectThreshold;

            // --- Load từ ClientConfig ---
            ClientConfig ccfg = ClientConfigManager.instance;
            if (ccfg.currentShape != null && !ccfg.currentShape.isBlank()) {
                state.selectedShapeId = ccfg.currentShape;
            }
            state.showOutline = ccfg.showOutline;
            state.outlineThickness = ccfg.outlineThickness;
            state.colorR = ccfg.colorR;
            state.colorG = ccfg.colorG;
            state.colorB = ccfg.colorB;
            state.outlineAlpha = ccfg.outlineAlpha;
            state.colorRainbow = ccfg.colorRainbow;
            state.colorDisabled = ccfg.colorDisabled;
            state.colorList = new ArrayList<>();
            if (ccfg.colorList != null) {
                for (String hex : ccfg.colorList) {
                    int[] rgb = com.tcveinminer.client.util.ColorManager.fromHex(hex);
                    if (rgb != null) {
                        state.colorList.add(rgb);
                    }
                }
            }
            state.enableFlowAnimation = ccfg.enableFlowAnimation;
            state.segmentLength = ccfg.segmentLength;
            state.flowSmoothness = ccfg.flowSmoothness;
            state.colorTransitionTime = ccfg.colorTransitionTime;
            state.showHud = ccfg.showHud;
            state.activationMode = ccfg.activationMode;
            state.maxBlocks = Math.min(ccfg.clientMaxBlocks, ccfg.serverMaxBlocks);
            state.enabledShapes = new LinkedHashSet<>(ccfg.enabledShapes);
            state.blacklist = new LinkedHashSet<>(ccfg.personalBlacklist.stream()
                    .map(ResourceLocation::parse).toList());
            state.requireCorrectTool = ccfg.requireCorrectTool;
            state.customShapeEquation = ccfg.customShapeEquation;
            state.customShapes = new ArrayList<>(ccfg.customShapes);
            normalizeShapeState();
            stateLoaded = true;
        }

        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        int cx = px + PAD_X, cw = W - PAD_X * 2;
        int cy = py + HDR_H + PAD_Y;
        int ch = H - HDR_H - TAB_H - FOOTER_H - PAD_Y * 2;

        addRenderableWidget(new AmberButton(px + 10, py + H - 28, 80, 20, Component.literal("Report Bug"), btn -> {
            String link = "https://docs.google.com/forms/d/e/1FAIpQLScSBVjy7EBTdKZnfd0wf9AbAebhwi9SBnnY7-_uN9sIGzx5mQ/viewform?usp=dialog";
            minecraft.setScreen(new net.minecraft.client.gui.screens.ConfirmLinkScreen(confirmed -> {
                if (confirmed) {
                    net.minecraft.Util.getPlatform().openUri(link);
                }
                minecraft.setScreen(MainMenuScreen.this);
            }, link, true));
        }));

        addRenderableWidget(new AmberButton(px + W - 90, py + H - 28, 80, 20,
                Component.translatable("gui.tcveinminer.button.save_config"), btn -> {
                    save();
                    triggerSave();
                }));
        addRenderableWidget(new AmberButton(px + W - 20, py + 2, 18, 16, Component.translatable("gui.tcveinminer.button.close"),
                btn -> minecraft.setScreen(parent)));

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

    public <T extends GuiEventListener & Renderable & net.minecraft.client.gui.narration.NarratableEntry> T addUIElement(T element) {
        return this.addRenderableWidget(element);
    }

    public MenuState getState() {
        return state;
    }

    public Font getTextRenderer() {
        return this.font;
    }

    public int getPx() {
        return px;
    }

    public int getPy() {
        return py;
    }

    public int getW() {
        return W;
    }

    public int getH() {
        return H;
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        DrawHelper.drawPanel(ctx, px, py, W, H);

        String title = Component.translatable("gui.tcveinminer.title").getString();
        int titleW = font.width(title);
        int titleX = px + W / 2 - titleW / 2;
        int titleY = py + 6;

        // Vẽ shadow tạo chiều sâu
        ctx.drawString(font, Component.literal(title), titleX + 2, titleY + 2, 0xAA000000, false);

        int currentX = titleX;
        int color1 = ThemeColors.TEXT_TITLE; // Vàng nhạt
        int color2 = ThemeColors.EMERALD_TEXT; // Xanh ngọc
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        for (int i = 0; i < title.length(); i++) {
            String s = String.valueOf(title.charAt(i));
            float ratio = (float) i / Math.max(1, title.length() - 1);
            int r = (int) (r1 + (r2 - r1) * ratio);
            int g = (int) (g1 + (g2 - g1) * ratio);
            int b = (int) (b1 + (b2 - b1) * ratio);
            int color = 0xFF000000 | (r << 16) | (g << 8) | b;
            ctx.drawString(font, Component.literal(s), currentX, titleY, color, true);
            currentX += font.width(s);
        }

        // --- VẼ TAB BAR VỚI HIỆU ỨNG TRƯỢT MƯỢT (LERP) ---
        int tabBarY = py + H - FOOTER_H - TAB_H;
        DrawHelper.drawSolidBorder(ctx, px + 2, tabBarY, W - 4, 1, DrawHelper.BORDER_MODERN);

        int tabW = (W - 4) / TAB_KEYS.length;
        int targetTabX = px + 2 + currentTabIndex * tabW;

        if (animatedTabX == -1) {
            animatedTabX = targetTabX;
        } else {
            // Tịnh tiến mượt vị trí thanh chỉ hướng
            animatedTabX = animatedTabX + (targetTabX - animatedTabX) * 0.25f;
            if (Math.abs(animatedTabX - targetTabX) < 0.1f)
                animatedTabX = targetTabX;
        }

        // Vẽ thanh chỉ hướng màu vàng chạy dưới chân Tab hoạt động
        int tx_start = (int) animatedTabX + 10;
        int tx_end = (int) animatedTabX + tabW - 10;

        // Hiệu ứng glow mờ cho vạch dưới tab đang chọn
        ctx.fillGradient(tx_start - 2, tabBarY + TAB_H - 3, tx_end + 2, tabBarY + TAB_H, 0x00D8A15B, 0x88D8A15B);
        // Vạch chính sắc nét
        ctx.fill(tx_start, tabBarY + TAB_H - 1, tx_end, tabBarY + TAB_H, ThemeColors.GOLD);

        for (int i = 0; i < TAB_KEYS.length; i++) {
            int tx = px + 2 + i * tabW;
            boolean isActive = (i == currentTabIndex);
            int tc = isActive ? ThemeColors.GOLD : ThemeColors.TEXT_DIM;
            String tabLabel = Component.translatable(TAB_KEYS[i]).getString();
            int tw = font.width(tabLabel);
            // Nếu không được chọn, vẽ không bóng để làm chìm đi, nếu chọn vẽ có bóng
            ctx.drawString(font, Component.literal(tabLabel), tx + (tabW - tw) / 2, tabBarY + (TAB_H - 8) / 2, tc, isActive);
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

        // Hiệu ứng Fade chuyển cảnh khi vừa đổi tab (diễn ra trong 120ms)
        long elapsed = System.currentTimeMillis() - tabSwitchTime;
        if (elapsed < 120) {
            int alpha = (int) ((1.0f - (elapsed / 120.0f)) * 0x33); // Giảm dần từ độ mờ 20% về 0%
            ctx.fill(cx, cy, cx + cw, cy + ch, (alpha << 24) | (ThemeColors.APP_BG & 0xFFFFFF));
        }

        super.render(ctx, mouseX, mouseY, delta);

        // --- OVERLAY GỢI Ý BLOCK (FILTER TAB) ---
        if (tabInstances[currentTabIndex] instanceof FilterTab filterTab) {
            filterTab.renderOverlay(ctx, this, mouseX, mouseY);
        }

        // Thông báo lưu thành công
        // Thông báo lưu
        if (saveNotify && System.currentTimeMillis() < saveHideAt) {
            String msg = Component.translatable("gui.tcveinminer.notify.saved").getString();

            // Tính toán thời gian đã trôi qua kể từ lúc nhấn nút Lưu (hiển thị trong
            // 2500ms)
            long startTime = saveHideAt - 2500;
            long elapsed2 = System.currentTimeMillis() - startTime;
            int charsToShow = (int) (elapsed2 / 50); // Cứ 50ms hiển thị thêm 1 ký tự
            String animatedMsg = msg.substring(0, Math.min(msg.length(), Math.max(0, charsToShow)));

            int mw = font.width(msg) + 24, mh = 18, mx = (width - mw) / 2, my = py - 22;
            ctx.fill(mx, my, mx + mw, my + mh, 0xE6091410);
            DrawHelper.drawSolidBorder(ctx, mx, my, mw, mh, ThemeColors.EMERALD_BORDER);

            // Sử dụng animatedMsg để vẽ chữ, nhưng căn lề theo độ dài msg gốc để chữ không
            // bị lệch tâm khi chạy
            int textX = mx + (mw - font.width(msg)) / 2;
            ctx.drawString(font, Component.literal(animatedMsg), textX, my + (mh - 8) / 2, ThemeColors.EMERALD_TEXT, true);
        } else
            saveNotify = false;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int tabBarY = py + H - FOOTER_H - TAB_H;
        if (my >= tabBarY && my <= tabBarY + TAB_H) {
            int tw = (W - 4) / TAB_KEYS.length;
            for (int i = 0; i < TAB_KEYS.length; i++) {
                int tx = px + 2 + i * tw;
                if (mx >= tx && mx <= tx + tw) {
                    if (currentTabIndex != i) {
                        currentTabIndex = i;
                        this.tabSwitchTime = System.currentTimeMillis(); // Kích hoạt hiệu ứng fade nội dung
                        rebuild();
                    }
                    return true;
                }
            }
        }
        if (tabInstances[currentTabIndex].mouseClicked(this, mx, my, btn))
            return true;
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (tabInstances[currentTabIndex].mouseScrolled(this, mx, my, h, v))
            return true;
        return super.mouseScrolled(mx, my, h, v);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (tabInstances[currentTabIndex].mouseDragged(this, mx, my, btn, dx, dy))
            return true;
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (tabInstances[currentTabIndex].mouseReleased(this, mx, my, btn))
            return true;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int kc, int sc, int mod) {
        if (kc == 256) {
            minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(kc, sc, mod);
    }

    private void save() {
        ModConfig cfg = ConfigManager.get();
        try {
            cfg.miningShape = ModConfig.MiningShape.valueOf(state.selectedShapeId);
        } catch (Exception ignored) {
        }
        cfg.preventMiningNearFluids = state.preventMiningNearFluids;
        cfg.enableBucketSkill = state.enableBucketSkill;
        cfg.enableCropHarvestSkill = state.enableCropHarvestSkill;
        cfg.enableTreeCapitatorSkill = state.enableTreeCapitatorSkill;
        cfg.enableInteractSkill = state.enableInteractSkill;
        cfg.enableBreakSkill = state.enableBreakSkill;
        cfg.enableToolSwapSkill = state.enableToolSwapSkill;
        cfg.enableToolProtectSkill = state.enableToolProtectSkill;
        cfg.toolProtectThreshold = state.toolProtectThreshold;
        ConfigManager.save();

        ClientConfig ccfg = ClientConfigManager.instance;
        ccfg.showOutline = state.showOutline;
        ccfg.outlineThickness = state.outlineThickness;
        ccfg.colorR = state.colorR;
        ccfg.colorG = state.colorG;
        ccfg.colorB = state.colorB;
        ccfg.outlineAlpha = state.outlineAlpha;
        ccfg.colorRainbow = state.colorRainbow;
        ccfg.colorDisabled = state.colorDisabled;
        ccfg.colorList = new ArrayList<>();
        if (state.colorList != null) {
            for (int[] rgb : state.colorList) {
                ccfg.colorList.add(com.tcveinminer.client.util.ColorManager.toHex(rgb[0], rgb[1], rgb[2]));
            }
        }
        ccfg.enableFlowAnimation = state.enableFlowAnimation;
        ccfg.segmentLength = state.segmentLength;
        ccfg.flowSmoothness = state.flowSmoothness;
        ccfg.colorTransitionTime = state.colorTransitionTime;
        ccfg.showHud = state.showHud;
        ccfg.activationMode = state.activationMode;
        ccfg.clientMaxBlocks = state.maxBlocks;
        ccfg.currentShape = state.selectedShapeId;
        ccfg.enabledShapes = state.enabledShapes;
        ccfg.personalBlacklist = new ArrayList<>(state.blacklist.stream()
                .map(ResourceLocation::toString).toList());
        ccfg.requireCorrectTool = state.requireCorrectTool;
        ccfg.customShapeEquation = state.customShapeEquation;
        ccfg.customShapes = new ArrayList<>(state.customShapes);
        ClientConfigManager.save();
    }

    private void triggerSave() {
        saveNotify = true;
        saveHideAt = System.currentTimeMillis() + 2500;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics c, int mx, int my, float d) {
    }
}
