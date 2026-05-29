package com.tcveinminer.client.gui.screens;

import com.tcveinminer.client.config.ClientConfig;
import com.tcveinminer.client.gui.CustomButton;
import com.tcveinminer.client.gui.widgets.AmberButton;
import com.tcveinminer.client.util.DrawHelper;
import com.tcveinminer.client.util.ExpressionEvaluator;
import com.tcveinminer.client.util.ThemeColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

import com.tcveinminer.client.gui.screens.custom.*;

public class CustomShapeDesignerScreen extends Screen {

    private int W, H, px, py;
    private final int HDR_H = 20;
    private final int FOOTER_H = 26;

    private static final String[][] QUICK_PRESETS = {
            { "gui.tcveinminer.preset.sphere",     "x^2 + y^2 + z^2 <= 16" },
            { "gui.tcveinminer.preset.cube",       "abs(x) <= 3 && abs(y) <= 3 && abs(z) <= 3" },
            { "gui.tcveinminer.preset.cylinder",   "x^2 + z^2 <= 9 && abs(y) <= 4" },
            { "gui.tcveinminer.preset.ellipsoid",  "x^2/9 + y^2/4 + z^2/9 <= 1" },
            { "gui.tcveinminer.preset.diamond",    "abs(x) + abs(y) + abs(z) <= 5" },
            { "gui.tcveinminer.preset.cone",       "sqrt(x^2 + z^2) <= 4 - abs(y) && abs(y) <= 4" },
    };
    private CustomButton[] presetButtons;

    private final Screen parent;
    /** Khác null = đang chỉnh sửa entry có sẵn; null = tạo mới. */
    private final ClientConfig.CustomShapeEntry editEntry;

    private TextFieldWidget nameField;
    private TextFieldWidget equationField;

    private String equationError = null;
    private String nameError = null;
    private String equationText;
    private String nameText;

    private float rotX = -20, rotY = 45;
    private boolean dragging;
    private double lastMX, lastMY;

    private RenderMesh meshCache = new RenderMesh();
    private ShapeType currentShapeType = ShapeType.EMPTY;
    private String lastBuiltEquation = null;

    private long equationChangedAt = 0;
    private static final long DEBOUNCE_MS = 400;
    private boolean voxelDirty = false;

    public CustomShapeDesignerScreen(Screen parent, ClientConfig.CustomShapeEntry editEntry) {
        super(Text.translatable("gui.tcveinminer.designer.title"));
        this.parent = parent;
        this.editEntry = editEntry;
        this.nameText     = (editEntry != null) ? editEntry.name     : Text.translatable("gui.tcveinminer.designer.default_name").getString();
        this.equationText = (editEntry != null) ? editEntry.equation : "x^2 + y^2 + z^2 <= 4";
    }

    @Override
    protected void init() {
        this.W = Math.max(420, Math.min(560, (int)(this.width * 0.8)));
        this.H = Math.max(260, Math.min(360, (int)(this.height * 0.85)));
        this.px = (this.width - this.W) / 2;
        this.py = (this.height - this.H) / 2;

        addDrawableChild(new CustomButton(px + W - 70, py + 2, 60, 16, Text.translatable("gui.tcveinminer.button.cancel"), btn -> client.setScreen(parent)));

        int cx = px + 10, cy = py + HDR_H + 10, leftW = (W - 30) / 2;
        int inputY = cy + 12;

        nameField = new TextFieldWidget(textRenderer, cx + 4, inputY, leftW - 8, 16, Text.translatable("gui.tcveinminer.designer.name_prompt"));
        nameField.setMaxLength(100);
        nameField.setText(nameText);
        nameField.setChangedListener(s -> { nameText = s; nameError = null; });
        addDrawableChild(nameField);

        inputY += 34;
        equationField = new TextFieldWidget(textRenderer, cx + 4, inputY, leftW - 8, 16, Text.literal(""));
        equationField.setMaxLength(300);
        equationField.setText(equationText);
        equationField.setChangedListener(s -> {
            equationText = s;
            equationError = null;
            equationChangedAt = System.currentTimeMillis();
            voxelDirty = true;
        });
        addDrawableChild(equationField);

        addDrawableChild(new AmberButton(px + W - 160, py + H - 22, 150, 16, Text.translatable("gui.tcveinminer.button.save_equation"), btn -> trySave()));

        // Quick preset buttons – two columns below equation field
        presetButtons = new CustomButton[QUICK_PRESETS.length];
        int btnW = (leftW - 8) / 2 - 2;
        int btnStartY = inputY + 56;
        for (int i = 0; i < QUICK_PRESETS.length; i++) {
            final String preset = QUICK_PRESETS[i][1];
            final String label  = QUICK_PRESETS[i][0];
            int col = i % 2, row = i / 2;
            int bx = cx + 4 + col * (btnW + 4);
            int by = btnStartY + row * 19;
            presetButtons[i] = new CustomButton(bx, by, btnW, 14,
                    Text.translatable(label),
                    btn -> {
                        equationField.setText(preset);
                        equationText = preset;
                        equationError = null;
                        equationChangedAt = System.currentTimeMillis();
                        voxelDirty = true;
                    });
            addDrawableChild(presetButtons[i]);
        }

        voxelDirty = true;
        rebuildVoxelsIfNeeded();
    }

