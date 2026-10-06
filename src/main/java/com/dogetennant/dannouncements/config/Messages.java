package com.dogetennant.dannouncements.config;

import com.dogetennant.dannouncements.DAnnouncements;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.Map;

/** Reads command feedback strings from config.yml's "messages" section, with {placeholder} substitution. */
public final class Messages {

    private Messages() {}

    public static String get(String key) {
        return get(key, Collections.emptyMap());
    }

    public static String get(String key, Map<String, String> placeholders) {
        return getOrDefault(key, "<red>Missing message: " + key, placeholders);
    }

    /**
     * For message keys added in an update: YamlMergeUtil only appends whole new top-level
     * sections, so a new key inside the existing "messages" block never reaches an upgraded
     * install's config.yml. The shipped wording lives in code as the fallback, and an admin
     * who adds the key by hand still overrides it.
     */
    public static String getOrDefault(String key, String fallback) {
        return getOrDefault(key, fallback, Collections.emptyMap());
    }

    public static String getOrDefault(String key, String fallback, Map<String, String> placeholders) {
        FileConfiguration cfg = DAnnouncements.getInstance().getConfig();
        String raw = cfg.getString("messages." + key, fallback);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return raw;
    }
}
