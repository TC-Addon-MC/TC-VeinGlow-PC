package com.tcveinminer.gui.screens.tabs;

import com.tcveinminer.gui.CustomButton;
import com.tcveinminer.gui.screens.MainMenuScreen;
import com.tcveinminer.gui.widgets.AmberButton;
import com.tcveinminer.gui.widgets.ThicknessSlider;
import com.tcveinminer.gui.widgets.TransitionTimeSlider;
import com.tcveinminer.util.ColorManager;
import com.tcveinminer.util.DrawHelper;
import com.tcveinminer.util.ThemeColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;

public class ColorTab implements MenuTab {

        private static final int PREVIEW_SIZE = 48;
        private static final int PANEL_W = 228;
        private static final int PANEL_H = 150;
        private static final int PRESET_CELL = 16;
        private static final int PRESET_GAP = 4;
        private static final int PRESET_COLS = 8;

        private boolean colorPanelOpen = true;
        private TextFieldWidget hexInput;
        private String colorError = "";

        @Override
        public void init(MainMenuScreen screen, int cx, int cy, int cw, int ch) {
                int leftW = colorPanelOpen ? (cw - PANEL_W - 8) : cw;

                if (colorPanelOpen) {
                        int btnX = cx + PREVIEW_SIZE + 8;

                        CustomButton colorButton = new CustomButton(btnX, cy + 2, 70, 18,
                                        Text.literal("Color"), btn -> {
                                                colorPanelOpen = !colorPanelOpen;
                                                colorError = "";
                                                screen.rebuildMenu();
                                        });
                        colorButton.setSelectedInstant(colorPanelOpen);
                        screen.addUIElement(colorButton);

                        CustomButton btnRainbow = new CustomButton(btnX, cy + 22, 70, 18,
                                        Text.translatable("gui.tcveinminer.color.rainbow"),
                                        btn -> {
                                                screen.getState().colorList.clear();
                                                screen.getState().colorList.add(new int[]{255, 0, 0});     // Red
                                                screen.getState().colorList.add(new int[]{255, 127, 0});   // Orange
                                                screen.getState().colorList.add(new int[]{255, 255, 0});   // Yellow
                                                screen.getState().colorList.add(new int[]{0, 255, 0});     // Green
                                                screen.getState().colorList.add(new int[]{0, 255, 255});   // Cyan
                                                screen.getState().colorList.add(new int[]{0, 0, 255});     // Blue
                                                screen.getState().colorList.add(new int[]{139, 0, 255});   // Purple
                                                screen.getState().colorDisabled = false;
                                                screen.getState().colorRainbow = false;
                                                screen.rebuildMenu();
                                        });
                        btnRainbow.setSelectedInstant(false);
                        screen.addUIElement(btnRainbow);

                        CustomButton btnDisable = new CustomButton(btnX, cy + 42, 70, 18,
                                        Text.translatable("gui.tcveinminer.color.disable"),
                                        btn -> {
                                                screen.getState().colorDisabled = true;
                                                screen.getState().colorRainbow = false;
                                                screen.rebuildMenu();
                                        });
                        btnDisable.setSelectedInstant(screen.getState().colorDisabled);
                        screen.addUIElement(btnDisable);
                } else {
                        int topX = cx + PREVIEW_SIZE + 8;
                        int topY = cy + 2;

                        CustomButton colorButton = new CustomButton(topX, topY, 70, 18,
                                        Text.literal("Color"), btn -> {
                                                colorPanelOpen = !colorPanelOpen;
                                                colorError = "";
                                                screen.rebuildMenu();
                                        });
                        colorButton.setSelectedInstant(colorPanelOpen);
                        screen.addUIElement(colorButton);

                        CustomButton btnRainbow = new CustomButton(topX + 76, topY, 70, 18,
                                        Text.translatable("gui.tcveinminer.color.rainbow"),
                                        btn -> {
                                                screen.getState().colorList.clear();
                                                screen.getState().colorList.add(new int[]{255, 0, 0});     // Red
                                                screen.getState().colorList.add(new int[]{255, 127, 0});   // Orange
                                                screen.getState().colorList.add(new int[]{255, 255, 0});   // Yellow
                                                screen.getState().colorList.add(new int[]{0, 255, 0});     // Green
                                                screen.getState().colorList.add(new int[]{0, 255, 255});   // Cyan
                                                screen.getState().colorList.add(new int[]{0, 0, 255});     // Blue
                                                screen.getState().colorList.add(new int[]{139, 0, 255});   // Purple
                                                screen.getState().colorDisabled = false;
                                                screen.getState().colorRainbow = false;
                                                screen.rebuildMenu();
                                        });
                        btnRainbow.setSelectedInstant(false);
                        screen.addUIElement(btnRainbow);

                        CustomButton btnDisable = new CustomButton(topX + 152, topY, 70, 18,
                                        Text.translatable("gui.tcveinminer.color.disable"),
                                        btn -> {
                                                screen.getState().colorDisabled = true;
                                                screen.getState().colorRainbow = false;
                                                screen.rebuildMenu();
                                        });
                        btnDisable.setSelectedInstant(screen.getState().colorDisabled);
                        screen.addUIElement(btnDisable);
                }

                if (colorPanelOpen) {
                        int panelX = cx + cw - PANEL_W;
                        int panelY = cy;
                        initColorPanel(screen, panelX, panelY);
                }

                int sliderY = cy + PREVIEW_SIZE + 24;
                int labelW = 84;
                screen.addUIElement(new ThicknessSlider(cx + labelW, sliderY, leftW - labelW, 14,
                                screen.getState(), screen.getState().outlineThickness));

                int timeSliderY = sliderY + 20;
                screen.addUIElement(new TransitionTimeSlider(cx + labelW, timeSliderY, leftW - labelW, 14,
                                screen.getState(), screen.getState().colorTransitionTime));

                int segmentSliderY = timeSliderY + 20;
                screen.addUIElement(new net.minecraft.client.gui.widget.SliderWidget(cx + labelW, segmentSliderY, leftW - labelW, 14,
                                Text.literal(String.format("%.1f", screen.getState().segmentLength)),
                                (screen.getState().segmentLength - 0.1) / 9.9) {
                        @Override protected void updateMessage() {
                                float t = 0.1f + (float) (value * 9.9);
                                screen.getState().segmentLength = t;
                                setMessage(Text.literal(String.format("%.1f", t)));
                        }
                        @Override protected void applyValue() { updateMessage(); }
                });

                int smoothSliderY = segmentSliderY + 20;
                screen.addUIElement(new net.minecraft.client.gui.widget.SliderWidget(cx + labelW, smoothSliderY, leftW - labelW, 14,
                                Text.literal(String.format("%.2f", screen.getState().flowSmoothness)),
                                screen.getState().flowSmoothness) {
                        @Override protected void updateMessage() {
                                float t = (float) value;
                                screen.getState().flowSmoothness = t;
                                setMessage(Text.literal(String.format("%.2f", t)));
                        }
                        @Override protected void applyValue() { updateMessage(); }
                });

                int flowBtnY = smoothSliderY + 20;
                CustomButton flowBtn = new CustomButton(cx, flowBtnY, leftW, 14,
                                Text.literal("Flow Animation: " + (screen.getState().enableFlowAnimation ? "ON" : "OFF")),
                                btn -> {
                                        screen.getState().enableFlowAnimation = !screen.getState().enableFlowAnimation;
                                        screen.rebuildMenu();
                                });
                flowBtn.setSelectedInstant(screen.getState().enableFlowAnimation);
                screen.addUIElement(flowBtn);
        }

