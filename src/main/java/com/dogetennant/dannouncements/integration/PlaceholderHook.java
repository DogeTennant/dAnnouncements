package com.dogetennant.dannouncements.integration;

import com.dogetennant.dannouncements.util.LogUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Soft integration: resolves other plugins' %placeholders% in announcement text when PlaceholderAPI is present. */
public final class PlaceholderHook {

    private static boolean enabled;

    private PlaceholderHook() {}

    public static void init() {
        enabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        if (enabled) {
            LogUtil.info("PlaceholderAPI found - %placeholders% in announcements will be resolved.");
        }
    }

    public static String resolve(Player player, String text) {
        if (!enabled || text == null || text.isEmpty()) return text;
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    public static boolean isEnabled() {
        return enabled;
    }
}
