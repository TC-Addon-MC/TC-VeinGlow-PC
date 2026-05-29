package com.tcveinminer.engine.skill;

import com.tcveinminer.config.ModConfig;
import com.tcveinminer.engine.session.ActionSession;
import com.tcveinminer.logic.HudNotifier;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

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
        ItemEnchantmentsComponent enchants = stack.getOrDefault(
                DataComponentTypes.ENCHANTMENTS,
                ItemEnchantmentsComponent.DEFAULT);

        int fortuneLevel = 0;
        boolean hasSilkTouch = false;

        for (RegistryEntry<Enchantment> entry : enchants.getEnchantments()) {
            String path = entry.getKey()
                    .map(k -> k.getValue().getPath())
                    .orElse("");
            int level = enchants.getLevel(entry);
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
        if (!stack.isDamageable()) return false;
        return (stack.getMaxDamage() - stack.getDamage()) <= threshold;
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
    public static int findReplacementTool(ServerPlayerEntity player, Item oldItem, String originalSig, int protectThreshold) {
        PlayerInventory inv = player.getInventory();
        int currentSlot = inv.selectedSlot;
        Class<?> toolClass = oldItem.getClass();

        // ── Priority 1: Cùng item + cùng enchant sig (hotbar) ────────────
        for (int i = 0; i < 9; i++) {
            if (i == currentSlot) continue;
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty() || stack.getItem() != oldItem || !originalSig.equals(getSpecialEnchantSig(stack))) continue;
            if (protectThreshold >= 0 && isToolNearBreaking(stack, protectThreshold)) continue;
            return i;
        }
        // ── Priority 1b: (inventory thường) ──────────────────────────────
        for (int i = 9; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty() || stack.getItem() != oldItem || !originalSig.equals(getSpecialEnchantSig(stack))) continue;
            if (protectThreshold >= 0 && isToolNearBreaking(stack, protectThreshold)) continue;
            return i;
        }

        // ── Priority 2: Cùng class + cùng enchant sig (hotbar) ───────────
        for (int i = 0; i < 9; i++) {
            if (i == currentSlot) continue;
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty() || stack.getItem().getClass() != toolClass || !originalSig.equals(getSpecialEnchantSig(stack))) continue;
            if (protectThreshold >= 0 && isToolNearBreaking(stack, protectThreshold)) continue;
            return i;
        }
        // ── Priority 2b: (inventory thường) ──────────────────────────────
        for (int i = 9; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
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
    public static boolean equipToolFromSlot(ServerPlayerEntity player, int slot) {
        PlayerInventory inv = player.getInventory();
        if (slot < 0 || slot >= 36) return false;

        int currentSlot = inv.selectedSlot; // giữ nguyên tay, KHÔNG đổi selectedSlot
        ItemStack currentStack = inv.getStack(currentSlot).copy();
        ItemStack targetStack  = inv.getStack(slot).copy();

        inv.setStack(currentSlot, targetStack); // đặt tool mới vào tay
        inv.setStack(slot, currentStack);        // đặt tool cũ/vỡ về slot nguồn
        return true;
    }

    // ── Main Hook ─────────────────────────────────────────────────────────

    /**
     * Hook chính — gọi sau mỗi block break trong AbstractActionEngine.
     */
    public static ToolAction handleToolState(
            ServerPlayerEntity player, ModConfig config,
            Item initialItem, ActionSession session) {

        ItemStack mainHand = player.getMainHandStack();
        String enchantSig = session.getInitialEnchantSig();

        int thresholdForSwap = config.enableToolProtectSkill ? config.toolProtectThreshold : -1;

        // ── Tool đã vỡ hoàn toàn ─────────────────────────────────────────
        if (mainHand.isEmpty()) {
            if (config.enableToolSwapSkill) {
                int newSlot = findReplacementTool(player, initialItem, enchantSig, thresholdForSwap);
                if (newSlot != -1) {
                    equipToolFromSlot(player, newSlot);
                    Item newItem = player.getMainHandStack().getItem();
                    String newSig = getSpecialEnchantSig(player.getMainHandStack());
                    session.setInitialItem(newItem);
                    session.setInitialEnchantSig(newSig);
                    HudNotifier.notifyAt = System.currentTimeMillis() + 2500;
                    player.sendMessage(Text.translatable("msg.tcveinminer.tool_swap_broken", newItem.getName().getString()), true);
                    return ToolAction.SWAPPED;
                } else {
                    HudNotifier.notifyAt = System.currentTimeMillis() + 2500;
                    player.sendMessage(Text.translatable("msg.tcveinminer.tool_swap_no_tool_broken"), true);
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
                    Item newItem = player.getMainHandStack().getItem();
                    String newSig = getSpecialEnchantSig(player.getMainHandStack());
                    session.setInitialItem(newItem);
                    session.setInitialEnchantSig(newSig);
                    HudNotifier.notifyAt = System.currentTimeMillis() + 2500;
                    player.sendMessage(Text.translatable("msg.tcveinminer.tool_swap_protected", newItem.getName().getString()), true);
                    return ToolAction.SWAPPED;
                } else if (config.enableToolProtectSkill) {
                    // Swap bật nhưng không có tool phù hợp, protect bật → dừng bảo vệ tool
                    HudNotifier.notifyAt = System.currentTimeMillis() + 2500;
                    player.sendMessage(Text.translatable("msg.tcveinminer.tool_swap_no_tool_protected"), true);
                    return ToolAction.STOP;
                }
                // Swap bật, không có tool thay thế, protect tắt → tiếp tục đến khi vỡ hẳn
            } else if (config.enableToolProtectSkill) {
                // Chỉ protect, không swap → dừng
                HudNotifier.notifyAt = System.currentTimeMillis() + 2500;
                player.sendMessage(Text.translatable("msg.tcveinminer.tool_protected"), true);
                return ToolAction.STOP;
            }
            // Cả 2 tắt → tiếp tục bình thường
        }

        return ToolAction.CONTINUE;
    }
}
