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
// Trong file BaseScreen.java



    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Nếu bạn muốn làm nền game tối đi một chút (như bản menu tròn)
        // thì thêm dòng này ở ĐẦU hàm, còn không thì bỏ qua:
        // ctx.fill(0, 0, width, height, 0x44000000);

        // 1. Vẽ Panel và Header của mod trước
        Draw.panel(ctx, x, y, W, H);
        Draw.header(ctx, x, y, W, HEADER_H);

        // 2. Vẽ nội dung riêng của từng screen
        renderContent(ctx, mx, my, delta);

        // 3. Gọi super sau cùng để vẽ các nút bấm (nó sẽ nằm đè lên panel)
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
    // Trong file BaseScreen.java
    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        // Để trống hoàn toàn để không làm mờ/tối nền game
    }
    protected ButtonWidget backBtn(int bx, int by, int bw, String lbl) {
        return ButtonWidget.builder(Text.literal(lbl), b -> client.setScreen(parent))
            .dimensions(bx, by, bw, 18).build();
    }

    @Override public boolean shouldPause() { return false; }
}
