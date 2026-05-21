package com.tcveinminer.gui.screens;

import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.gui.CustomButton;
import com.tcveinminer.util.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.*;

/**
 * Màn hình cấu hình chính — 5 tab:
 * BẢNG CHÍNH | CẤU HÌNH PHỤ | CHẾ ĐỘ ĐÀO | BỘ LỌC | CÀI ĐẶT MÀU
 */
public class MainMenuScreen extends Screen {

    // ── Layout ───────────────────────────────────────────────────────────
    private static final int W         = 320;
    private static final int H         = 540;
    private static final int HDR_H     = 40;
    private static final int TAB_H     = 40;
    private static final int FOOTER_H  = 60;
    private static final int PAD_X     = 12;
    private static final int PAD_Y     = 12;
    private static final int CONT_OFF  = HDR_H + TAB_H; // relative to py

    // ── Tabs ─────────────────────────────────────────────────────────────
    private static final int T_DASH=0, T_GEN=1, T_SHAPES=2, T_FILTER=3, T_COLOR=4;
    private static final String[] TABS = {
            "CHÍNH","PHỤ","ĐÀO","LỌC","MÀU"
    };

    // ── Preset color swatches ─────────────────────────────────────────────
    private static final int[][] PRESET_RGB = {
            {16,185,129},{216,161,91},{59,130,246},{239,68,68},
            {168,85,247},{249,115,22},{203,213,225},{6,182,212}
    };
    private static final String[] PRESET_NAMES = {
            "Xanh lục","Vàng ròng","Thạch anh","Đỏ Reds.",
            "Tím thạch","Cam đồng","Xám kim","Xanh KCuong"
    };

    private final Screen parent;
    private int px, py;
    private int tab = T_DASH;

    // ── Config state ──────────────────────────────────────────────────────
    private int     maxBlocks;
    private int     activationMode;
    private boolean showHud;
    private boolean showOutline;
    private Set<String>          enabledShapes;
    private List<String>         blacklist;
    private Map<String,Boolean>  enabledTools;
    private int  colorR, colorG, colorB;
    private boolean colorRainbow, colorDisabled;

    // ── UI state ──────────────────────────────────────────────────────────
    private boolean saveNotify;
    private long    saveHideAt;
    private String  hoveredShapeId;
    private int     shapeScroll, blScroll;
    private TextFieldWidget blockInput;
    private String  blError = "";

