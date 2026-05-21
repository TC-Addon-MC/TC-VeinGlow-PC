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
    private static final int W         = 860;
    private static final int H         = 540;
    private static final int HDR_H     = 40;
    private static final int TAB_H     = 40;
    private static final int FOOTER_H  = 60;
    private static final int PAD_X     = 56;
    private static final int PAD_Y     = 24;
    private static final int CONT_OFF  = HDR_H + TAB_H; // relative to py

    // ── Tabs ─────────────────────────────────────────────────────────────
    private static final int T_DASH=0, T_GEN=1, T_SHAPES=2, T_FILTER=3, T_COLOR=4;
    private static final String[] TABS = {
        "BẢNG CHÍNH","CẤU HÌNH PHỤ","CHẾ ĐỘ ĐÀO","BỘ LỌC","CÀI ĐẶT MÀU"
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
            px + W - 24 - 160, py + H - 46, 160, 32,
            Text.literal("Lưu cấu hình"),
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
        int cw2 = Math.min(620, cw);
        int cx2 = cx + (cw - cw2) / 2;
        // MaxBlocks slider positioned inside card
        addDrawableChild(new MaxBlockSlider(cx2 + 16, cy + 152, cw2 - 32, 12));
    }

    // ── T_GEN ──────────────────────────────────────────────────────────
    private void buildGen(int cx, int cy, int cw) {
        int cw2 = Math.min(460, cw);
        int cx2 = cx + (cw - cw2) / 2;
        String[] modes = {
            "Đè nút sẽ đào (Mặc định)",
            "Đè nút + sneak",
            "Nhấn nút thì nó sẽ bật",
            "Nhấn nút bật + sneak"
        };
        for (int i = 0; i < 4; i++) {
            final int m = i + 1;
            addDrawableChild(new CustomButton(cx2+16, cy+32+i*30, cw2-32, 24,
                Text.literal(modes[i]), btn -> { activationMode = m; rebuild(); }));
        }
        addDrawableChild(new CustomButton(cx2+16, cy+172, cw2-32, 28,
            Text.literal("Hiển thị HUD nổi ngoài màn hình"),
            btn -> { showHud = !showHud; rebuild(); }));
        addDrawableChild(new CustomButton(cx2+16, cy+208, cw2-32, 28,
            Text.literal("Hiển thị viền khối quét (Outline)"),
            btn -> { showOutline = !showOutline; rebuild(); }));
    }

    // ── T_SHAPES ───────────────────────────────────────────────────────
    private void buildShapes(int cx, int cy, int cw) {
        int listW = (cw * 7) / 12;
        // Custom designer button
        addDrawableChild(new CustomButton(cx, cy+28, listW, 28,
            Text.literal("+ TỰ THIẾT KẾ CHẾ ĐỘ ĐÀO MỚI (CUSTOM)"),
            btn -> client.setScreen(new CustomShapeDesignerScreen(this))));
        // Restore defaults
        addDrawableChild(new CustomButton(cx+listW-162, cy, 162, 22,
            Text.literal("Khôi phục mặc định"),
            btn -> { enabledShapes = new LinkedHashSet<>(Set.of("FACE","EDGES","CORNERS","TUNNEL_1x2","AREA_3x3","TREE_CAP")); rebuild(); }));

        ModConfig.MiningShape[] shapes = ModConfig.MiningShape.values();
        int itemH = 52, maxV = 6;
        int start = Math.max(0, Math.min(shapeScroll, shapes.length - maxV));
        for (int i = start; i < Math.min(shapes.length, start + maxV); i++) {
            ModConfig.MiningShape s = shapes[i];
            boolean on = enabledShapes.contains(s.name());
            int ry = cy + 64 + (i - start) * (itemH + 4);
            addDrawableChild(new CustomButton(cx+listW-80, ry+14, 74, 22,
                Text.literal(on ? "Đang bật" : "Đang ẩn"),
                btn -> {
                    if (on) { if (enabledShapes.size() > 1) enabledShapes.remove(s.name()); }
                    else    { enabledShapes.add(s.name()); }
                    rebuild();
                }));
        }
    }

    // ── T_FILTER ───────────────────────────────────────────────────────
    private void buildFilter(int cx, int cy, int cw) {
        int leftW  = (cw * 5) / 12;
        int rightX = cx + leftW + 24;
        int rightW = cw - leftW - 24;
        int cardW2 = (leftW - 8) / 2;

        // Tool toggle cards (2-col grid)
        String[][] tools = {
            {"all","TẤT CẢ (ALL)"},{"hand","Tay không (Hand)"},
            {"item","Vật phẩm khác (Item)"},{"pickaxe","Cúp (Pickaxe)"},
            {"axe","Rìu (Axe)"},{"shovel","Xẻng (Shovel)"},
            {"sword","Kiếm (Sword)"},{"hoe","Cuốc (Hoe)"}
        };
        for (int i = 0; i < tools.length; i++) {
            final String key = tools[i][0];
            int col = i % 2, row = i / 2;
            addDrawableChild(new CustomButton(
                cx + col*(cardW2+8), cy+40+row*52, cardW2, 44,
                Text.literal(tools[i][1]),
                btn -> toggleTool(key)));
        }

        // "Thêm khối trên tay" button
        addDrawableChild(new CustomButton(rightX, cy+76, rightW, 22,
            Text.literal("Thêm khối trên tay"),
            btn -> {
                String held = "minecraft:deepslate_coal_ore";
                if (!blacklist.contains(held)) { blacklist.add(held); blError = ""; }
                else blError = "Khối đang cầm trên tay đã có sẵn trong danh sách cấm";
                rebuild();
            }));

        // Block input field
        if (blockInput == null) {
            blockInput = new TextFieldWidget(textRenderer, 0, 0, rightW-80, 20, Text.literal("minecraft:dirt"));
            blockInput.setMaxLength(200);
        }
        blockInput.setX(rightX); blockInput.setY(cy+128); blockInput.setWidth(rightW-80);
        addDrawableChild(blockInput);

        // "Thêm ID" amber button
        addDrawableChild(new AmberButton(rightX, cy+156, rightW, 24,
            Text.literal("Thêm ID"),
            btn -> {
                String id = blockInput.getText().trim().toLowerCase();
                if (id.isBlank())        { blError = "Không được để trống ID"; rebuild(); return; }
                if (!id.contains(":"))   { blError = "Thiếu namespace (ví dụ minecraft:stone)"; rebuild(); return; }
                if (blacklist.contains(id)){ blError = "Block này đã có sẵn trong danh sách"; rebuild(); return; }
                blacklist.add(id); blError = "";
                rebuild();
            }));

        // Blacklist "XÓA" buttons
        int maxBl = 5, startBl = Math.max(0, Math.min(blScroll, blacklist.size() - maxBl));
        for (int i = startBl; i < Math.min(blacklist.size(), startBl + maxBl); i++) {
            final String bid = blacklist.get(i);
            int ry = cy+196 + (i-startBl)*24;
            addDrawableChild(new CustomButton(rightX+rightW-50, ry, 46, 20,
                Text.literal("XÓA"), btn -> { blacklist.remove(bid); rebuild(); }));
        }
    }

    // ── T_COLOR ────────────────────────────────────────────────────────
    private void buildColor(int cx, int cy, int cw) {
        int leftW = (cw-24)/2;
        int sw = (leftW-48)/4;

        // 8 preset swatches
        for (int i = 0; i < 8; i++) {
            final int ri=PRESET_RGB[i][0], gi=PRESET_RGB[i][1], bi=PRESET_RGB[i][2];
            addDrawableChild(new CustomButton(
                cx+16+(i%4)*(sw+6), cy+52+(i/4)*(sw+22), sw, sw, Text.empty(),
                btn -> { colorR=ri; colorG=gi; colorB=bi; colorRainbow=false; colorDisabled=false; rebuild(); }));
        }
        int row2Y = cy+52+2*(sw+22);
        // Rainbow
        addDrawableChild(new CustomButton(cx+16, row2Y, sw, sw, Text.empty(),
            btn -> { colorRainbow=true; colorDisabled=false; rebuild(); }));
        // Tắt màu
        addDrawableChild(new CustomButton(cx+16+sw+6, row2Y, sw, sw, Text.empty(),
            btn -> { colorDisabled=true; colorRainbow=false; rebuild(); }));

        // RGB sliders
        boolean dis = colorRainbow || colorDisabled;
        addDrawableChild(new RGBSlider(cx+16, cy+258, leftW-32, 12, 'R', colorR/255.0, dis));
        addDrawableChild(new RGBSlider(cx+16, cy+282, leftW-32, 12, 'G', colorG/255.0, dis));
        addDrawableChild(new RGBSlider(cx+16, cy+306, leftW-32, 12, 'B', colorB/255.0, dis));
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
        ctx.drawTextWithShadow(textRenderer, "TC VEINGLOW - CẤU HÌNH MOD",
            px+16, py+14, ThemeColors.TEXT_TITLE);

        // Tab bar
        ctx.fill(px+2, py+HDR_H, px+W-2, py+HDR_H+TAB_H, ThemeColors.BG_PANEL_INSET);
        DrawHelper.drawSolidBorder(ctx, px+2, py+HDR_H+TAB_H-1, W-4, 1, ThemeColors.BORDER_DEFAULT);
        int tabW = (W-4) / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            int tx = px+2+i*tabW;
            if (i == tab) ctx.fill(tx, py+HDR_H+TAB_H-2, tx+tabW, py+HDR_H+TAB_H, ThemeColors.GOLD);
            int tc = (i==tab) ? ThemeColors.GOLD : ThemeColors.TEXT_LABEL;
            int tw = textRenderer.getWidth(TABS[i]);
            ctx.drawTextWithShadow(textRenderer, TABS[i], tx+(tabW-tw)/2, py+HDR_H+(TAB_H-8)/2, tc);
        }

        // Footer
        int fy = py+H-FOOTER_H;
        ctx.fill(px+2, fy, px+W-2, py+H-2, ThemeColors.BG_PANEL_INSET);
        DrawHelper.drawSolidBorder(ctx, px+2, fy, W-4, 1, ThemeColors.BORDER_DEFAULT);
        ctx.drawTextWithShadow(textRenderer, "Lưu cấu hình cho phía Client.", px+24, fy+22, ThemeColors.TEXT_LABEL);

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
        int cw2=Math.min(620,cw), cx2=cx+(cw-cw2)/2;
        // Status card
        DrawHelper.drawCard(ctx, cx2, cy, cw2, 90);
        ctx.drawTextWithShadow(textRenderer, "TRẠNG THÁI HOẠT ĐỘNG HIỆN TẠI", cx2+16, cy+12, ThemeColors.TEXT_LABEL);
        int subW=(cw2-48)/2;
        DrawHelper.drawCard(ctx, cx2+12, cy+28, subW, 50);
        ctx.drawTextWithShadow(textRenderer, "CHẾ ĐỘ ĐÀO ĐANG CHỌN", cx2+22, cy+36, ThemeColors.TEXT_LABEL);
        try { ctx.drawTextWithShadow(textRenderer, ModConfig.MiningShape.valueOf(hoveredShapeId).label, cx2+22, cy+52, ThemeColors.TEXT_WHITE); }
        catch (Exception e) { ctx.drawTextWithShadow(textRenderer, hoveredShapeId, cx2+22, cy+52, ThemeColors.TEXT_WHITE); }
        DrawHelper.drawCard(ctx, cx2+24+subW, cy+28, subW, 50);
        ctx.drawTextWithShadow(textRenderer, "CƠ CHẾ KÍCH HOẠT", cx2+34+subW, cy+36, ThemeColors.TEXT_LABEL);
        String ms = switch(activationMode) {
            case 2->"Đè nút + sneak"; case 3->"Nhấn nút để bật"; case 4->"Nhấn nút bật + sneak"; default->"Đè nút sẽ đào";
        };
        ctx.drawTextWithShadow(textRenderer, ms, cx2+34+subW, cy+52, ThemeColors.GOLD);

        // MaxBlocks card
        DrawHelper.drawCard(ctx, cx2, cy+102, cw2, 90);
        ctx.drawTextWithShadow(textRenderer, "GIỚI HẠN KHỐI ĐÀO TỐI ĐA (MAX BLOCKS)", cx2+16, cy+116, ThemeColors.TEXT_LABEL);
        String lim = maxBlocks+" / 128 Khối";
        ctx.drawTextWithShadow(textRenderer, lim, cx2+cw2-16-textRenderer.getWidth(lim), cy+116, ThemeColors.GOLD);
        // Slider track bg (actual slider widget drawn by super)
        ctx.fill(cx2+16, cy+134, cx2+cw2-16, cy+148, ThemeColors.BG_INPUT);
        DrawHelper.drawSolidBorder(ctx, cx2+16, cy+134, cw2-32, 14, ThemeColors.BORDER_DEFAULT);
        ctx.drawTextWithShadow(textRenderer,
            "Giới hạn số lượng khối tối đa một lần đào (Thắt chặt theo giới hạn tối đa 128 khối từ Server).",
            cx2+16, cy+158, ThemeColors.TEXT_HINT);
    }

    // ── Draw: CẤU HÌNH PHỤ ───────────────────────────────────────────
    private void drawGen(DrawContext ctx, int cx, int cy, int cw) {
        int cw2=Math.min(460,cw), cx2=cx+(cw-cw2)/2;
        DrawHelper.drawCard(ctx, cx2, cy, cw2, 302);
        ctx.drawTextWithShadow(textRenderer, "ĐÀO KHI NÀO (CHẾ ĐỘ KÍCH HOẠT)", cx2+16, cy+12, ThemeColors.TEXT_LABEL);
        String[] modes={"Đè nút sẽ đào (Mặc định)","Đè nút + sneak","Nhấn nút thì nó sẽ bật","Nhấn nút bật + sneak"};
        for (int i=0;i<4;i++) {
            int ry=cy+32+i*30;
            boolean sel=(activationMode==i+1);
            ctx.fill(cx2+16, ry, cx2+cw2-16, ry+24, sel?ThemeColors.GOLD_FILL:ThemeColors.BG_INPUT);
            DrawHelper.drawSolidBorder(ctx, cx2+16, ry, cw2-32, 24, sel?ThemeColors.GOLD:ThemeColors.BORDER_DEFAULT);
            ctx.drawTextWithShadow(textRenderer, modes[i], cx2+28, ry+8, sel?ThemeColors.GOLD:ThemeColors.TEXT_LABEL);
        }
        // Divider
        int dY=cy+32+4*30+8;
        DrawHelper.drawDivider(ctx, cx2+16, dY, cw2-32);
        drawCheckRow(ctx, cx2+16, dY+12, cw2-32, "Hiển thị HUD nổi ngoài màn hình", showHud);
        drawCheckRow(ctx, cx2+16, dY+44, cw2-32, "Hiển thị viền khối quét (Outline)", showOutline);
    }

    private void drawCheckRow(DrawContext ctx, int rx, int ry, int rw, String label, boolean checked) {
        ctx.fill(rx, ry, rx+rw, ry+28, ThemeColors.BG_ROW);
        DrawHelper.drawSolidBorder(ctx, rx, ry, rw, 28, ThemeColors.BORDER_DEFAULT);
        ctx.drawTextWithShadow(textRenderer, label, rx+12, ry+10, ThemeColors.TEXT_BRIGHT);
        int bx=rx+rw-26, by=ry+6;
        ctx.fill(bx, by, bx+16, by+16, ThemeColors.BG_INPUT);
        DrawHelper.drawSolidBorder(ctx, bx, by, 16, 16, ThemeColors.BORDER_DIM);
        if (checked) ctx.fill(bx+3, by+3, bx+13, by+13, ThemeColors.GOLD);
    }

    // ── Draw: CHẾ ĐỘ ĐÀO ─────────────────────────────────────────────
    private void drawShapes(DrawContext ctx, int cx, int cy, int cw) {
        int listW=(cw*7)/12, prevX=cx+listW+24, prevW=cw-listW-24;

        ctx.drawTextWithShadow(textRenderer, "DANH SÁCH CHẾ ĐỘ KHAI THÁC QUẶNG", cx, cy, ThemeColors.TEXT_LABEL);

        // Custom designer button bg
        ctx.fill(cx, cy+28, cx+listW, cy+56, ThemeColors.PURPLE_FILL);
        DrawHelper.drawSolidBorder(ctx, cx, cy+28, listW, 28, ThemeColors.PURPLE_BORDER_DIM);
        String custTxt="+ TỰ THIẾT KẾ CHẾ ĐỘ ĐÀO MỚI (CUSTOM)";
        ctx.drawTextWithShadow(textRenderer, custTxt, cx+(listW-textRenderer.getWidth(custTxt))/2, cy+38, ThemeColors.PURPLE_TEXT);

        // Restore defaults button (text only, widget handles click)
        ctx.drawTextWithShadow(textRenderer, "Khôi phục mặc định", cx+listW-162+8, cy+4, ThemeColors.GOLD);

        // Shape rows
        ModConfig.MiningShape[] shapes=ModConfig.MiningShape.values();
        int itemH=52, maxV=6;
        int start=Math.max(0, Math.min(shapeScroll, shapes.length-maxV));
        for (int i=start; i<Math.min(shapes.length,start+maxV); i++) {
            ModConfig.MiningShape s=shapes[i];
            int ry=cy+64+(i-start)*(itemH+4);
            boolean sel=s.name().equals(ConfigManager.get().miningShape.name());
            boolean hov=s.name().equals(hoveredShapeId);
            boolean on=enabledShapes.contains(s.name());

            ctx.fill(cx, ry, cx+listW, ry+itemH, sel?ThemeColors.BG_ROW_SELECTED:hov?ThemeColors.BG_ROW_HOVER:ThemeColors.BG_ROW);
            DrawHelper.drawSolidBorder(ctx, cx, ry, listW, itemH, sel?ThemeColors.GOLD_BORDER:ThemeColors.BORDER_DEFAULT);
            if (sel) ctx.fill(cx, ry+10, cx+3, ry+itemH-10, ThemeColors.GOLD);

            ctx.drawTextWithShadow(textRenderer, s.label, cx+14, ry+12, sel?ThemeColors.GOLD:ThemeColors.TEXT_WHITE);
            ctx.drawTextWithShadow(textRenderer, s.desc,  cx+14, ry+30, ThemeColors.TEXT_LABEL);

            // Toggle pill bg
            int pX=cx+listW-80, pY=ry+14;
            ctx.fill(pX, pY, pX+74, pY+22, on?ThemeColors.EMERALD_FILL:ThemeColors.BG_INPUT);
            DrawHelper.drawSolidBorder(ctx, pX, pY, 74, 22, on?ThemeColors.EMERALD_BORDER:ThemeColors.BORDER_DEFAULT);
            String pillTxt=on?"Đang bật":"Đang ẩn";
            ctx.drawTextWithShadow(textRenderer, pillTxt, pX+(74-textRenderer.getWidth(pillTxt))/2, pY+7, on?ThemeColors.EMERALD_TEXT:ThemeColors.TEXT_LABEL);
        }

        // Preview panel
        DrawHelper.drawCard(ctx, prevX, cy-8, prevW, 420);
        ctx.drawTextWithShadow(textRenderer, "BẢN ĐỒ QUÉT KHỐI MÔ PHỎNG", prevX+16, cy, ThemeColors.TEXT_LABEL);
        String drag="Kéo chuột để xoay camera xem trước";
        ctx.drawTextWithShadow(textRenderer, drag, prevX+(prevW-textRenderer.getWidth(drag))/2, cy+22, ThemeColors.TEXT_DIM);
        try {
            ModConfig.MiningShape hs=ModConfig.MiningShape.valueOf(hoveredShapeId);
            String sname=hs.label.toUpperCase();
            ctx.drawTextWithShadow(textRenderer, sname, prevX+(prevW-textRenderer.getWidth(sname))/2, cy+44, ThemeColors.GOLD);
            // Mock hologram dots (3x3 grid as visual placeholder for 3D viewer)
            drawHologramPreview(ctx, prevX+prevW/2, cy+200, hs);
            // Block count bar
            int barY=cy+390;
            ctx.fill(prevX+8, barY, prevX+prevW-8, barY+28, ThemeColors.BG_INPUT);
            DrawHelper.drawSolidBorder(ctx, prevX+8, barY, prevW-16, 28, ThemeColors.BORDER_DEFAULT);
            ctx.drawTextWithShadow(textRenderer, "Mô phỏng quét khối:", prevX+18, barY+10, ThemeColors.TEXT_LABEL);
            String bc=(hs.blockCount+1)+" Khối";
            ctx.drawTextWithShadow(textRenderer, bc, prevX+prevW-18-textRenderer.getWidth(bc), barY+10, ThemeColors.GOLD);
        } catch (Exception ignored) {}
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
        int leftW=(cw*5)/12, rightX=cx+leftW+24, rightW=cw-leftW-24;
        int cardW2=(leftW-8)/2;

        ctx.drawTextWithShadow(textRenderer, "CÔNG CỤ KÍCH HOẠT QUÉT", cx, cy, ThemeColors.TEXT_LABEL);
        ctx.drawTextWithShadow(textRenderer, "Đánh dấu những nhóm công cụ cầm trên tay được phép kích hoạt tính năng đào quặng lan truyền.",
            cx, cy+14, ThemeColors.TEXT_DIM);

        String[][] tools={
            {"all","TẤT CẢ (ALL)"},{"hand","Tay không (Hand)"},
            {"item","Vật phẩm khác (Item)"},{"pickaxe","Cúp (Pickaxe)"},
            {"axe","Rìu (Axe)"},{"shovel","Xẻng (Shovel)"},
            {"sword","Kiếm (Sword)"},{"hoe","Cuốc (Hoe)"}
        };
        for (int i=0;i<tools.length;i++) {
            String key=tools[i][0], label=tools[i][1];
            boolean on=Boolean.TRUE.equals(enabledTools.get(key));
            int col=i%2, row=i/2;
            int bx=cx+col*(cardW2+8), by=cy+40+row*52;
            ctx.fill(bx, by, bx+cardW2, by+44, on?ThemeColors.EMERALD_FILL:ThemeColors.BG_ROW);
            DrawHelper.drawSolidBorder(ctx, bx, by, cardW2, 44, on?ThemeColors.EMERALD_BORDER:ThemeColors.BORDER_DEFAULT);
            int tc=on?ThemeColors.EMERALD_TEXT:ThemeColors.TEXT_LABEL;
            int lw=textRenderer.getWidth(label);
            ctx.drawTextWithShadow(textRenderer, label, bx+(cardW2-lw)/2, by+10, tc);
            String st=on?"CHO PHÉP":"KHÓA";
            int sw2=textRenderer.getWidth(st);
            ctx.fill(bx+(cardW2-sw2-12)/2, by+26, bx+(cardW2+sw2+12)/2, by+38, on?0x2210B981:ThemeColors.BG_INPUT);
            ctx.drawTextWithShadow(textRenderer, st, bx+(cardW2-sw2)/2, by+28, tc);
        }

        // Right panel
        ctx.drawTextWithShadow(textRenderer, "DANH SÁCH BLOCK BỊ CẤM ĐÀO ("+blacklist.size()+")",
            rightX, cy, ThemeColors.TEXT_LABEL);
        DrawHelper.drawCard(ctx, rightX, cy+18, rightW, 78);
        ctx.drawTextWithShadow(textRenderer, "KHỐI ĐANG CẦM TRÊN TAY", rightX+12, cy+30, ThemeColors.TEXT_DIM);
        ctx.drawTextWithShadow(textRenderer, "minecraft:deepslate_coal_ore", rightX+12, cy+46, ThemeColors.GOLD);
        // input label
        ctx.drawTextWithShadow(textRenderer, "ID Khối thủ công", rightX, cy+112, ThemeColors.TEXT_DIM);
        if (!blError.isEmpty())
            ctx.drawTextWithShadow(textRenderer, blError, rightX, cy+148, ThemeColors.TEXT_ERROR);

        // Blacklist list
        int listY=cy+188;
        int listH=Math.max(50, Math.min(5,blacklist.size())*24+16);
        DrawHelper.drawCard(ctx, rightX, listY, rightW, listH+8);
        if (blacklist.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, "Danh sách trống", rightX+rightW/2-30, listY+18, ThemeColors.TEXT_HINT);
        } else {
            int maxBl=5, startBl=Math.max(0, Math.min(blScroll, blacklist.size()-maxBl));
            for (int i=startBl; i<Math.min(blacklist.size(),startBl+maxBl); i++) {
                int ry=listY+8+(i-startBl)*24;
                ctx.fill(rightX+4, ry, rightX+rightW-4, ry+20, i%2==0?ThemeColors.BG_ROW:ThemeColors.BG_ROW_HOVER);
                ctx.drawTextWithShadow(textRenderer, blacklist.get(i), rightX+10, ry+6, ThemeColors.TEXT_LABEL);
            }
        }
    }

    // ── Draw: CÀI ĐẶT MÀU ────────────────────────────────────────────
    private void drawColor(DrawContext ctx, int cx, int cy, int cw) {
        int leftW=(cw-24)/2, rightX=cx+leftW+24, rightW=cw-leftW-24;
        int sw=(leftW-48)/4;

        // Left card
        DrawHelper.drawCard(ctx, cx, cy, leftW, 340);
        ctx.drawTextWithShadow(textRenderer, "BỘ CÀI ĐẶT MÀU QUÉT KHỐI", cx+16, cy+12, ThemeColors.TEXT_LABEL);
        ctx.drawTextWithShadow(textRenderer, "Màu sắc lựa chọn nhanh", cx+16, cy+34, ThemeColors.TEXT_DIM);

        // 8 preset swatches
        for (int i=0;i<8;i++) {
            int col=i%4, row=i/4;
            int bx=cx+16+col*(sw+6), by=cy+52+row*(sw+22);
            int sc=0xFF000000|(PRESET_RGB[i][0]<<16)|(PRESET_RGB[i][1]<<8)|PRESET_RGB[i][2];
            ctx.fill(bx, by, bx+sw, by+sw, ThemeColors.BG_INPUT);
            DrawHelper.drawSolidBorder(ctx, bx, by, sw, sw, ThemeColors.BORDER_DEFAULT);
            ctx.fill(bx+3, by+3, bx+sw-3, by+sw-3, sc);
            ctx.drawTextWithShadow(textRenderer, PRESET_NAMES[i], bx, by+sw+2, ThemeColors.TEXT_HINT);
        }

        int row2Y=cy+52+2*(sw+22);
        // Rainbow swatch
        ctx.fill(cx+16, row2Y, cx+16+sw, row2Y+sw, colorRainbow?ThemeColors.PURPLE_FILL:ThemeColors.BG_INPUT);
        DrawHelper.drawSolidBorder(ctx, cx+16, row2Y, sw, sw, colorRainbow?ThemeColors.PURPLE:ThemeColors.BORDER_DEFAULT);
        ctx.drawTextWithShadow(textRenderer, "Cầu vồng (Rainbow)", cx+16, row2Y+sw+2, ThemeColors.TEXT_HINT);
        // Tắt màu swatch
        ctx.fill(cx+16+sw+6, row2Y, cx+16+sw*2+6, row2Y+sw, colorDisabled?0xFF334155:ThemeColors.BG_INPUT);
        DrawHelper.drawSolidBorder(ctx, cx+16+sw+6, row2Y, sw, sw, ThemeColors.BORDER_DEFAULT);
        ctx.drawTextWithShadow(textRenderer, "Tắt màu quét", cx+16+sw+6, row2Y+sw+2, ThemeColors.TEXT_HINT);

        // Hex + RGB labels
        ctx.drawTextWithShadow(textRenderer, "MÃ MÀU HEX TỰ NHẬP", cx+16, cy+196, ThemeColors.TEXT_DIM);
        ctx.drawTextWithShadow(textRenderer, "ĐIỀU CHỈNH RGB TRỰC TIẾP", cx+16, cy+236, ThemeColors.TEXT_DIM);
        ctx.drawTextWithShadow(textRenderer, "ĐỎ (R)", cx+16, cy+254, ThemeColors.REDSTONE_TEXT);
        ctx.drawTextWithShadow(textRenderer, String.valueOf(colorR), cx+leftW-36, cy+254, ThemeColors.REDSTONE_TEXT);
        ctx.drawTextWithShadow(textRenderer, "LỤC (G)", cx+16, cy+278, ThemeColors.EMERALD_TEXT);
        ctx.drawTextWithShadow(textRenderer, String.valueOf(colorG), cx+leftW-36, cy+278, ThemeColors.EMERALD_TEXT);
        ctx.drawTextWithShadow(textRenderer, "LAM (B)", cx+16, cy+302, ThemeColors.TEXT_LABEL);
        ctx.drawTextWithShadow(textRenderer, String.valueOf(colorB), cx+leftW-36, cy+302, 0xFF93C5FD);

        // Right: preview card
        DrawHelper.drawCard(ctx, rightX, cy, rightW, 340);
        String prevLabel="XEM TRƯỚC KHỐI QUÉT QUẶNG";
        ctx.drawTextWithShadow(textRenderer, prevLabel, rightX+(rightW-textRenderer.getWidth(prevLabel))/2, cy+12, ThemeColors.TEXT_DIM);

        // Preview block box
        int bpX=rightX+rightW/2-32, bpY=cy+80;
        int pc=colorDisabled?ThemeColors.BG_INPUT:colorRainbow?0xFFEE82EE:0xFF000000|(colorR<<16)|(colorG<<8)|colorB;
        ctx.fill(bpX, bpY, bpX+64, bpY+64, ThemeColors.BG_INPUT);
        DrawHelper.drawSolidBorder(ctx, bpX, bpY, 64, 64, pc);
        if (!colorDisabled && !colorRainbow) ctx.fill(bpX+4, bpY+4, bpX+60, bpY+60, (pc&0x00FFFFFF)|0x15000000);

        int sY=cy+162;
        if (colorDisabled) {
            ctx.drawTextWithShadow(textRenderer, "MÀU SẮC ĐÃ TẮT", rightX+(rightW-textRenderer.getWidth("MÀU SẮC ĐÃ TẮT"))/2, sY, ThemeColors.TEXT_DIM);
        } else if (colorRainbow) {
            ctx.drawTextWithShadow(textRenderer, "CẦU VỒNG TRƯỢT SÁNG ĐỘNG", rightX+(rightW-textRenderer.getWidth("CẦU VỒNG TRƯỢT SÁNG ĐỘNG"))/2, sY, ThemeColors.PURPLE_TEXT);
        } else {
            String hex=String.format("#%02X%02X%02X",colorR,colorG,colorB);
            ctx.drawTextWithShadow(textRenderer, hex, rightX+(rightW-textRenderer.getWidth(hex))/2, sY, ThemeColors.TEXT_WHITE);
            String rgb="RGB: "+colorR+", "+colorG+", "+colorB;
            ctx.drawTextWithShadow(textRenderer, rgb, rightX+(rightW-textRenderer.getWidth(rgb))/2, sY+14, ThemeColors.TEXT_DIM);
        }
    }

    private void drawSaveNotify(DrawContext ctx) {
        String msg="CẤU HÌNH ĐÃ ĐƯỢC LƯU THÀNH CÔNG!";
        int mw=textRenderer.getWidth(msg)+52, mh=32, mx=(width-mw)/2, my=py-44;
        ctx.fill(mx, my, mx+mw, my+mh, 0xE6091410);
        DrawHelper.drawSolidBorder(ctx, mx, my, mw, mh, ThemeColors.EMERALD_BORDER);
        ctx.fill(mx+14, my+mh/2-3, mx+20, my+mh/2+3, ThemeColors.EMERALD);
        ctx.drawTextWithShadow(textRenderer, msg, mx+26, my+(mh-8)/2, ThemeColors.EMERALD_TEXT);
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
