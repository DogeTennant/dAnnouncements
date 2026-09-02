package com.dogetennant.dannouncements.announcement;

import com.dogetennant.dannouncements.util.LogUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Loads/saves announcements.yml. Saving always rewrites the whole file from the in-memory
 * model.
 */
public class AnnouncementConfigLoader {

    private final Plugin plugin;
    private final File file;
    private final Map<String, Announcement> announcements = new LinkedHashMap<>();

    public AnnouncementConfigLoader(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "announcements.yml");
    }

    public void load() {
        if (!file.exists()) {
            plugin.saveResource("announcements.yml", false);
        }

        announcements.clear();
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yml.getConfigurationSection("announcements");
        if (root != null) {
            for (String id : root.getKeys(false)) {
                ConfigurationSection sec = root.getConfigurationSection(id);
                if (sec == null) continue;
                announcements.put(id, Announcement.fromConfig(id, sec));
            }
        }

        LogUtil.info("Loaded " + announcements.size() + " announcement(s).");
    }

    public void saveAll() {
        YamlConfiguration yml = new YamlConfiguration();
        ConfigurationSection root = yml.createSection("announcements");
        for (Announcement a : announcements.values()) {
            a.writeTo(root.createSection(a.id));
        }
        try {
            yml.save(file);
        } catch (IOException e) {
            LogUtil.severe("Failed to save announcements.yml: " + e.getMessage());
        }
    }

    public Map<String, Announcement> getAll() {
        return announcements;
    }

    public Optional<Announcement> get(String id) {
        return Optional.ofNullable(announcements.get(id));
    }

    public boolean exists(String id) {
        return announcements.containsKey(id);
    }

    public void put(Announcement announcement) {
        announcements.put(announcement.id, announcement);
        saveAll();
    }

    public void remove(String id) {
        announcements.remove(id);
        saveAll();
    }
}