        private void initColorPanel(MainMenuScreen screen, int panelX, int panelY) {
                String currentHex = "#" + ColorManager.toHex(
                                screen.getState().colorR,
                                screen.getState().colorG,
                                screen.getState().colorB);

                if (hexInput == null) {
                        hexInput = new TextFieldWidget(screen.getTextRenderer(),
                                        panelX + 8, panelY + 20, 80, 16, Text.literal("Hex"));
                        hexInput.setMaxLength(7);
                }
                hexInput.setX(panelX + 8);
                hexInput.setY(panelY + 20);
                hexInput.setWidth(80);
                if (!hexInput.isFocused()) {
                        hexInput.setText(currentHex);
                }
                screen.addUIElement(hexInput);

                // Add button to add to colorList
                screen.addUIElement(new AmberButton(panelX + 92, panelY + 20, 32, 16,
                                Text.literal("Add"), btn -> addHex(screen)));

                // Apply button to set single color
                screen.addUIElement(new AmberButton(panelX + 128, panelY + 20, 44, 16,
                                Text.literal("Apply"), btn -> applyHex(screen)));

                // Clear button to clear colorList
                screen.addUIElement(new AmberButton(panelX + 172, panelY + 90, 48, 12,
                                Text.literal("Clear"), btn -> {
                                        screen.getState().colorList.clear();
                                        screen.rebuildMenu();
                                }));

                int gridX = panelX + 8;
                int gridY = panelY + 46;
                for (int i = 0; i < ColorManager.PRESET_COUNT; i++) {
                        final int ri = ColorManager.PRESET_RGB[i][0];
                        final int gi = ColorManager.PRESET_RGB[i][1];
                        final int bi = ColorManager.PRESET_RGB[i][2];

                        int col = i % PRESET_COLS;
                        int row = i / PRESET_COLS;
                        int bx = gridX + col * (PRESET_CELL + PRESET_GAP);
                        int by = gridY + row * (PRESET_CELL + PRESET_GAP);

                        screen.addUIElement(new CustomButton(bx, by, PRESET_CELL, PRESET_CELL,
                                        Text.empty(), btn -> {
                                                setColor(screen, ri, gi, bi);
                                                if (hexInput != null) {
                                                        hexInput.setText("#" + ColorManager.toHex(ri, gi, bi));
                                                }
                                        }) {
                                @Override
                                public void renderWidget(DrawContext ctx, int mx, int my, float delta) {
                                        boolean selected = !screen.getState().colorRainbow
                                                         && !screen.getState().colorDisabled
                                                         && screen.getState().colorR == ri
                                                         && screen.getState().colorG == gi
                                                         && screen.getState().colorB == bi;

                                        int border = selected ? ThemeColors.GOLD
                                                         : (isHovered() ? 0xFFFFFFFF : 0xFF334155);
                                        ctx.fill(getX(), getY(), getX() + width, getY() + height, 0xFF0B1120);
                                        ctx.fill(getX() + 1, getY() + 1,
                                                        getX() + width - 1, getY() + height - 1,
                                                        0xFF000000 | (ri << 16) | (gi << 8) | bi);
                                        DrawHelper.drawSolidBorder(ctx, getX(), getY(), width, height, border);
                                        if (selected) {
                                                ctx.fill(getX() - 1, getY() - 1,
                                                                getX() + width + 1, getY() + height + 1,
                                                                0x33D8A15B);
                                        }
                                }
                        });
                }

                // Render current colors in state.colorList as small buttons
                int listX = panelX + 8;
                int listY = panelY + 102;
                for (int i = 0; i < screen.getState().colorList.size(); i++) {
                        final int index = i;
                        int[] rgb = screen.getState().colorList.get(i);
                        int ri = rgb[0];
                        int gi = rgb[1];
                        int bi = rgb[2];

                        int col = i % 10;
                        int row = i / 10;
                        int bx = listX + col * (PRESET_CELL + PRESET_GAP);
                        int by = listY + row * (PRESET_CELL + PRESET_GAP);

                        screen.addUIElement(new CustomButton(bx, by, PRESET_CELL, PRESET_CELL,
                                        Text.empty(), btn -> {
                                                screen.getState().colorList.remove(index);
                                                screen.rebuildMenu();
                                        }) {
                                @Override
                                public void renderWidget(DrawContext ctx, int mx, int my, float delta) {
                                        boolean hov = isHovered();
                                        int border = hov ? 0xFFFF0000 : 0xFF334155;
                                        ctx.fill(getX(), getY(), getX() + width, getY() + height, 0xFF000000 | (ri << 16) | (gi << 8) | bi);
                                        DrawHelper.drawSolidBorder(ctx, getX(), getY(), width, height, border);
                                        if (hov) {
                                                // Tiny minus symbol when hovered
                                                ctx.fill(getX() + 4, getY() + 7, getX() + 12, getY() + 9, 0xFFFFFFFF);
                                        }
                                }
                        });
                }
        }

