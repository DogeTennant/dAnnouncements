package com.dogetennant.dannouncements.config;

import com.dogetennant.dannouncements.util.LogUtil;
import com.dogetennant.dannouncements.util.YamlMergeUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;

public class ConfigManager {

    private final Plugin plugin;
    private MainConfig mainConfig;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        YamlMergeUtil.mergeMissingKeys(new File(plugin.getDataFolder(), "config.yml"),
                "config.yml", plugin, "config.yml");
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        MainConfig c = new MainConfig();

        c.timezone = cfg.getString("timezone", "system");
        c.pollIntervalTicks = cfg.getLong("poll-interval-ticks", 20);

        c.defaultChatEnabled = cfg.getBoolean("defaults.chat.enabled", true);
        c.defaultChatBorder = cfg.getBoolean("defaults.chat.border", true);
        c.defaultChatBorderChar = cfg.getString("defaults.chat.border-char", "▬");
        c.defaultChatBorderLength = cfg.getInt("defaults.chat.border-length", 46);
        c.defaultChatBorderColor = cfg.getString("defaults.chat.border-color", "<dark_gray>");
        c.defaultChatCenter = cfg.getBoolean("defaults.chat.center", true);

        c.defaultTitleEnabled = cfg.getBoolean("defaults.title.enabled", false);
        c.defaultTitleFadeIn = cfg.getInt("defaults.title.fade-in", 10);
        c.defaultTitleStay = cfg.getInt("defaults.title.stay", 60);
        c.defaultTitleFadeOut = cfg.getInt("defaults.title.fade-out", 20);

        c.defaultActionbarEnabled = cfg.getBoolean("defaults.actionbar.enabled", false);

        c.defaultBossbarEnabled = cfg.getBoolean("defaults.bossbar.enabled", false);
        c.defaultBossbarColor = cfg.getString("defaults.bossbar.color", "YELLOW").toUpperCase();
        c.defaultBossbarOverlay = cfg.getString("defaults.bossbar.overlay", "PROGRESS").toUpperCase();
        c.defaultBossbarSeconds = cfg.getInt("defaults.bossbar.seconds", 8);

        c.defaultSoundEnabled = cfg.getBoolean("defaults.sound.enabled", true);
        c.defaultSoundName = cfg.getString("defaults.sound.name", "ENTITY_PLAYER_LEVELUP").toUpperCase();
        c.defaultSoundVolume = cfg.getDouble("defaults.sound.volume", 1.0);
        c.defaultSoundPitch = cfg.getDouble("defaults.sound.pitch", 1.0);

        mainConfig = c;
        LogUtil.info("Config loaded. Timezone: " + c.timezone);
    }

    public MainConfig get() {
        return mainConfig;
    }
}
