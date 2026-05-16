package com.tcveinminer.gui;

import com.tcveinminer.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.*;
import com.tcveinminer.config.ConfigManager;

public class BlockListScreen extends BaseScreen {

    private static final int LIST_Y=54, ROW_H=18, LIST_H=112;

    private final List<String> blocks = new ArrayList<>();
    private TextFieldWidget search, addField;
    private int scroll = 0;
    private boolean popup = false;
    private String err = null;

    public BlockListScreen(Screen parent) {
        super(parent, "Block List", 300, 260);
        blocks.addAll(ConfigManager.get().blacklistedBlocks); // Sửa ở đây
    }

    @Override
    protected void initWidgets() {
        search = new TextFieldWidget(textRenderer, x+8, y+LIST_Y-18, W-16, 14, Text.empty());
        search.setPlaceholder(Text.literal("Tìm kiếm..."));
        addDrawableChild(search);

        addDrawableChild(ButtonWidget.builder(Text.literal("+ THÊM"), b -> {
            popup=true; err=null;
            if (addField!=null) { addField.setText(""); addField.setVisible(true); }
        }).dimensions(x+8, y+LIST_Y+LIST_H+6, 70, 15).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("XÓA TẤT CẢ"), b -> {
            blocks.clear(); scroll=0;
        }).dimensions(x+84, y+LIST_Y+LIST_H+6, 90, 15).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("LƯU & QUAY LẠI"), b -> {
            ConfigManager.get().blacklistedBlocks.clear(); // Sửa ở đây
            ConfigManager.get().blacklistedBlocks.addAll(blocks); // Sửa ở đây
            ConfigManager.save(); // Sửa ở đây
            client.setScreen(parent);
        }).dimensions(x+W/2-70, y+H-26, 140, 18).build());

        addField = new TextFieldWidget(textRenderer, 0, 0, 200, 18, Text.empty());
        addField.setMaxLength(200); addField.setVisible(false);
        addDrawableChild(addField);
    }

    @Override
    protected void renderContent(DrawContext ctx, int mx, int my, float delta) {
        drawTitle(ctx, "📋 Danh Sách Block Đen");

        int lx=x+8, ly=y+LIST_Y;
        ctx.fill(lx, ly, lx+W-16, ly+LIST_H, TC.BG_ROW_A);
        Draw.rect(ctx, lx, ly, W-16, LIST_H, TC.B_INNER);

        List<String> f=filtered();
        int maxV=LIST_H/ROW_H, start=Math.max(0,Math.min(scroll,Math.max(0,f.size()-maxV)));

        ctx.enableScissor(lx, ly, lx+W-16, ly+LIST_H);
        for (int i=start; i<Math.min(f.size(),start+maxV); i++) {
            int ry=ly+(i-start)*ROW_H;
            boolean hov=mx>=lx&&mx<=lx+W-16&&my>=ry&&my<=ry+ROW_H;
            ctx.fill(lx,ry,lx+W-16,ry+ROW_H, hov?TC.BG_ROW_HOV:(i%2==0?TC.BG_ROW_A:TC.BG_ROW_B));
            label(ctx, f.get(i), lx+5, ry+5);
            boolean xh=mx>=lx+W-22&&mx<=lx+W-16&&my>=ry+3&&my<=ry+14;
            ctx.fill(lx+W-22,ry+3,lx+W-16,ry+14, xh?TC.OFF_BORDER:0xFF2A0A0A);
            ctx.drawTextWithShadow(textRenderer,"✕",lx+W-21,ry+4,TC.TXT_ERROR);
        }
        ctx.disableScissor();
        if (f.isEmpty()) label(ctx,"Danh sách trống",lx+8,ly+LIST_H/2-4);
        if (popup) drawPopup(ctx,mx,my);
    }

    private void drawPopup(DrawContext ctx, int mx, int my) {
        int pw=240,ph=72,px=x+(W-pw)/2,py=y+(H-ph)/2;
        addField.setX(px+10); addField.setY(py+24); addField.setWidth(pw-20);
        Draw.panel(ctx,px,py,pw,ph);
        ctx.drawTextWithShadow(textRenderer,"Nhập Block ID:",px+10,py+10,TC.TXT_TITLE);
        if (err!=null) ctx.drawTextWithShadow(textRenderer,err,px+10,py+45,TC.TXT_ERROR);
        boolean ah=mx>=px+10&&mx<=px+58&&my>=py+57&&my<=py+69;
        boolean ch=mx>=px+pw-58&&mx<=px+pw-10&&my>=py+57&&my<=py+69;
        Draw.btn(ctx,px+10,py+57,48,12,ah);
        Draw.btn(ctx,px+pw-58,py+57,48,12,ch);
        ctx.drawTextWithShadow(textRenderer,"THÊM",px+16,py+60,TC.BTN_TEXT);
        ctx.drawTextWithShadow(textRenderer,"HỦY",px+pw-52,py+60,TC.BTN_TEXT);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (popup) {
            int pw=240,ph=72,px=x+(W-pw)/2,py=y+(H-ph)/2;
            if (mx>=px+10&&mx<=px+58&&my>=py+57&&my<=py+69) { tryAdd(addField.getText().trim()); return true; }
            if (mx>=px+pw-58&&mx<=px+pw-10&&my>=py+57&&my<=py+69) { popup=false; addField.setVisible(false); return true; }
            return super.mouseClicked(mx,my,btn);
        }
        List<String> f=filtered(); int lx=x+8,ly=y+LIST_Y,maxV=LIST_H/ROW_H;
        int start=Math.max(0,Math.min(scroll,Math.max(0,f.size()-maxV)));
        for (int i=start;i<Math.min(f.size(),start+maxV);i++) {
            int ry=ly+(i-start)*ROW_H;
            if (mx>=lx+W-22&&mx<=lx+W-16&&my>=ry+3&&my<=ry+14) { blocks.remove(f.get(i)); return true; }
        }
        return super.mouseClicked(mx,my,btn);
    }

    private void tryAdd(String id) {
        if (id.isBlank()) { err="Không được để trống"; return; }
        try { if (!Registries.BLOCK.containsId(Identifier.of(id))) { err="Block không tồn tại"; return; } }
        catch (Exception e) { err="ID không hợp lệ"; return; }
        if (!blocks.contains(id)) blocks.add(id);
        popup=false; addField.setVisible(false); err=null;
    }

    private List<String> filtered() {
        String q=search!=null?search.getText().toLowerCase():"";
        return q.isBlank()?blocks:blocks.stream().filter(b->b.contains(q)).toList();
    }

    @Override
    public boolean mouseScrolled(double mx,double my,double h,double v) {
        scroll=Math.max(0,scroll-(int)v); return true;
    }
}