        private void applyHex(MainMenuScreen screen) {
                int[] rgb = ColorManager.fromHex(hexInput == null ? "" : hexInput.getText());
                if (rgb == null) {
                        colorError = "Invalid hex";
                        return;
                }
                setColor(screen, rgb[0], rgb[1], rgb[2]);
                colorError = "";
        }

        private void addHex(MainMenuScreen screen) {
                int[] rgb = ColorManager.fromHex(hexInput == null ? "" : hexInput.getText());
                if (rgb == null) {
                        colorError = "Invalid hex";
                        return;
                }
                if (screen.getState().colorList.size() >= 20) {
                        colorError = "Max 20 colors";
                        return;
                }
                screen.getState().colorList.add(rgb);
                screen.getState().colorRainbow = false;
                screen.getState().colorDisabled = false;
                colorError = "";
                screen.rebuildMenu();
        }

        private void setColor(MainMenuScreen screen, int r, int g, int b) {
                screen.getState().colorR = r;
                screen.getState().colorG = g;
                screen.getState().colorB = b;
                screen.getState().colorRainbow = false;
                screen.getState().colorDisabled = false;
        }

        @Override
        public void render(DrawContext ctx, MainMenuScreen screen, int cx, int cy,
                        int cw, int ch, int mouseX, int mouseY, float delta) {
                renderPreview(ctx, screen, cx, cy);

                String hexStr = "#" + ColorManager.toHex(
                                screen.getState().colorR,
                                screen.getState().colorG,
                                screen.getState().colorB);

                int activeColor;
                if (screen.getState().colorDisabled) {
                        activeColor = ThemeColors.TEXT_DIM;
                } else if (screen.getState().colorRainbow) {
                        activeColor = ThemeColors.PURPLE_TEXT;
                } else if (screen.getState().colorList != null && !screen.getState().colorList.isEmpty()) {
                        int n = screen.getState().colorList.size();
                        if (n == 1) {
                                int[] rgb = screen.getState().colorList.get(0);
                                activeColor = 0xFF000000 | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
                        } else {
                                float transitionMs = screen.getState().colorTransitionTime * 1000f;
                                if (transitionMs <= 0) transitionMs = 1000f;
                                double totalMs = n * transitionMs;
                                double progress = System.currentTimeMillis() % totalMs;
                                int index = (int)(progress / transitionMs);
                                int nextIndex = (index + 1) % n;
                                float t = (float)((progress % transitionMs) / transitionMs);

                                int[] rgb1 = screen.getState().colorList.get(index);
                                int[] rgb2 = screen.getState().colorList.get(nextIndex);
                                int r = (int)(rgb1[0] + (rgb2[0] - rgb1[0]) * t);
                                int g = (int)(rgb1[1] + (rgb2[1] - rgb1[1]) * t);
                                int b = (int)(rgb1[2] + (rgb2[2] - rgb1[2]) * t);
                                activeColor = 0xFF000000 | (r << 16) | (g << 8) | b;
                        }
                } else {
                        activeColor = 0xFF000000 | (screen.getState().colorR << 16)
                                        | (screen.getState().colorG << 8)
                                        | screen.getState().colorB;
                }

                String statusStr = screen.getState().colorDisabled ? "DISABLED"
                                : screen.getState().colorRainbow ? "RAINBOW"
                                : (screen.getState().colorList != null && !screen.getState().colorList.isEmpty() ? "PLAYLIST (" + screen.getState().colorList.size() + ")" : hexStr);

                int leftW = colorPanelOpen ? (cw - PANEL_W - 8) : cw;

                if (colorPanelOpen) {
                        int statusX = cx;
                        int statusY = cy + 54;
                        ctx.drawTextWithShadow(screen.getTextRenderer(), statusStr, statusX, statusY, activeColor);
                } else {
                        int topX = cx + PREVIEW_SIZE + 8;
                        int statusY = cy + 28;
                        ctx.drawTextWithShadow(screen.getTextRenderer(), statusStr, topX, statusY, activeColor);
                }

                if (colorPanelOpen) {
                        int panelX = cx + cw - PANEL_W;
                        int panelY = cy;
                        renderColorPanel(ctx, screen, panelX, panelY);
                }

                int sliderY = cy + PREVIEW_SIZE + 24;
                DrawHelper.drawDivider(ctx, cx, sliderY - 8, leftW);

                String widthLabel = "Width";
                String widthValue = String.format("%.1f", screen.getState().outlineThickness);
                ctx.drawTextWithShadow(screen.getTextRenderer(), widthLabel, cx, sliderY + 3, ThemeColors.TEXT_LABEL);
                int vw = screen.getTextRenderer().getWidth(widthValue);
                ctx.drawTextWithShadow(screen.getTextRenderer(), widthValue, cx + 80 - vw, sliderY + 3, 0xFFFFFFFF);

                int timeSliderY = sliderY + 20;
                String timeLabel = "Interval";
                String timeValue = String.format("%.1fs", screen.getState().colorTransitionTime);
                ctx.drawTextWithShadow(screen.getTextRenderer(), timeLabel, cx, timeSliderY + 3, ThemeColors.TEXT_LABEL);
                int tw = screen.getTextRenderer().getWidth(timeValue);
                ctx.drawTextWithShadow(screen.getTextRenderer(), timeValue, cx + 80 - tw, timeSliderY + 3, 0xFFFFFFFF);

                int segmentSliderY = timeSliderY + 20;
                String segmentLabel = "Segment";
                String segmentValue = String.format("%.1f", screen.getState().segmentLength);
                ctx.drawTextWithShadow(screen.getTextRenderer(), segmentLabel, cx, segmentSliderY + 3, ThemeColors.TEXT_LABEL);
                int sw = screen.getTextRenderer().getWidth(segmentValue);
                ctx.drawTextWithShadow(screen.getTextRenderer(), segmentValue, cx + 80 - sw, segmentSliderY + 3, 0xFFFFFFFF);

                int smoothSliderY = segmentSliderY + 20;
                String smoothLabel = "Smooth";
                String smoothValue = String.format("%.2f", screen.getState().flowSmoothness);
                ctx.drawTextWithShadow(screen.getTextRenderer(), smoothLabel, cx, smoothSliderY + 3, ThemeColors.TEXT_LABEL);
                int smw = screen.getTextRenderer().getWidth(smoothValue);
                ctx.drawTextWithShadow(screen.getTextRenderer(), smoothValue, cx + 80 - smw, smoothSliderY + 3, 0xFFFFFFFF);
        }

