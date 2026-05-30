package com.tcveinminer.engine.skill;

import com.tcveinminer.config.ModConfig;
import com.tcveinminer.engine.session.ActionSession;
import com.tcveinminer.logic.HudNotifier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

public final class ToolManagerSkill {

    private ToolManagerSkill() {}

    public enum ToolAction { CONTINUE, SWAPPED, STOP }

    // ── Enchant Signature ──────────────────────────────────────────────────

    /**
     * Lấy "chữ ký enchant đặc biệt" của item.
     * Chỉ xét Fortune và Silk Touch vì chúng ảnh hưởng đến loot drop.
     * Ví dụ: "fortune:3", "silk_touch:1", "" (không có enchant đặc biệt).
     */
    public static String getSpecialEnchantSig(ItemStack stack) {
        if (stack.isEmpty()) return "";
        ItemEnchantments enchants = stack.getOrDefault(
                DataComponents.ENCHANTMENTS,
                ItemEnchantments.EMPTY);

        int fortuneLevel = 0;
        boolean hasSilkTouch = false;

        for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<Holder<Enchantment>> entry : enchants.entrySet()) {
            String path = entry.getKey().unwrapKey()
                    .map(k -> k.location().getPath())
                    .orElse("");
            int level = entry.getIntValue();
            if ("fortune".equals(path)) fortuneLevel = level;
            else if ("silk_touch".equals(path)) hasSilkTouch = true;
        }

