package com.tcveinminer.client.gui.screens.tabs;

import com.tcveinminer.client.gui.screens.MainMenuScreen;
import com.tcveinminer.client.util.DrawHelper;
import com.tcveinminer.client.util.ThemeColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class SkillsTab implements MenuTab {

    // Không thêm CustomButton vào init — toàn bộ interaction xử lý trong
    // mouseClicked/render
    @Override
    public void init(MainMenuScreen screen, int cx, int cy, int cw, int ch) {
        int sliderWidth = 100;
        int sliderX = cx + cw - sliderWidth - 8;
        int sliderY = cy + ch - DESC_H - 24; // Above description panel
        com.tcveinminer.client.gui.widgets.ProtectThresholdSlider slider = new com.tcveinminer.client.gui.widgets.ProtectThresholdSlider(sliderX, sliderY, sliderWidth, 16, screen.getState());
        screen.addUIElement(slider);
    }

    // ── Dữ liệu từng skill ──────────────────────────────────────────────────
    private record SkillEntry(
            String labelKey, // translation key — tên ngắn hiện trên chip
            String descKey, // translation key — mô tả dài hiện khi hover
            java.util.function.Supplier<Boolean> getter,
            java.util.function.Consumer<MainMenuScreen> toggle) {
    }

    private SkillEntry[] buildEntries(MainMenuScreen screen) {
        return new SkillEntry[] {

                new SkillEntry(
                        "gui.tcveinminer.skills.tree",
                        "gui.tcveinminer.skills.tree.desc",
                        () -> screen.getState().enableTreeCapitatorSkill,
                        s -> {
                            s.getState().enableTreeCapitatorSkill = !s.getState().enableTreeCapitatorSkill;
                            s.rebuildMenu();
                        }),
                new SkillEntry(
                        "gui.tcveinminer.skills.crop",
                        "gui.tcveinminer.skills.crop.desc",
                        () -> screen.getState().enableCropHarvestSkill,
                        s -> {
                            s.getState().enableCropHarvestSkill = !s.getState().enableCropHarvestSkill;
                            s.rebuildMenu();
                        }),
                new SkillEntry(
                        "gui.tcveinminer.skills.interact",
                        "gui.tcveinminer.skills.interact.desc",
                        () -> screen.getState().enableInteractSkill,
                        s -> {
                            s.getState().enableInteractSkill = !s.getState().enableInteractSkill;
                            s.rebuildMenu();
                        }),
                new SkillEntry(
                        "gui.tcveinminer.skills.bucket",
                        "gui.tcveinminer.skills.bucket.desc",
                        () -> screen.getState().enableBucketSkill,
                        s -> {
                            s.getState().enableBucketSkill = !s.getState().enableBucketSkill;
                            s.rebuildMenu();
                        }),
                new SkillEntry(
                        "gui.tcveinminer.general.prevent_fluids",
                        "gui.tcveinminer.general.prevent_fluids.desc",
                        () -> screen.getState().preventMiningNearFluids,
                        s -> {
                            s.getState().preventMiningNearFluids = !s.getState().preventMiningNearFluids;
                            s.rebuildMenu();
                        }),
                new SkillEntry(
                        "gui.tcveinminer.skills.tool_swap",
                        "gui.tcveinminer.skills.tool_swap.desc",
                        () -> screen.getState().enableToolSwapSkill,
                        s -> {
                            s.getState().enableToolSwapSkill = !s.getState().enableToolSwapSkill;
                            s.rebuildMenu();
                        }),
                new SkillEntry(
                        "gui.tcveinminer.skills.tool_protect",
                        "gui.tcveinminer.skills.tool_protect.desc",
                        () -> screen.getState().enableToolProtectSkill,
                        s -> {
                            s.getState().enableToolProtectSkill = !s.getState().enableToolProtectSkill;
                            s.rebuildMenu();
                        }),
        };
    }

    // ── Layout constants ────────────────────────────────────────────────────
    private static final int COLS = 3;
    private static final int CHIP_H = 22;
    private static final int CHIP_GAP = 5;
    private static final int DESC_H = 36; // chiều cao panel mô tả phía dưới
    private static final int TITLE_H = 14;
    private static final int SCROLL_SPEED = CHIP_H + CHIP_GAP;

    // ── State ────────────────────────────────────────────────────────────────
    private int scrollOffset = 0; // px đã cuộn (bội số của CHIP_H+GAP)
    private int hoveredIndex = -1;

    // ── Helpers ──────────────────────────────────────────────────────────────
    /** Chiều cao của vùng lưới chip (không gồm title + desc panel) */
    private int gridAreaH(int ch) {
        return ch - TITLE_H - DESC_H - CHIP_GAP * 2;
    }

    private int chipW(int cw) {
        int padding = 8 * 2; // 8px mỗi bên
        return (cw - padding - CHIP_GAP * (COLS - 1)) / COLS;
    }

    /** Tổng chiều cao nội dung lưới (tất cả hàng) */
    private int contentH(int count) {
        int rows = (count + COLS - 1) / COLS;
        return rows * (CHIP_H + CHIP_GAP) - CHIP_GAP;
    }

    /** Trả về bounding box của chip i trong không gian local (chưa áp scroll) */
    private int[] chipBounds(int i, int cx, int cy, int cw, int gridTop) {
        int cWidth = chipW(cw);
        int col = i % COLS;
        int row = i / COLS;
        int x = cx + 8 + col * (cWidth + CHIP_GAP);
        int y = gridTop + row * (CHIP_H + CHIP_GAP) - scrollOffset;
        return new int[] { x, y, cWidth, CHIP_H };
    }

    // ── Render ───────────────────────────────────────────────────────────────
    @Override
    public void render(DrawContext ctx, MainMenuScreen screen, int cx, int cy, int cw, int ch,
            int mouseX, int mouseY, float delta) {

        DrawHelper.drawCard(ctx, cx, cy, cw, ch);

        // Title
        ctx.drawTextWithShadow(screen.getTextRenderer(),
                Text.translatable("gui.tcveinminer.skills.title").getString(),
                cx + 8, cy + 3, ThemeColors.TEXT_LABEL);

        SkillEntry[] entries = buildEntries(screen);
        int count = entries.length;

        int gridTop = cy + TITLE_H + CHIP_GAP;
        int gridH = gridAreaH(ch);
        int gridBottom = gridTop + gridH;

        // Clamp scroll
        int maxScroll = Math.max(0, contentH(count) - gridH);
        if (scrollOffset > maxScroll)
            scrollOffset = maxScroll;
        if (scrollOffset < 0)
            scrollOffset = 0;

        // Vẽ scrollbar nếu cần
        if (maxScroll > 0) {
            int sbX = cx + cw - 5;
            int sbH = gridH;
            int thumbH = Math.max(12, sbH * gridH / contentH(count));
            int thumbY = gridTop + (int) ((long) scrollOffset * (sbH - thumbH) / maxScroll);
            ctx.fill(sbX, gridTop, sbX + 3, gridTop + sbH, ThemeColors.BG_INPUT);
            ctx.fill(sbX, thumbY, sbX + 3, thumbY + thumbH, ThemeColors.BORDER_DEFAULT);
        }

        // Enable scissor để clip chip trong vùng grid
        ctx.enableScissor(cx, gridTop, cx + cw, gridBottom);

        hoveredIndex = -1;
        for (int i = 0; i < count; i++) {
            int[] b = chipBounds(i, cx, cy, cw, gridTop);
            int bx = b[0], by = b[1], bw = b[2], bh = b[3];

            // Skip nếu ngoài viewport
            if (by + bh < gridTop || by > gridBottom)
                continue;

            boolean active = entries[i].getter.get();
            boolean hovered = mouseX >= bx && mouseX <= bx + bw
                    && mouseY >= by && mouseY <= by + bh
                    && mouseY >= gridTop && mouseY <= gridBottom;
            if (hovered)
                hoveredIndex = i;

            // Nền chip
            int bgColor = active
                    ? (hovered ? 0xFF2A1F00 : 0xFF1A1300)
                    : (hovered ? 0xFF1A2233 : 0xFF0D1117);
            ctx.fill(bx, by, bx + bw, by + bh, bgColor);

            // Viền chip
            int borderColor = active
                    ? (hovered ? ThemeColors.GOLD : 0xFF8B6914)
                    : (hovered ? ThemeColors.BORDER_DEFAULT : 0xFF1F2937);
            DrawHelper.drawSolidBorder(ctx, bx, by, bw, bh, borderColor);

            // Chấm trạng thái (góc trái trên)
            int dotColor = active ? ThemeColors.GOLD : 0xFF374151;
            ctx.fill(bx + 3, by + 3, bx + 7, by + 7, dotColor);

            // Label chip — tên ngắn, màu theo trạng thái
            String label = Text.translatable(entries[i].labelKey).getString();
            int labelColor = active
                    ? (hovered ? 0xFFFFD875 : ThemeColors.GOLD)
                    : (hovered ? ThemeColors.TEXT_BRIGHT : ThemeColors.TEXT_DIM);

            int maxLabelW = bw - 14; // trừ dot + padding
            // Cắt bớt text nếu quá dài
            String displayLabel = label;
            while (screen.getTextRenderer().getWidth(displayLabel) > maxLabelW && displayLabel.length() > 3) {
                displayLabel = displayLabel.substring(0, displayLabel.length() - 1);
            }
            if (!displayLabel.equals(label))
                displayLabel = displayLabel.substring(0, displayLabel.length() - 1) + "…";

            ctx.drawText(screen.getTextRenderer(), displayLabel,
                    bx + 10, by + (bh - 8) / 2, labelColor, false);
        }

        ctx.disableScissor();

        // ── Description panel ────────────────────────────────────────────────
        int descY = cy + ch - DESC_H - 2;
        ctx.fill(cx + 2, descY, cx + cw - 2, descY + DESC_H, 0xFF070B10);
        DrawHelper.drawSolidBorder(ctx, cx + 2, descY, cw - 4, DESC_H, 0xFF1F2937);

        if (hoveredIndex >= 0 && hoveredIndex < count) {
            SkillEntry entry = entries[hoveredIndex];
            String name = Text.translatable(entry.labelKey).getString();
            boolean active = entry.getter.get();

            // Tên skill + badge trạng thái
            ctx.drawTextWithShadow(screen.getTextRenderer(), name,
                    cx + 8, descY + 4,
                    active ? ThemeColors.GOLD : ThemeColors.TEXT_BRIGHT);

            String badge = active ? "✔ ON" : "✘ OFF";
            int badgeColor = active ? 0xFF4ADE80 : 0xFFEF4444;
            int badgeX = cx + cw - screen.getTextRenderer().getWidth(badge) - 8;
            ctx.drawTextWithShadow(screen.getTextRenderer(), badge, badgeX, descY + 4, badgeColor);

            // Mô tả
            String desc = Text.translatable(entry.descKey).getString();
            ctx.drawText(screen.getTextRenderer(), desc,
                    cx + 8, descY + 16, ThemeColors.TEXT_DIM, false);

            // Gợi ý click
            String hint = "[Click to toggle]";
            ctx.drawText(screen.getTextRenderer(), hint,
                    cx + 8, descY + DESC_H - 12, 0xFF374151, false);

            if (screen.getState().enableToolSwapSkill && screen.getState().enableToolProtectSkill) {
                String comboHint = Text.translatable("gui.tcveinminer.skills.tool_combo_hint1").getString();
                ctx.drawText(screen.getTextRenderer(), comboHint, cx + 8 + screen.getTextRenderer().getWidth(hint) + 10, descY + DESC_H - 12, 0xFF00AAFF, false);
            }
        } else {
            // Placeholder khi không hover
            ctx.drawText(screen.getTextRenderer(),
                    "Hover over a skill to see details",
                    cx + 8, descY + (DESC_H - 8) / 2, 0xFF374151, false);
            if (screen.getState().enableToolSwapSkill && screen.getState().enableToolProtectSkill) {
                String comboHint = Text.translatable("gui.tcveinminer.skills.tool_combo_hint2").getString();
                ctx.drawText(screen.getTextRenderer(), comboHint, cx + 8, descY + DESC_H - 12, 0xFF00AAFF, false);
            }
        }

        // Threshold Label
        String threshLabel = Text.translatable("gui.tcveinminer.skills.protect_threshold").getString();
        ctx.drawTextWithShadow(screen.getTextRenderer(), threshLabel, cx + cw - 100 - 8 - screen.getTextRenderer().getWidth(threshLabel) - 4, cy + ch - DESC_H - 20, ThemeColors.TEXT_BRIGHT);

        // Scrollbar hint nếu có thể cuộn
        if (maxScroll > 0) {
            String scrollHint = "↑↓ scroll";
            ctx.drawText(screen.getTextRenderer(), scrollHint,
                    cx + cw - screen.getTextRenderer().getWidth(scrollHint) - 6,
                    descY + DESC_H - 12, 0xFF374151, false);
        }
    }

    // ── Mouse input ──────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(MainMenuScreen screen, double mx, double my, int btn) {
        if (btn != 0)
            return false;

        SkillEntry[] entries = buildEntries(screen);
        int count = entries.length;

        // Tái tính layout — cần cx/cy từ screen
        int cx = screen.getPx() + 10;
        int cy = screen.getPy() + 20 + 8; // HDR_H=20, PAD_Y=8
        int cw = screen.getW() - 20;
        int ch = screen.getH() - 20 - 24 - 36 - 16; // trừ header/tab/footer/pad

        int gridTop = cy + TITLE_H + CHIP_GAP;
        int gridBottom = gridTop + gridAreaH(ch);

        if (my < gridTop || my > gridBottom)
            return false;

        for (int i = 0; i < count; i++) {
            int[] b = chipBounds(i, cx, cy, cw, gridTop);
            if (mx >= b[0] && mx <= b[0] + b[2]
                    && my >= b[1] && my <= b[1] + b[3]) {
                entries[i].toggle.accept(screen);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(MainMenuScreen screen, double mx, double my, double h, double v) {
        scrollOffset -= (int) (v * SCROLL_SPEED);
        return true;
    }
}
