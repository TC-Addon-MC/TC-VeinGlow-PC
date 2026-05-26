package com.tcveinminer.util;

import com.tcveinminer.engine.strategy.FilterModeManager;
import net.minecraft.util.Identifier;

import java.util.List;

@Deprecated
public class BlockFilterManager {

    public static void updateSearch(String query) {
        FilterModeManager.updateBlockSearch(query);
    }

    public static List<Identifier> getSearchCache() {
        return FilterModeManager.getBlockSearchCache();
    }

    public static Identifier validateAndParseBlock(String input) {
        return FilterModeManager.validateAndParseBlock(input);
    }
}
