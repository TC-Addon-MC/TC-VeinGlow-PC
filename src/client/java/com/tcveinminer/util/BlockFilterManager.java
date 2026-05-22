package com.tcveinminer.util;

import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BlockFilterManager {
    
    // Cache kết quả tìm kiếm để không phải quét lại Registry liên tục mỗi frame
    private static List<Identifier> searchCache = new ArrayList<>();
    private static String lastQuery = "";

    /**
     * Tìm kiếm block trực tiếp trong Registry (Hỗ trợ modded, contains, namespace).
     * Chỉ gọi hàm này khi user THAY ĐỔI text trong ô nhập liệu.
     */
    public static void updateSearch(String query) {
        String lowerQuery = query.toLowerCase().trim();
        if (lowerQuery.equals(lastQuery)) return;
        lastQuery = lowerQuery;

        if (lowerQuery.isEmpty()) {
            searchCache.clear();
            return;
        }

        // Quét toàn bộ ID trong game (cả vanilla và mod)
        searchCache = Registries.BLOCK.getIds().stream()
                .filter(id -> id.toString().contains(lowerQuery) || id.getPath().contains(lowerQuery))
                .limit(100) // Giới hạn kết quả để tránh lag UI khi gõ 1 chữ cái
                .collect(Collectors.toList());
    }

    public static List<Identifier> getSearchCache() {
        return searchCache;
    }

    /**
     * Xác thực xem input có phải là một Block hợp lệ đang tồn tại trong game hay không.
     * Chuyển đổi String -> Identifier an toàn.
     */
    public static Identifier validateAndParseBlock(String input) {
        String trim = input.trim().toLowerCase();
        
        // Tự động thêm minecraft: nếu user chỉ gõ tên block
        if (!trim.contains(":")) {
            trim = "minecraft:" + trim;
        }

        Identifier id = Identifier.tryParse(trim);
        if (id != null && Registries.BLOCK.containsId(id)) {
            // Chặn thêm block Air hoặc các block lỗi
            Block block = Registries.BLOCK.get(id);
            if (block != net.minecraft.block.Blocks.AIR) {
                return id;
            }
        }
        return null;
    }
}