        private void renderColorPanel(DrawContext ctx, MainMenuScreen screen, int panelX, int panelY) {
                DrawHelper.drawCard(ctx, panelX, panelY, PANEL_W, PANEL_H);
                ctx.drawTextWithShadow(screen.getTextRenderer(), "Enter color", panelX + 8, panelY + 8, 0xFFFFFFFF);
                ctx.drawTextWithShadow(screen.getTextRenderer(), "Basic colors", panelX + 8, panelY + 38,
                                ThemeColors.TEXT_LABEL);

                int previewColor = 0xFF000000 | (screen.getState().colorR << 16)
                                | (screen.getState().colorG << 8)
                                | screen.getState().colorB;
                ctx.fill(panelX + 176, panelY + 20, panelX + 220, panelY + 36, previewColor);
                DrawHelper.drawSolidBorder(ctx, panelX + 176, panelY + 20, 44, 16, ThemeColors.BORDER_DEFAULT);

                ctx.drawTextWithShadow(screen.getTextRenderer(), "Color List", panelX + 8, panelY + 92,
                                ThemeColors.TEXT_LABEL);
                String countStr = screen.getState().colorList.size() + "/20";
                int countW = screen.getTextRenderer().getWidth(countStr);
                ctx.drawTextWithShadow(screen.getTextRenderer(), countStr, panelX + 164 - countW, panelY + 92,
                                ThemeColors.TEXT_DIM);

                if (!colorError.isEmpty()) {
                        ctx.drawTextWithShadow(screen.getTextRenderer(), colorError, panelX + 110, panelY + 8,
                                        ThemeColors.TEXT_ERROR);
                }
        }