    private void trySave() {
        nameError = null;
        if (nameText == null || nameText.isBlank()) { nameError = Text.translatable("gui.tcveinminer.error.missing_name").getString(); return; }
        if (equationError != null) return;
        if (meshCache.isEmpty() || currentShapeType == ShapeType.EMPTY) {
            nameError = Text.translatable("gui.tcveinminer.error.no_blocks").getString(); return;
        }

        // Lưu vào MenuState của MainMenuScreen (parent)
        if (parent instanceof MainMenuScreen mainMenu) {
            List<ClientConfig.CustomShapeEntry> list = mainMenu.getState().customShapes;

            if (editEntry != null) {
                // Chỉnh sửa: cập nhật entry cũ tại chỗ
                String oldId = editEntry.strategyId;
                boolean wasEnabled = mainMenu.getState().enabledShapes.remove(oldId);
                boolean wasSelected = oldId.equals(mainMenu.getState().selectedShapeId);
                editEntry.name = nameText;
                editEntry.equation = equationText;
                editEntry.strategyId = ClientConfig.customShapeId(nameText);
                if (wasEnabled) mainMenu.getState().enabledShapes.add(editEntry.strategyId);
                if (wasSelected) mainMenu.getState().selectedShapeId = editEntry.strategyId;
            } else {
                // Tạo mới: kiểm tra trùng tên
                String newId = ClientConfig.customShapeId(nameText);
                boolean duplicate = list.stream().anyMatch(e -> e.strategyId.equals(newId));
                if (duplicate) { nameError = Text.translatable("gui.tcveinminer.error.duplicate_name").getString(); return; }

                list.add(new ClientConfig.CustomShapeEntry(nameText, equationText));
                mainMenu.getState().enabledShapes.add(newId);
                mainMenu.getState().selectedShapeId = newId;
            }
        }

        // Rebuild UI trước khi quay lại để danh sách cập nhật ngay
        if (parent instanceof MainMenuScreen mainMenu) {
            mainMenu.rebuildMenu();
        }
        client.setScreen(parent);
    }

