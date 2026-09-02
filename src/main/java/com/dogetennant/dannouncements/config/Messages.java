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
        FileConfiguration cfg = DAnnouncements.getInstance().getConfig();
        String raw = cfg.getString("messages." + key, "<red>Missing message: " + key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return raw;
    }
}