        private void renderPreview(DrawContext ctx, MainMenuScreen screen, int bpX, int bpY) {
                int s = PREVIEW_SIZE;
                DrawHelper.drawCard(ctx, bpX, bpY, s, s);

                if (screen.getState().colorDisabled) {
                        String off = "OFF";
                        int tw = screen.getTextRenderer().getWidth(off);
                        ctx.drawTextWithShadow(screen.getTextRenderer(), off,
                                        bpX + (s - tw) / 2, bpY + (s - 8) / 2, ThemeColors.TEXT_DIM);
                        return;
                }

                long now = System.currentTimeMillis();
                int r, g, b;
                if (screen.getState().colorRainbow) {
                        int[] rgb = ColorManager.rainbowRGB(now);
                        r = rgb[0];
                        g = rgb[1];
                        b = rgb[2];
                } else if (screen.getState().colorList != null && !screen.getState().colorList.isEmpty()) {
                        int n = screen.getState().colorList.size();
                        if (n == 1) {
                                int[] rgb = screen.getState().colorList.get(0);
                                r = rgb[0];
                                g = rgb[1];
                                b = rgb[2];
                        } else {
                                float transitionMs = screen.getState().colorTransitionTime * 1000f;
                                if (transitionMs <= 0) transitionMs = 1000f;
                                double totalMs = n * transitionMs;
                                double progress = now % totalMs;
                                int index = (int)(progress / transitionMs);
                                int nextIndex = (index + 1) % n;
                                float t = (float)((progress % transitionMs) / transitionMs);

                                int[] rgb1 = screen.getState().colorList.get(index);
                                int[] rgb2 = screen.getState().colorList.get(nextIndex);
                                r = (int)(rgb1[0] + (rgb2[0] - rgb1[0]) * t);
                                g = (int)(rgb1[1] + (rgb2[1] - rgb1[1]) * t);
                                b = (int)(rgb1[2] + (rgb2[2] - rgb1[2]) * t);
                        }
                } else {
                        r = screen.getState().colorR;
                        g = screen.getState().colorG;
                        b = screen.getState().colorB;
                }

                int alpha = screen.getState().outlineAlpha;
                int fillColor = (alpha << 24) | (r << 16) | (g << 8) | b;
                int lineColor = 0xFF000000 | (r << 16) | (g << 8) | b;

                int innerX = bpX + 8;
                int innerY = bpY + 8;
                int innerW = s - 16;
                int innerH = s - 16;
                ctx.fill(innerX, innerY, innerX + innerW, innerY + innerH, fillColor);

                int thickness = (int) Math.max(1, screen.getState().outlineThickness / 2f);
                for (int t = 0; t < thickness; t++) {
                        DrawHelper.drawSolidBorder(ctx,
                                        innerX - t, innerY - t,
                                        innerW + t * 2, innerH + t * 2,
                                        lineColor);
                }

                int cx2 = innerX - thickness;
                int cy2 = innerY - thickness;
                int ex = innerX + innerW + thickness - 1;
                int ey = innerY + innerH + thickness - 1;
                ctx.fill(cx2 - 1, cy2 - 1, cx2 + 2, cy2 + 2, lineColor);
                ctx.fill(ex - 1, cy2 - 1, ex + 2, cy2 + 2, lineColor);
                ctx.fill(cx2 - 1, ey - 1, cx2 + 2, ey + 2, lineColor);
                ctx.fill(ex - 1, ey - 1, ex + 2, ey + 2, lineColor);
        }
}