        if (hasSilkTouch) return "silk_touch:1";
        if (fortuneLevel > 0) return "fortune:" + fortuneLevel;
        return "";
    }

    // ── Tool State Check ───────────────────────────────────────────────────

    /** Kiểm tra xem tool hiện tại có sắp vỡ không (remaining ≤ threshold) */
    public static boolean isToolNearBreaking(ItemStack stack, int threshold) {
        if (!stack.isDamageableItem()) return false;
        return (stack.getMaxDamage() - stack.getDamageValue()) <= threshold;
    }

    // ── Replacement Search ────────────────────────────────────────────────

    /**
     * Tìm tool thay thế trong inventory với ưu tiên enchant đặc biệt.
     *
     * <p>Ưu tiên tìm kiếm (từ cao đến thấp):
     * <ol>
     *   <li>Cùng item + cùng enchant signature (hotbar → inventory)</li>
     *   <li>Cùng class + cùng enchant signature (hotbar → inventory)</li>
     *   <li>Cùng item + không có enchant đặc biệt nào (chỉ khi originalSig == "") </li>
     *   <li>Cùng class + không có enchant đặc biệt nào (chỉ khi originalSig == "") </li>
     * </ol>
     *
     * <p>Nếu {@code originalSig} là "" (tool gốc không có enchant đặc biệt),
     * bước 1–2 sẽ tìm tool cũng không có enchant đặc biệt.
     * Bước 3–4 chỉ là fallback thừa (sig "" = sig "" nên đã khớp ở bước 1–2).
     * Thực tế logic này đảm bảo: tool có Fortune không bao giờ swap sang tool không có Fortune.
     *
     * @param player      người chơi
     * @param oldItem     item cần thay thế
     * @param originalSig chữ ký enchant đặc biệt của tool gốc (lấy từ session)
     * @param protectThreshold nếu > -1, bỏ qua những tool có độ bền còn lại <= threshold
     * @return slot index [0,35] hoặc -1 nếu không tìm thấy
     */
    public static int findReplacementTool(ServerPlayer player, Item oldItem, String originalSig, int protectThreshold) {
        Inventory inv = player.getInventory();
        int currentSlot = inv.selected;
        Class<?> toolClass = oldItem.getClass();

        // ── Priority 1: Cùng item + cùng enchant sig (hotbar) ────────────
        for (int i = 0; i < 9; i++) {
            if (i == currentSlot) continue;
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || stack.getItem() != oldItem || !originalSig.equals(getSpecialEnchantSig(stack))) continue;
            if (protectThreshold >= 0 && isToolNearBreaking(stack, protectThreshold)) continue;
            return i;
        }
        // ── Priority 1b: (inventory thường) ──────────────────────────────
        for (int i = 9; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || stack.getItem() != oldItem || !originalSig.equals(getSpecialEnchantSig(stack))) continue;
            if (protectThreshold >= 0 && isToolNearBreaking(stack, protectThreshold)) continue;
            return i;
        }

        // ── Priority 2: Cùng class + cùng enchant sig (hotbar) ───────────
        for (int i = 0; i < 9; i++) {
            if (i == currentSlot) continue;
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || stack.getItem().getClass() != toolClass || !originalSig.equals(getSpecialEnchantSig(stack))) continue;
            if (protectThreshold >= 0 && isToolNearBreaking(stack, protectThreshold)) continue;
            return i;
        }
        // ── Priority 2b: (inventory thường) ──────────────────────────────
        for (int i = 9; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || stack.getItem().getClass() != toolClass || !originalSig.equals(getSpecialEnchantSig(stack))) continue;
            if (protectThreshold >= 0 && isToolNearBreaking(stack, protectThreshold)) continue;
            return i;
        }

        return -1;
    }

    // ── Tool Equip ────────────────────────────────────────────────────────

    /**
     * Đưa item từ slot chỉ định VỀ slot tay hiện tại (selectedSlot không đổi).
     * Item cũ ở tay (vỡ/empty) sẽ được đặt sang slot nguồn.
     */
    public static boolean equipToolFromSlot(ServerPlayer player, int slot) {
        Inventory inv = player.getInventory();
        if (slot < 0 || slot >= 36) return false;

        int currentSlot = inv.selected; // giữ nguyên tay, KHÔNG đổi selectedSlot
        ItemStack currentStack = inv.getItem(currentSlot).copy();
        ItemStack targetStack  = inv.getItem(slot).copy();

        inv.setItem(currentSlot, targetStack); // đặt tool mới vào tay
        inv.setItem(slot, currentStack);        // đặt tool cũ/vỡ về slot nguồn
        return true;
    }

    // ── Main Hook ─────────────────────────────────────────────────────────

    /**
     * Hook chính — gọi sau mỗi block break trong AbstractActionEngine.
     */
    public static ToolAction handleToolState(
            ServerPlayer player, ModConfig config,
            Item initialItem, ActionSession session) {

        ItemStack mainHand = player.getMainHandItem();
        String enchantSig = session.getInitialEnchantSig();

        int thresholdForSwap = config.enableToolProtectSkill ? config.toolProtectThreshold : -1;

        // ── Tool đã vỡ hoàn toàn ─────────────────────────────────────────
        if (mainHand.isEmpty()) {
            if (config.enableToolSwapSkill) {
                int newSlot = findReplacementTool(player, initialItem, enchantSig, thresholdForSwap);
                if (newSlot != -1) {
                    equipToolFromSlot(player, newSlot);
                    Item newItem = player.getMainHandItem().getItem();
                    String newSig = getSpecialEnchantSig(player.getMainHandItem());
                    session.setInitialItem(newItem);
                    session.setInitialEnchantSig(newSig);
                    HudNotifier.notifyAt = System.currentTimeMillis() + 2500;
                    player.sendSystemMessage(Component.translatable("msg.tcveinminer.tool_swap_broken", newItem.getDescription().getString()), true);
                    return ToolAction.SWAPPED;
                } else {
                    HudNotifier.notifyAt = System.currentTimeMillis() + 2500;
                    player.sendSystemMessage(Component.translatable("msg.tcveinminer.tool_swap_no_tool_broken"), true);
                    return ToolAction.STOP;
                }
            }
            return ToolAction.STOP; // tool vỡ, không có swap skill → dừng
        }

        // ── Tool sắp vỡ ──────────────────────────────────────────────────
        if (isToolNearBreaking(mainHand, config.toolProtectThreshold)) {
            if (config.enableToolSwapSkill) {
                // Swap bật (dù protect có bật hay không) → cố đổi tool trước khi vỡ
                int newSlot = findReplacementTool(player, initialItem, enchantSig, thresholdForSwap);
                if (newSlot != -1) {
                    equipToolFromSlot(player, newSlot);
                    Item newItem = player.getMainHandItem().getItem();
                    String newSig = getSpecialEnchantSig(player.getMainHandItem());
                    session.setInitialItem(newItem);
                    session.setInitialEnchantSig(newSig);
                    HudNotifier.notifyAt = System.currentTimeMillis() + 2500;
                    player.sendSystemMessage(Component.translatable("msg.tcveinminer.tool_swap_protected", newItem.getDescription().getString()), true);
                    return ToolAction.SWAPPED;
                } else if (config.enableToolProtectSkill) {
                    // Swap bật nhưng không có tool phù hợp, protect bật → dừng bảo vệ tool
                    HudNotifier.notifyAt = System.currentTimeMillis() + 2500;
                    player.sendSystemMessage(Component.translatable("msg.tcveinminer.tool_swap_no_tool_protected"), true);
                    return ToolAction.STOP;
                }
                // Swap bật, không có tool thay thế, protect tắt → tiếp tục đến khi vỡ hẳn
            } else if (config.enableToolProtectSkill) {
                // Chỉ protect, không swap → dừng
                HudNotifier.notifyAt = System.currentTimeMillis() + 2500;
                player.sendSystemMessage(Component.translatable("msg.tcveinminer.tool_protected"), true);
                return ToolAction.STOP;
            }
            // Cả 2 tắt → tiếp tục bình thường
        }

        return ToolAction.CONTINUE;
    }
}
