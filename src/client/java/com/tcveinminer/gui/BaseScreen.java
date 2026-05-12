package com.tcveinminer.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public abstract class BaseScreen extends Screen {

    protected final Screen parent;
    protected final int W, H, HEADER_H = 24;
    protected int x, y;

    protected BaseScreen(Screen parent, String title, int w, int h) {
        super(Text.literal(title));
        this.parent = parent; this.W = w; this.H = h;
    }

    @Override
    protected final void init() {
        x = (width - W) / 2; y = (height - H) / 2;
        initWidgets();
    }

    protected abstract void initWidgets();

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx, mx, my, delta);
        Draw.panel(ctx, x, y, W, H);
        Draw.header(ctx, x, y, W, HEADER_H);
        renderContent(ctx, mx, my, delta);
        super.render(ctx, mx, my, delta);
    }

    protected abstract void renderContent(DrawContext ctx, int mx, int my, float delta);

    protected void drawTitle(DrawContext ctx, String t) {
        ctx.drawTextWithShadow(textRenderer, t, x+10, y+8, TC.TXT_TITLE);
    }

    protected void label(DrawContext ctx, String t, int lx, int ly) {
        ctx.drawTextWithShadow(textRenderer, t, lx, ly, TC.TXT_LABEL);
    }

    protected void value(DrawContext ctx, String t, int lx, int ly) {
        ctx.drawTextWithShadow(textRenderer, t, lx, ly, TC.TXT_VALUE);
    }

    protected ButtonWidget backBtn(int bx, int by, int bw, String lbl) {
        return ButtonWidget.builder(Text.literal(lbl), b -> client.setScreen(parent))
            .dimensions(bx, by, bw, 18).build();
    }

    @Override public boolean shouldPause() { return false; }
}
