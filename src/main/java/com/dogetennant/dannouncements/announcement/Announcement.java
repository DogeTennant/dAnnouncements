package com.dogetennant.dannouncements.announcement;

import com.dogetennant.dannouncements.config.MainConfig;
import com.dogetennant.dannouncements.schedule.AnnouncementSchedule;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class Announcement {

    public String id;
    public boolean enabled;
    public boolean once;
    public String permission = "";
    public AnnouncementSchedule schedule;
    public Join join = new Join();
    public List<String> lines = new ArrayList<>();

    public Chat chat = new Chat();
    public Title title = new Title();
    public Actionbar actionbar = new Actionbar();
    public Bossbar bossbar = new Bossbar();
    public Sound sound = new Sound();

    public static class Chat {
        public boolean enabled = true;
        public boolean border = true;
        public String borderChar = "▬";
        public int borderLength = 46;
        public String borderColor = "<dark_gray>";
        public boolean center = true;
    }

    public static class Title {
        public boolean enabled = false;
        public String text = "";
        public String subtitle = "";
        public int fadeIn = 10;
        public int stay = 60;
        public int fadeOut = 20;
    }

    public static class Actionbar {
        public boolean enabled = false;
        public String text = "";
    }

    public static class Bossbar {
        public boolean enabled = false;
        public String text = "";
        public String color = "YELLOW";
        public String overlay = "PROGRESS";
        public int seconds = 8;
    }

    public static class Sound {
        public boolean enabled = true;
        public String name = "ENTITY_PLAYER_LEVELUP";
        public double volume = 1.0;
        public double pitch = 1.0;
    }

    /** Independent of "schedule" - fires per-player on PlayerJoinEvent instead of on a timer.
     *  Can be the sole trigger for an announcement (schedule disabled entirely). */
    public static class Join {
        public boolean enabled = false;
        public int delaySeconds = 0;
        public boolean excludeOps = false;
        public List<String> excludedPermissions = new ArrayList<>();
    }

    /** Builds a fresh, disabled announcement seeded from the server's configured defaults. */
    public static Announcement createDefault(String id, MainConfig cfg) {
        Announcement a = new Announcement();
        a.id = id;
        a.enabled = false;
        a.once = false;
        a.permission = "";
        a.schedule = new AnnouncementSchedule(false,
                com.dogetennant.dannouncements.schedule.ScheduleType.DAILY,
                9, 0, 60, null, 1, null, null);

        a.chat.enabled = cfg.defaultChatEnabled;
        a.chat.border = cfg.defaultChatBorder;
        a.chat.borderChar = cfg.defaultChatBorderChar;
        a.chat.borderLength = cfg.defaultChatBorderLength;
        a.chat.borderColor = cfg.defaultChatBorderColor;
        a.chat.center = cfg.defaultChatCenter;

        a.title.enabled = cfg.defaultTitleEnabled;
        a.title.fadeIn = cfg.defaultTitleFadeIn;
        a.title.stay = cfg.defaultTitleStay;
        a.title.fadeOut = cfg.defaultTitleFadeOut;

        a.actionbar.enabled = cfg.defaultActionbarEnabled;

        a.bossbar.enabled = cfg.defaultBossbarEnabled;
        a.bossbar.color = cfg.defaultBossbarColor;
        a.bossbar.overlay = cfg.defaultBossbarOverlay;
        a.bossbar.seconds = cfg.defaultBossbarSeconds;

        a.sound.enabled = cfg.defaultSoundEnabled;
        a.sound.name = cfg.defaultSoundName;
        a.sound.volume = cfg.defaultSoundVolume;
        a.sound.pitch = cfg.defaultSoundPitch;

        return a;
    }

    public static Announcement fromConfig(String id, ConfigurationSection sec) {
        Announcement a = new Announcement();
        a.id = id;
        a.enabled = sec.getBoolean("enabled", false);
        a.once = sec.getBoolean("once", false);
        a.permission = sec.getString("permission", "");
        a.schedule = AnnouncementSchedule.fromConfig(sec.getConfigurationSection("schedule"));
        a.lines = new ArrayList<>(sec.getStringList("lines"));

        ConfigurationSection joinSec = sec.getConfigurationSection("join");
        if (joinSec != null) {
            a.join.enabled = joinSec.getBoolean("enabled", a.join.enabled);
            a.join.delaySeconds = joinSec.getInt("delay-seconds", a.join.delaySeconds);
            a.join.excludeOps = joinSec.getBoolean("exclude-ops", a.join.excludeOps);
            a.join.excludedPermissions = new ArrayList<>(joinSec.getStringList("excluded-permissions"));
        }

        ConfigurationSection chatSec = sec.getConfigurationSection("delivery.chat");
        if (chatSec != null) {
            a.chat.enabled = chatSec.getBoolean("enabled", a.chat.enabled);
            a.chat.border = chatSec.getBoolean("border", a.chat.border);
            a.chat.borderChar = chatSec.getString("border-char", a.chat.borderChar);
            a.chat.borderLength = chatSec.getInt("border-length", a.chat.borderLength);
            a.chat.borderColor = chatSec.getString("border-color", a.chat.borderColor);
            a.chat.center = chatSec.getBoolean("center", a.chat.center);
        }

        ConfigurationSection titleSec = sec.getConfigurationSection("delivery.title");
        if (titleSec != null) {
            a.title.enabled = titleSec.getBoolean("enabled", a.title.enabled);
            a.title.text = titleSec.getString("text", a.title.text);
            a.title.subtitle = titleSec.getString("subtitle", a.title.subtitle);
            a.title.fadeIn = titleSec.getInt("fade-in", a.title.fadeIn);
            a.title.stay = titleSec.getInt("stay", a.title.stay);
            a.title.fadeOut = titleSec.getInt("fade-out", a.title.fadeOut);
        }

        ConfigurationSection actionbarSec = sec.getConfigurationSection("delivery.actionbar");
        if (actionbarSec != null) {
            a.actionbar.enabled = actionbarSec.getBoolean("enabled", a.actionbar.enabled);
            a.actionbar.text = actionbarSec.getString("text", a.actionbar.text);
        }

        ConfigurationSection bossbarSec = sec.getConfigurationSection("delivery.bossbar");
        if (bossbarSec != null) {
            a.bossbar.enabled = bossbarSec.getBoolean("enabled", a.bossbar.enabled);
            a.bossbar.text = bossbarSec.getString("text", a.bossbar.text);
            a.bossbar.color = bossbarSec.getString("color", a.bossbar.color).toUpperCase();
            a.bossbar.overlay = bossbarSec.getString("overlay", a.bossbar.overlay).toUpperCase();
            a.bossbar.seconds = bossbarSec.getInt("seconds", a.bossbar.seconds);
        }

        ConfigurationSection soundSec = sec.getConfigurationSection("delivery.sound");
        if (soundSec != null) {
            a.sound.enabled = soundSec.getBoolean("enabled", a.sound.enabled);
            a.sound.name = soundSec.getString("name", a.sound.name).toUpperCase();
            a.sound.volume = soundSec.getDouble("volume", a.sound.volume);
            a.sound.pitch = soundSec.getDouble("pitch", a.sound.pitch);
        }

        return a;
    }

    /** Writes into the announcement's section of announcements.yml, changing only values, so comments stay. */
    public void writeTo(ConfigurationSection sec) {
        sec.set("enabled", enabled);
        sec.set("once", once);
        sec.set("permission", permission);
        schedule.writeTo(section(sec, "schedule"));

        ConfigurationSection joinSec = section(sec, "join");
        joinSec.set("enabled", join.enabled);
        joinSec.set("delay-seconds", join.delaySeconds);
        joinSec.set("exclude-ops", join.excludeOps);
        joinSec.set("excluded-permissions", join.excludedPermissions);

        sec.set("lines", lines);

        ConfigurationSection chatSec = section(sec, "delivery.chat");
        chatSec.set("enabled", chat.enabled);
        chatSec.set("border", chat.border);
        chatSec.set("border-char", chat.borderChar);
        chatSec.set("border-length", chat.borderLength);
        chatSec.set("border-color", chat.borderColor);
        chatSec.set("center", chat.center);

        ConfigurationSection titleSec = section(sec, "delivery.title");
        titleSec.set("enabled", title.enabled);
        titleSec.set("text", title.text);
        titleSec.set("subtitle", title.subtitle);
        titleSec.set("fade-in", title.fadeIn);
        titleSec.set("stay", title.stay);
        titleSec.set("fade-out", title.fadeOut);

        ConfigurationSection actionbarSec = section(sec, "delivery.actionbar");
        actionbarSec.set("enabled", actionbar.enabled);
        actionbarSec.set("text", actionbar.text);

        ConfigurationSection bossbarSec = section(sec, "delivery.bossbar");
        bossbarSec.set("enabled", bossbar.enabled);
        bossbarSec.set("text", bossbar.text);
        bossbarSec.set("color", bossbar.color);
        bossbarSec.set("overlay", bossbar.overlay);
        bossbarSec.set("seconds", bossbar.seconds);

        ConfigurationSection soundSec = section(sec, "delivery.sound");
        soundSec.set("enabled", sound.enabled);
        soundSec.set("name", sound.name);
        soundSec.set("volume", sound.volume);
        soundSec.set("pitch", sound.pitch);
    }

    /** The existing section (its comments stay) or a new one. */
    private static ConfigurationSection section(ConfigurationSection parent, String path) {
        ConfigurationSection existing = parent.getConfigurationSection(path);
        return existing != null ? existing : parent.createSection(path);
    }
}