    public MainMenuScreen(Screen parent) {
        super(Text.literal("TC-VeinMiner Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        px = (width - W) / 2;
        py = (height - H) / 2;

        ModConfig cfg = ConfigManager.get();
        maxBlocks     = cfg.maxBlocks;
        showHud       = cfg.showHud;
        enabledShapes = new LinkedHashSet<>(cfg.enabledShapes);

        // Fields that may not exist yet in ModConfig — use reflection with defaults
        activationMode = fieldInt(cfg,  "activationMode",  1);
        showOutline    = fieldBool(cfg, "showOutline",      true);
        colorR         = fieldInt(cfg,  "colorR",           216);
        colorG         = fieldInt(cfg,  "colorG",           161);
        colorB         = fieldInt(cfg,  "colorB",           91);
        colorRainbow   = fieldBool(cfg, "colorRainbow",     false);
        colorDisabled  = fieldBool(cfg, "colorDisabled",    false);
        blacklist      = new ArrayList<>(fieldList(cfg,  "blacklistedBlocks"));
        enabledTools   = new LinkedHashMap<>(fieldToolMap(cfg));

        if (hoveredShapeId == null) hoveredShapeId = cfg.miningShape.name();

        rebuild();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Widget rebuild
    // ─────────────────────────────────────────────────────────────────────
    private void rebuild() {
        clearChildren();
        int cx = px + PAD_X, cw = W - PAD_X * 2;
        int cy = py + CONT_OFF + PAD_Y;

        // Amber "Lưu cấu hình" CTA
        addDrawableChild(new AmberButton(
                px + 8, py + H - 38, W - 16, 28,
                Text.literal("Lưu"),
                btn -> { save(); triggerSave(); }
        ));

        switch (tab) {
            case T_DASH   -> buildDash(cx, cy, cw);
            case T_GEN    -> buildGen(cx, cy, cw);
            case T_SHAPES -> buildShapes(cx, cy, cw);
            case T_FILTER -> buildFilter(cx, cy, cw);
            case T_COLOR  -> buildColor(cx, cy, cw);
        }
    }

    // ── T_DASH ─────────────────────────────────────────────────────────
    private void buildDash(int cx, int cy, int cw) {
        int cw2 = cw;
        // MaxBlocks slider positioned inside card
        addDrawableChild(new MaxBlockSlider(cx + 8, cy + 80, cw - 16, 12));
    }

    // ── T_GEN ──────────────────────────────────────────────────────────
    private void buildGen(int cx, int cy, int cw) {
        int cw2 = cw;
        String[] modes = {
                "Đè nút (Mặc định)",
                "Đè nút + sneak",
                "Nhấn nút bật",
                "Nhấn nút + sneak"
        };
        for (int i = 0; i < 4; i++) {
            final int m = i + 1;
            addDrawableChild(new CustomButton(cx + 4, cy + 12 + i*26, cw - 8, 22,
                    Text.literal(modes[i]), btn -> { activationMode = m; rebuild(); }));
        }
        addDrawableChild(new CustomButton(cx + 4, cy + 116, cw - 8, 22,
                Text.literal("Hiển thị HUD nổi"),
                btn -> { showHud = !showHud; rebuild(); }));
        addDrawableChild(new CustomButton(cx + 4, cy + 144, cw - 8, 22,
                Text.literal("Hiển thị Outline"),
                btn -> { showOutline = !showOutline; rebuild(); }));
    }

    // ── T_SHAPES ───────────────────────────────────────────────────────
    private void buildShapes(int cx, int cy, int cw) {
        int listW = cw;
        // Custom designer button
        addDrawableChild(new CustomButton(cx + 4, cy + 12, listW - 8, 24,
                Text.literal("+ TỰ THIẾT KẾ"),
                btn -> client.setScreen(new CustomShapeDesignerScreen(this))));
        // Restore defaults
        addDrawableChild(new CustomButton(cx + 4, cy + 40, listW - 8, 20,
                Text.literal("Khôi phục mặc định"),
                btn -> { enabledShapes = new LinkedHashSet<>(Set.of("FACE","EDGES","CORNERS","TUNNEL_1x2","AREA_3x3","TREE_CAP")); rebuild(); }));

        ModConfig.MiningShape[] shapes = ModConfig.MiningShape.values();
        int itemH = 40, maxV = 8;
        int start = Math.max(0, Math.min(shapeScroll, shapes.length - maxV));
        for (int i = start; i < Math.min(shapes.length, start + maxV); i++) {
            ModConfig.MiningShape s = shapes[i];
            boolean on = enabledShapes.contains(s.name());
            int ry = cy + 68 + (i - start) * (itemH + 2);
            addDrawableChild(new CustomButton(cx + 4, ry, listW - 8, itemH,
                    Text.literal(on ? "✓ " + s.label : "  " + s.label),
                    btn -> {
                        if (on) { if (enabledShapes.size() > 1) enabledShapes.remove(s.name()); }
                        else    { enabledShapes.add(s.name()); }
                        rebuild();
                    }));
        }
    }

    // ── T_FILTER ───────────────────────────────────────────────────────
    private void buildFilter(int cx, int cy, int cw) {
        int cardW2 = (cw - 4) / 2;

        // Tool toggle cards (1-col or 2-col grid)
        String[][] tools = {
                {"all","TẤT CẢ"},{"hand","Tay"},
                {"item","Vật"},{"pickaxe","Cúp"},
                {"axe","Rìu"},{"shovel","Xẻng"},
                {"sword","Kiếm"},{"hoe","Cuốc"}
        };
        for (int i = 0; i < tools.length; i++) {
            final String key = tools[i][0];
            int col = i % 2, row = i / 2;
            addDrawableChild(new CustomButton(
                    cx + col * (cardW2 + 2), cy + 12 + row * 28, cardW2, 24,
                    Text.literal(tools[i][1]),
                    btn -> toggleTool(key)));
        }

        // "Thêm khối trên tay" button
        addDrawableChild(new CustomButton(cx + 4, cy + 136, cw - 8, 20,
                Text.literal("Thêm khối trên tay"),
                btn -> {
                    String held = "minecraft:deepslate_coal_ore";
                    if (!blacklist.contains(held)) { blacklist.add(held); blError = ""; }
                    else blError = "Đã có rồi";
                    rebuild();
                }));

        // Block input field
        if (blockInput == null) {
            blockInput = new TextFieldWidget(textRenderer, 0, 0, cw - 60, 18, Text.literal("minecraft:dirt"));
            blockInput.setMaxLength(100);
        }
        blockInput.setX(cx + 4); blockInput.setY(cy + 160); blockInput.setWidth(cw - 60);
        addDrawableChild(blockInput);

        // "Thêm ID" amber button
        addDrawableChild(new AmberButton(cx + 4, cy + 182, cw - 8, 18,
                Text.literal("Thêm"),
                btn -> {
                    String id = blockInput.getText().trim().toLowerCase();
                    if (id.isBlank())        { blError = "Không được để trống"; rebuild(); return; }
                    if (!id.contains(":"))   { blError = "Thiếu namespace"; rebuild(); return; }
                    if (blacklist.contains(id)){ blError = "Đã có rồi"; rebuild(); return; }
                    blacklist.add(id); blError = "";
                    rebuild();
                }));

        // Blacklist "XÓA" buttons
        int maxBl = 6, startBl = Math.max(0, Math.min(blScroll, blacklist.size() - maxBl));
        for (int i = startBl; i < Math.min(blacklist.size(), startBl + maxBl); i++) {
            final String bid = blacklist.get(i);
            int ry = cy + 206 + (i - startBl) * 20;
            addDrawableChild(new CustomButton(cx + cw - 44, ry, 40, 16,
                    Text.literal("XÓA"), btn -> { blacklist.remove(bid); rebuild(); }));
        }
    }

    // ── T_COLOR ────────────────────────────────────────────────────────
    private void buildColor(int cx, int cy, int cw) {
        int sw = (cw - 20) / 4;

        // 8 preset swatches in 2 rows of 4
        for (int i = 0; i < 8; i++) {
            final int ri=PRESET_RGB[i][0], gi=PRESET_RGB[i][1], bi=PRESET_RGB[i][2];
            addDrawableChild(new CustomButton(
                    cx + 4 + (i%4)*(sw+2), cy + 12 + (i/4)*(sw+16), sw, sw, Text.empty(),
                    btn -> { colorR=ri; colorG=gi; colorB=bi; colorRainbow=false; colorDisabled=false; rebuild(); }));
        }
        // Rainbow
        addDrawableChild(new CustomButton(cx + 4, cy + 60, sw, sw, Text.empty(),
                btn -> { colorRainbow=true; colorDisabled=false; rebuild(); }));
        // Tắt màu
        addDrawableChild(new CustomButton(cx + 4 + sw + 2, cy + 60, sw, sw, Text.empty(),
                btn -> { colorDisabled=true; colorRainbow=false; rebuild(); }));

        // RGB sliders
        boolean dis = colorRainbow || colorDisabled;
        addDrawableChild(new RGBSlider(cx + 4, cy + 90, cw - 8, 10, 'R', colorR/255.0, dis));
        addDrawableChild(new RGBSlider(cx + 4, cy + 108, cw - 8, 10, 'G', colorG/255.0, dis));
        addDrawableChild(new RGBSlider(cx + 4, cy + 126, cw - 8, 10, 'B', colorB/255.0, dis));
    }

    private void toggleTool(String key) {
        if ("all".equals(key)) {
            boolean now = !enabledTools.getOrDefault("all", false);
            enabledTools.put("all", now);
            if (now) for (String k : List.of("hand","item","pickaxe","axe","shovel","sword","hoe")) enabledTools.put(k, false);
        } else {
            enabledTools.put(key, !enabledTools.getOrDefault(key, false));
            if (Boolean.TRUE.equals(enabledTools.get(key))) enabledTools.put("all", false);
        }
        rebuild();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  RENDER
    // ─────────────────────────────────────────────────────────────────────
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Darken background
        ctx.fill(0, 0, width, height, 0xB8010208);

        // Panel
        DrawHelper.drawPanel(ctx, px, py, W, H);

        // Header
        DrawHelper.drawHeader(ctx, px, py, W, HDR_H);
        ctx.drawTextWithShadow(textRenderer, "TC VEINGLOW",
                px + W / 2 - textRenderer.getWidth("TC VEINGLOW") / 2, py + 14, ThemeColors.TEXT_TITLE);

        // Tab bar
        ctx.fill(px+2, py+HDR_H, px+W-2, py+HDR_H+TAB_H, ThemeColors.BG_PANEL_INSET);
        DrawHelper.drawSolidBorder(ctx, px+2, py+HDR_H+TAB_H-1, W-4, 1, ThemeColors.BORDER_DEFAULT);
        int tabW = (W-4) / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            int tx = px+2+i*tabW;
            if (i == tab) ctx.fill(tx, py+HDR_H+TAB_H-2, tx+tabW, py+HDR_H+TAB_H, ThemeColors.GOLD);
            int tc = (i==tab) ? ThemeColors.GOLD : ThemeColors.TEXT_LABEL;
            int tw = textRenderer.getWidth(TABS[i]);
            ctx.drawTextWithShadow(textRenderer, TABS[i], tx + (tabW - tw) / 2, py + HDR_H + (TAB_H - 8) / 2, tc);
        }

        // Footer
        int fy = py+H-FOOTER_H;
        ctx.fill(px+2, fy, px+W-2, py+H-2, ThemeColors.BG_PANEL_INSET);
        DrawHelper.drawSolidBorder(ctx, px+2, fy, W-4, 1, ThemeColors.BORDER_DEFAULT);
        ctx.drawTextWithShadow(textRenderer, "Lưu cấu hình Client", px + 8, fy + 20, ThemeColors.TEXT_LABEL);

        // Tab content
        int cx=px+PAD_X, cw=W-PAD_X*2, cy=py+CONT_OFF+PAD_Y;
        switch (tab) {
            case T_DASH   -> drawDash(ctx, cx, cy, cw);
            case T_GEN    -> drawGen(ctx, cx, cy, cw);
            case T_SHAPES -> drawShapes(ctx, cx, cy, cw);
            case T_FILTER -> drawFilter(ctx, cx, cy, cw);
            case T_COLOR  -> drawColor(ctx, cx, cy, cw);
        }

        super.render(ctx, mouseX, mouseY, delta);

        if (saveNotify && System.currentTimeMillis() < saveHideAt) drawSaveNotify(ctx);
        else saveNotify = false;
    }

    // ── Draw: BẢNG CHÍNH ─────────────────────────────────────────────
    private void drawDash(DrawContext ctx, int cx, int cy, int cw) {
        int cw2 = cw;
        // Status card
        DrawHelper.drawCard(ctx, cx, cy, cw2, 70);
        ctx.drawTextWithShadow(textRenderer, "CHẾ ĐỘ HIỆN TẠI", cx + 8, cy + 6, ThemeColors.TEXT_LABEL);
        DrawHelper.drawCard(ctx, cx + 4, cy + 22, cw2 - 8, 40);
        ctx.drawTextWithShadow(textRenderer, "ĐÀO", cx + 10, cy + 26, ThemeColors.TEXT_LABEL);
        try { ctx.drawTextWithShadow(textRenderer, ModConfig.MiningShape.valueOf(hoveredShapeId).label, cx + 10, cy + 38, ThemeColors.GOLD); }
        catch (Exception e) { ctx.drawTextWithShadow(textRenderer, hoveredShapeId, cx + 10, cy + 38, ThemeColors.GOLD); }

        // MaxBlocks card
        DrawHelper.drawCard(ctx, cx, cy + 84, cw2, 70);
        ctx.drawTextWithShadow(textRenderer, "GIỚI HẠN KHỐI", cx + 8, cy + 90, ThemeColors.TEXT_LABEL);
        String lim = maxBlocks + "/128";
        ctx.drawTextWithShadow(textRenderer, lim, cx + cw2 - 8 - textRenderer.getWidth(lim), cy + 90, ThemeColors.GOLD);
        // Slider track bg (actual slider widget drawn by super)
        ctx.fill(cx + 8, cy + 110, cx + cw2 - 8, cy + 122, ThemeColors.BG_INPUT);
        DrawHelper.drawSolidBorder(ctx, cx + 8, cy + 110, cw2 - 16, 12, ThemeColors.BORDER_DEFAULT);
        ctx.drawTextWithShadow(textRenderer,
                "Giới hạn khối tối đa",
                cx + 8, cy + 128, ThemeColors.TEXT_HINT);
    }

    // ── Draw: CẤU HÌNH PHỤ ───────────────────────────────────────────
    private void drawGen(DrawContext ctx, int cx, int cy, int cw) {
        int cw2 = cw;
        DrawHelper.drawCard(ctx, cx, cy, cw2, 180);
        ctx.drawTextWithShadow(textRenderer, "CHẾ ĐỘ KÍCH HOẠT", cx + 8, cy + 8, ThemeColors.TEXT_LABEL);
        String[] modes = {"Đè nút", "Đè + sneak", "Nhấn bật", "Nhấn + sneak"};
        for (int i = 0; i < 4; i++) {
            int ry = cy + 26 + i * 24;
            boolean sel = (activationMode == i + 1);
            ctx.fill(cx + 8, ry, cx + cw2 - 8, ry + 20, sel ? ThemeColors.GOLD_FILL : ThemeColors.BG_INPUT);
            DrawHelper.drawSolidBorder(ctx, cx + 8, ry, cw2 - 16, 20, sel ? ThemeColors.GOLD : ThemeColors.BORDER_DEFAULT);
            ctx.drawTextWithShadow(textRenderer, modes[i], cx + 14, ry + 6, sel ? ThemeColors.GOLD : ThemeColors.TEXT_LABEL);
        }
        // Divider
        int dY = cy + 26 + 4 * 24 + 6;
        DrawHelper.drawDivider(ctx, cx + 8, dY, cw2 - 16);
        drawCheckRow(ctx, cx + 8, dY + 8, cw2 - 16, "HUD nổi", showHud);
        drawCheckRow(ctx, cx + 8, dY + 32, cw2 - 16, "Outline", showOutline);
    }

    private void drawCheckRow(DrawContext ctx, int rx, int ry, int rw, String label, boolean checked) {
        ctx.fill(rx, ry, rx + rw, ry + 22, ThemeColors.BG_ROW);
        DrawHelper.drawSolidBorder(ctx, rx, ry, rw, 22, ThemeColors.BORDER_DEFAULT);
        ctx.drawTextWithShadow(textRenderer, label, rx + 8, ry + 7, ThemeColors.TEXT_BRIGHT);
        int bx = rx + rw - 20, by = ry + 4;
        ctx.fill(bx, by, bx + 14, by + 14, ThemeColors.BG_INPUT);
        DrawHelper.drawSolidBorder(ctx, bx, by, 14, 14, ThemeColors.BORDER_DIM);
        if (checked) ctx.fill(bx + 2, by + 2, bx + 12, by + 12, ThemeColors.GOLD);
    }

    // ── Draw: CHẾ ĐỘ ĐÀO ─────────────────────────────────────────────
    private void drawShapes(DrawContext ctx, int cx, int cy, int cw) {
        int listW = cw;

        ctx.drawTextWithShadow(textRenderer, "DANH SÁCH CHẾ ĐỘ", cx, cy, ThemeColors.TEXT_LABEL);

        // Custom designer button bg
        ctx.fill(cx + 4, cy + 12, cx + listW - 4, cy + 36, ThemeColors.PURPLE_FILL);
        DrawHelper.drawSolidBorder(ctx, cx + 4, cy + 12, listW - 8, 24, ThemeColors.PURPLE_BORDER_DIM);
        String custTxt = "+ TỰ THIẾT KẾ";
        ctx.drawTextWithShadow(textRenderer, custTxt, cx + (listW - textRenderer.getWidth(custTxt)) / 2, cy + 20, ThemeColors.PURPLE_TEXT);

        // Shape rows
        ModConfig.MiningShape[] shapes = ModConfig.MiningShape.values();
        int itemH = 40, maxV = 8;
        int start = Math.max(0, Math.min(shapeScroll, shapes.length - maxV));
        for (int i = start; i < Math.min(shapes.length, start + maxV); i++) {
            ModConfig.MiningShape s = shapes[i];
            int ry = cy + 44 + (i - start) * (itemH + 2);
            boolean sel = s.name().equals(ConfigManager.get().miningShape.name());
            boolean on = enabledShapes.contains(s.name());

            ctx.fill(cx + 4, ry, cx + listW - 4, ry + itemH, sel ? ThemeColors.BG_ROW_SELECTED : ThemeColors.BG_ROW);
            DrawHelper.drawSolidBorder(ctx, cx + 4, ry, listW - 8, itemH, sel ? ThemeColors.GOLD_BORDER : ThemeColors.BORDER_DEFAULT);
            if (sel) ctx.fill(cx + 4, ry + 8, cx + 7, ry + itemH - 8, ThemeColors.GOLD);

            String label = (on ? "✓ " : "  ") + s.label;
            ctx.drawTextWithShadow(textRenderer, label, cx + 12, ry + 12, sel ? ThemeColors.GOLD : ThemeColors.TEXT_WHITE);
            ctx.drawTextWithShadow(textRenderer, s.desc, cx + 12, ry + 24, ThemeColors.TEXT_LABEL);
        }
    }

    /** Vẽ hologram placeholder 3D (3x3 grid of mini blocks) */
    private void drawHologramPreview(DrawContext ctx, int cx, int cy, ModConfig.MiningShape shape) {
        int gold = ThemeColors.GOLD;
        // Core block
        ctx.fill(cx-6, cy-6, cx+6, cy+6, ThemeColors.GOLD_FILL);
        DrawHelper.drawSolidBorder(ctx, cx-6, cy-6, 12, 12, gold);
        ctx.drawTextWithShadow(textRenderer, "CORE", cx-textRenderer.getWidth("CORE")/2, cy-3, ThemeColors.GOLD);

        // Surrounding blocks based on shape type — simplified 2D projection
        int[][] offsets = getShapeOffsets(shape);
        for (int[] off : offsets) {
            int bx=cx+off[0]*22, by=cy+off[1]*22;
            ctx.fill(bx-5, by-5, bx+5, by+5, ThemeColors.GOLD_FILL);
            DrawHelper.drawSolidBorder(ctx, bx-5, by-5, 10, 10, gold);
        }
    }

    private int[][] getShapeOffsets(ModConfig.MiningShape shape) {
        return switch (shape) {
            case FACE -> new int[][]{{1,0},{-1,0},{0,1},{0,-1}};
            case EDGES -> new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,1},{1,-1},{-1,-1}};
            case CORNERS -> new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,1},{1,-1},{-1,-1},{0,2},{0,-2}};
            case TUNNEL_1x2 -> new int[][]{{0,-1},{0,-2},{0,-3},{0,1}};
            case TUNNEL_3x3 -> new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,1},{1,-1},{-1,-1}};
            case AREA_3x3 -> new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,1},{1,-1},{-1,-1}};
            case AREA_5x5 -> new int[][]{{2,0},{-2,0},{0,2},{0,-2},{2,2},{-2,2},{2,-2},{-2,-2},{1,0},{-1,0},{0,1}};
            case TALL_1x2 -> new int[][]{{1,0},{-1,0},{0,1},{0,-1},{0,-2}};
            case STAIR_UP -> new int[][]{{0,-1},{1,-1},{1,-2},{2,-2}};
            case STAIR_DOWN -> new int[][]{{0,1},{1,1},{1,2},{2,2}};
            case TREE_CAP -> new int[][]{{0,-1},{0,-2},{0,-3},{1,-2},{-1,-2}};
            default -> new int[][]{{1,0},{-1,0},{0,1},{0,-1}};
        };
    }

    // ── Draw: BỘ LỌC ─────────────────────────────────────────────────
    private void drawFilter(DrawContext ctx, int cx, int cy, int cw) {
        int cardW2 = (cw - 4) / 2;

        ctx.drawTextWithShadow(textRenderer, "CÔNG CỤ KÍCH HOẠT", cx, cy, ThemeColors.TEXT_LABEL);

        String[][] tools = {
                {"all","TẤT CẢ"},{"hand","Tay"},
                {"item","Vật"},{"pickaxe","Cúp"},
                {"axe","Rìu"},{"shovel","Xẻng"},
                {"sword","Kiếm"},{"hoe","Cuốc"}
        };
        for (int i = 0; i < tools.length; i++) {
            String key = tools[i][0], label = tools[i][1];
            boolean on = Boolean.TRUE.equals(enabledTools.get(key));
            int col = i % 2, row = i / 2;
            int bx = cx + col * (cardW2 + 2), by = cy + 14 + row * 28;
            ctx.fill(bx, by, bx + cardW2, by + 24, on ? ThemeColors.EMERALD_FILL : ThemeColors.BG_ROW);
            DrawHelper.drawSolidBorder(ctx, bx, by, cardW2, 24, on ? ThemeColors.EMERALD_BORDER : ThemeColors.BORDER_DEFAULT);
            int tc = on ? ThemeColors.EMERALD_TEXT : ThemeColors.TEXT_LABEL;
            int lw = textRenderer.getWidth(label);
            ctx.drawTextWithShadow(textRenderer, label, bx + (cardW2 - lw) / 2, by + 8, tc);
        }

        // Right panel
        ctx.drawTextWithShadow(textRenderer, "BLK BỊ CẤM (" + blacklist.size() + ")",
                cx, cy + 130, ThemeColors.TEXT_LABEL);
        DrawHelper.drawCard(ctx, cx, cy + 148, cw, 60);
        ctx.drawTextWithShadow(textRenderer, "KHỐI TRÊN TAY", cx + 8, cy + 156, ThemeColors.TEXT_DIM);
        ctx.drawTextWithShadow(textRenderer, "minecraft:coal_ore", cx + 8, cy + 168, ThemeColors.GOLD);

        // input label
        ctx.drawTextWithShadow(textRenderer, "ID Khối", cx, cy + 220, ThemeColors.TEXT_DIM);
        if (!blError.isEmpty())
            ctx.drawTextWithShadow(textRenderer, blError, cx, cy + 254, ThemeColors.TEXT_ERROR);

        // Blacklist list
        int listY = cy + 242;
        int listH = Math.max(50, Math.min(6, blacklist.size()) * 20 + 12);
        DrawHelper.drawCard(ctx, cx, listY, cw, listH);
        if (blacklist.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, "Trống", cx + cw / 2 - 15, listY + 14, ThemeColors.TEXT_HINT);
        } else {
            int maxBl = 6, startBl = Math.max(0, Math.min(blScroll, blacklist.size() - maxBl));
            for (int i = startBl; i < Math.min(blacklist.size(), startBl + maxBl); i++) {
                int ry = listY + 6 + (i - startBl) * 20;
                ctx.fill(cx + 2, ry, cx + cw - 2, ry + 18, i % 2 == 0 ? ThemeColors.BG_ROW : ThemeColors.BG_ROW_HOVER);
                ctx.drawTextWithShadow(textRenderer, blacklist.get(i), cx + 6, ry + 4, ThemeColors.TEXT_LABEL);
            }
        }
    }

    // ── Draw: CÀI ĐẶT MÀU ────────────────────────────────────────────
    private void drawColor(DrawContext ctx, int cx, int cy, int cw) {
        int sw = (cw - 20) / 4;

        // Left card
        DrawHelper.drawCard(ctx, cx, cy, cw, 160);
        ctx.drawTextWithShadow(textRenderer, "MÀU QUÉT KHỐI", cx + 8, cy + 8, ThemeColors.TEXT_LABEL);
        ctx.drawTextWithShadow(textRenderer, "Chọn nhanh", cx + 8, cy + 20, ThemeColors.TEXT_DIM);

        // 8 preset swatches
        for (int i = 0; i < 8; i++) {
            int col = i % 4, row = i / 4;
            int bx = cx + 4 + col * (sw + 2), by = cy + 32 + row * (sw + 12);
            int sc = 0xFF000000 | (PRESET_RGB[i][0] << 16) | (PRESET_RGB[i][1] << 8) | PRESET_RGB[i][2];
            ctx.fill(bx, by, bx + sw, by + sw, ThemeColors.BG_INPUT);
            DrawHelper.drawSolidBorder(ctx, bx, by, sw, sw, ThemeColors.BORDER_DEFAULT);
            ctx.fill(bx + 2, by + 2, bx + sw - 2, by + sw - 2, sc);
            String pname = PRESET_NAMES[i].substring(0, Math.min(5, PRESET_NAMES[i].length()));
            ctx.drawTextWithShadow(textRenderer, pname, bx, by + sw + 1, ThemeColors.TEXT_HINT);
        }

        // RGB sliders and preview
        DrawHelper.drawCard(ctx, cx, cy + 170, cw, 130);
        ctx.drawTextWithShadow(textRenderer, "RGB ĐIỀU CHỈNH", cx + 8, cy + 178, ThemeColors.TEXT_DIM);

        boolean dis = colorRainbow || colorDisabled;
        ctx.drawTextWithShadow(textRenderer, "R: " + colorR, cx + 8, cy + 198, ThemeColors.REDSTONE_TEXT);
        ctx.drawTextWithShadow(textRenderer, "G: " + colorG, cx + cw / 2, cy + 198, ThemeColors.EMERALD_TEXT);
        ctx.drawTextWithShadow(textRenderer, "B: " + colorB, cx + 8, cy + 214, 0xFF93C5FD);

        int pc = colorDisabled ? ThemeColors.BG_INPUT : colorRainbow ? 0xFFEE82EE : 0xFF000000 | (colorR << 16) | (colorG << 8) | colorB;
        int bpX = cx + cw / 2 - 20, bpY = cy + 240;
        ctx.fill(bpX, bpY, bpX + 40, bpY + 40, ThemeColors.BG_INPUT);
        DrawHelper.drawSolidBorder(ctx, bpX, bpY, 40, 40, pc);
        if (!colorDisabled && !colorRainbow) ctx.fill(bpX + 2, bpY + 2, bpX + 38, bpY + 38, (pc & 0x00FFFFFF) | 0x15000000);
    }

    private void drawSaveNotify(DrawContext ctx) {
        String msg="LƯU THÀNH CÔNG!";
        int mw=textRenderer.getWidth(msg)+32, mh=28, mx=(width-mw)/2, my=py-36;
        ctx.fill(mx, my, mx+mw, my+mh, 0xE6091410);
        DrawHelper.drawSolidBorder(ctx, mx, my, mw, mh, ThemeColors.EMERALD_BORDER);
        ctx.fill(mx+10, my+mh/2-2, mx+16, my+mh/2+2, ThemeColors.EMERALD);
        ctx.drawTextWithShadow(textRenderer, msg, mx+20, my+(mh-8)/2, ThemeColors.EMERALD_TEXT);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Mouse & keyboard
    // ─────────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        // Tab bar click
        int tabBarY=py+HDR_H;
        if (my>=tabBarY && my<=tabBarY+TAB_H) {
            int tw=(W-4)/TABS.length;
            for (int i=0;i<TABS.length;i++) {
                int tx=px+2+i*tw;
                if (mx>=tx && mx<=tx+tw) {
                    tab=i; blockInput=null; rebuild(); return true;
                }
            }
        }
        // Shape list click → update selected & hovered
        if (tab==T_SHAPES) {
            int cx=px+PAD_X, cy=py+CONT_OFF+PAD_Y;
            int listW=((W-PAD_X*2)*7)/12;
            ModConfig.MiningShape[] shapes=ModConfig.MiningShape.values();
            int itemH=52, maxV=6;
            int start=Math.max(0,Math.min(shapeScroll,shapes.length-maxV));
            for (int i=start;i<Math.min(shapes.length,start+maxV);i++) {
                int ry=cy+64+(i-start)*(itemH+4);
                if (mx>=cx && mx<=cx+listW-84 && my>=ry && my<=ry+itemH) {
                    hoveredShapeId=shapes[i].name();
                    ConfigManager.get().miningShape=shapes[i];
                    ConfigManager.save();
                    return super.mouseClicked(mx,my,btn);
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (tab==T_SHAPES) { shapeScroll=Math.max(0,shapeScroll-(int)v); return true; }
        if (tab==T_FILTER) { blScroll=Math.max(0,blScroll-(int)v); return true; }
        return super.mouseScrolled(mx,my,h,v);
    }

    @Override
    public boolean keyPressed(int kc, int sc, int mod) {
        if (kc==256) { client.setScreen(parent); return true; }
        return super.keyPressed(kc,sc,mod);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Save
    // ─────────────────────────────────────────────────────────────────────
    private void save() {
        ModConfig cfg=ConfigManager.get();
        cfg.maxBlocks=maxBlocks;
        cfg.showHud=showHud;
        cfg.enabledShapes=enabledShapes;
        setF(cfg,"activationMode",activationMode);
        setF(cfg,"showOutline",   showOutline);
        setF(cfg,"colorR",        colorR);
        setF(cfg,"colorG",        colorG);
        setF(cfg,"colorB",        colorB);
        setF(cfg,"colorRainbow",  colorRainbow);
        setF(cfg,"colorDisabled", colorDisabled);
        setF(cfg,"blacklistedBlocks", blacklist);
        setF(cfg,"enabledTools",  enabledTools);
        ConfigManager.save();
    }

    private void triggerSave() { saveNotify=true; saveHideAt=System.currentTimeMillis()+2500; }

    // ─────────────────────────────────────────────────────────────────────
    //  Reflection helpers (graceful for missing fields)
    // ─────────────────────────────────────────────────────────────────────
    private static int     fieldInt(ModConfig c, String n, int d)      { try{return (int)ModConfig.class.getField(n).get(c);}catch(Exception e){return d;} }
    private static boolean fieldBool(ModConfig c, String n, boolean d) { try{return (boolean)ModConfig.class.getField(n).get(c);}catch(Exception e){return d;} }
    @SuppressWarnings("unchecked")
    private static List<String>        fieldList(ModConfig c, String n)  { try{Object v=ModConfig.class.getField(n).get(c); return v instanceof List<?> l?new ArrayList<>((List<String>)l):new ArrayList<>();}catch(Exception e){return new ArrayList<>();} }
    @SuppressWarnings("unchecked")
    private static Map<String,Boolean> fieldToolMap(ModConfig c)         {
        try { Object v=ModConfig.class.getField("enabledTools").get(c); if(v instanceof Map<?,?> m) return new LinkedHashMap<>((Map<String,Boolean>)m); } catch(Exception ignored){}
        Map<String,Boolean> m=new LinkedHashMap<>(); m.put("all",true);
        for(String k:List.of("hand","item","pickaxe","axe","shovel","sword","hoe")) m.put(k,false);
        return m;
    }
    private static void setF(ModConfig c, String n, Object v) { try{ModConfig.class.getField(n).set(c,v);}catch(Exception ignored){} }

    @Override public boolean shouldPause()     { return false; }
    @Override public void renderBackground(DrawContext c,int mx,int my,float d) {}

    // ─────────────────────────────────────────────────────────────────────
    //  Inner slider widgets
    // ─────────────────────────────────────────────────────────────────────
    private class MaxBlockSlider extends SliderWidget {
        MaxBlockSlider(int x,int y,int w,int h) {
            super(x,y,w,h,Text.literal(String.valueOf(maxBlocks)),(maxBlocks-1)/127.0);
        }
        @Override protected void updateMessage(){maxBlocks=(int)(value*127)+1;setMessage(Text.literal(String.valueOf(maxBlocks)));}
        @Override protected void applyValue(){updateMessage();}
    }

    private class RGBSlider extends SliderWidget {
        private final char ch;
        RGBSlider(int x,int y,int w,int h,char ch,double init,boolean disabled){
            super(x,y,w,h,Text.literal(String.valueOf((int)(init*255))),init);
            this.ch=ch; this.active=!disabled;
        }
        @Override protected void updateMessage(){int v=(int)(value*255); if(ch=='R')colorR=v; else if(ch=='G')colorG=v; else colorB=v; setMessage(Text.literal(String.valueOf(v)));}
        @Override protected void applyValue(){updateMessage();}
    }

    /** Amber gradient CTA button */
    static class AmberButton extends CustomButton {
        AmberButton(int x,int y,int w,int h,Text msg,PressAction p){super(x,y,w,h,msg,p);}
        @Override protected void renderWidget(DrawContext ctx,int mx,int my,float d){
            ButtonDrawUtil.drawAmber(ctx,getX(),getY(),getWidth(),getHeight(),isHovered());
            net.minecraft.client.MinecraftClient mc=net.minecraft.client.MinecraftClient.getInstance();
            int tw=mc.textRenderer.getWidth(getMessage());
            ctx.drawTextWithShadow(mc.textRenderer,getMessage(),getX()+(getWidth()-tw)/2,getY()+(getHeight()-8)/2,ThemeColors.BG_SCREEN);
        }
    }
}