    private void rebuildVoxelsIfNeeded() {
        if (!voxelDirty) return;
        if (equationChangedAt != 0 && System.currentTimeMillis() - equationChangedAt < DEBOUNCE_MS) return;

        voxelDirty = false;
        String expr = equationText;

        if (expr.equals(lastBuiltEquation)) return;
        lastBuiltEquation = expr;
        equationError = null;

        ExpressionEvaluator ev = new ExpressionEvaluator(expr);
        if (!ev.isValid()) {
            equationError = ev.getError();
            return;
        }

        ShapeAnalyzer analyzer = new ShapeAnalyzer(ev, expr);
        analyzer.analyze();

        this.currentShapeType = analyzer.getType();
        this.meshCache = GeometryGenerator.generate(analyzer, ev);

        if (this.currentShapeType == ShapeType.EMPTY) {
            equationError = Text.translatable("gui.tcveinminer.error.no_blocks").getString();
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        rebuildVoxelsIfNeeded();

        DrawHelper.drawPanel(ctx, px, py, W, H);
        DrawHelper.drawHeader(ctx, px, py, W, HDR_H);
        ctx.drawTextWithShadow(textRenderer, Text.translatable("gui.tcveinminer.designer.header").getString(), px + 10, py + 6, ThemeColors.TEXT_TITLE);

        int cx = px + 10, cy = py + HDR_H + 10, cw = W - 20, ch = H - HDR_H - FOOTER_H - 20;
        int leftW = (cw - 10) / 2, rightX = cx + leftW + 10, rightW = cw - leftW - 10;

        DrawHelper.drawCard(ctx, cx, cy, leftW, ch);
        ctx.drawTextWithShadow(textRenderer, Text.translatable("gui.tcveinminer.designer.label_name").getString(), cx + 4, cy + 2, ThemeColors.TEXT_LABEL);
        ctx.drawTextWithShadow(textRenderer, Text.translatable("gui.tcveinminer.designer.label_equation").getString(), cx + 4, cy + 36, ThemeColors.TEXT_LABEL);

        int statusY = cy + 70;
        if (equationError != null) {
            ctx.drawTextWithShadow(textRenderer, equationError, cx + 4, statusY, ThemeColors.TEXT_ERROR);
        } else if (voxelDirty) {
            ctx.drawTextWithShadow(textRenderer, Text.translatable("gui.tcveinminer.designer.processing").getString(), cx + 4, statusY, ThemeColors.TEXT_DIM);
        } else {
            String status = currentShapeType.getDisplayName();
            ctx.drawTextWithShadow(textRenderer, status, cx + 4, statusY, (currentShapeType == ShapeType.FINITE_VOLUME) ? ThemeColors.EMERALD_TEXT : ThemeColors.GOLD);
        }

        if (nameError != null) ctx.drawTextWithShadow(textRenderer, nameError, cx + 4, statusY + 44, ThemeColors.TEXT_ERROR);

        int prevH = ch * 55 / 100;
        DrawHelper.drawCard(ctx, rightX, cy, rightW, prevH);

        int previewCX = rightX + rightW / 2, previewCY = cy + prevH / 2 + 8;
        ctx.enableScissor(rightX + 1, cy + 22, rightX + rightW - 1, cy + prevH - 12);

        VoxelRenderer.drawWireframe3D(ctx, textRenderer, meshCache, equationError, previewCX, previewCY, rightW, rotX, rotY);

        ctx.disableScissor();

        String bc = Text.translatable("gui.tcveinminer.designer.display_count").getString() + meshCache.size();
        ctx.drawTextWithShadow(textRenderer, bc, rightX + rightW - 6 - textRenderer.getWidth(bc), cy + prevH - 12, ThemeColors.EMERALD_TEXT);
        int hintY = cy + prevH + 10;
        
        ctx.drawTextWithShadow(textRenderer, Text.translatable("gui.tcveinminer.designer.hint_title").getString(), rightX, hintY, ThemeColors.TEXT_LABEL);
        ctx.drawTextWithShadow(textRenderer, Text.translatable("gui.tcveinminer.designer.hint_vars").getString(), rightX, hintY + 14, ThemeColors.TEXT_DIM);
        ctx.drawTextWithShadow(textRenderer, Text.translatable("gui.tcveinminer.designer.hint_ops").getString(), rightX, hintY + 26, ThemeColors.TEXT_DIM);
        ctx.drawTextWithShadow(textRenderer, Text.translatable("gui.tcveinminer.designer.hint_logic").getString(), rightX, hintY + 38, ThemeColors.TEXT_DIM);
        ctx.drawTextWithShadow(textRenderer, Text.translatable("gui.tcveinminer.designer.hint_funcs").getString(), rightX, hintY + 50, ThemeColors.TEXT_DIM);
        int fy = py + H - FOOTER_H;
        ctx.fill(px + 2, fy, px + W - 2, py + H - 2, ThemeColors.BG_PANEL_INSET);
        ctx.drawTextWithShadow(textRenderer, Text.translatable("gui.tcveinminer.designer.drag_hint").getString(), px + 10, fy + 8, ThemeColors.TEXT_DIM);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int cx = px + 10, cy = py + HDR_H + 10, cw = W - 20, ch = H - HDR_H - FOOTER_H - 20;
        int rightX = cx + (cw - 10) / 2 + 10, rightW = cw - (cw - 10) / 2 - 10;
        if (mx >= rightX && mx <= rightX + rightW && my >= cy && my <= cy + ch * 55 / 100) {
            dragging = true; lastMX = mx; lastMY = my; return true;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (dragging) {
            rotY += (float)(mx - lastMX) * 0.5f;
            rotX = (float) Math.max(-80, Math.min(80, rotX - (my - lastMY) * 0.5));
            lastMX = mx; lastMY = my; return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override public boolean mouseReleased(double mx, double my, int btn) { dragging = false; return super.mouseReleased(mx, my, btn); }
    @Override public boolean keyPressed(int kc, int sc, int mod) { if (kc == 256) { client.setScreen(parent); return true; } return super.keyPressed(kc, sc, mod); }
    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext c, int mx, int my, float d) {}
}
