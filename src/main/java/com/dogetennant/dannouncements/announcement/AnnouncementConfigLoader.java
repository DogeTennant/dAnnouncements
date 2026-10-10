package com.dogetennant.dannouncements.announcement;

import com.dogetennant.dannouncements.util.LogUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Loads announcements.yml and saves in-game changes to it. A change reads the file again first
 * (so what was edited by hand since the last reload is picked up, not overwritten), changes only
 * that announcement in it, in place (comments stay), and writes it through a temporary file. A
 * file that cannot be read is never saved over: the change is refused.
 */
public class AnnouncementConfigLoader {

    /** What an in-game change did. */
    public enum Change {
        SAVED,
        /** The edit itself decided against it (and told the sender why). */
        UNCHANGED,
        NOT_FOUND,
        EXISTS,
        /** announcements.yml has an error: nothing was changed. */
        UNREADABLE,
        /** Changed in memory, but writing the file failed. */
        NOT_SAVED
    }

    private final Plugin plugin;
    private final File file;
    private final Map<String, Announcement> announcements = new LinkedHashMap<>();
    private Runnable onChange = () -> {};

    public AnnouncementConfigLoader(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "announcements.yml");
    }

    /** Runs after every saved change (the scheduler picks up what changed). */
    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    /**
     * Reads the file. False when it cannot be read: the announcements loaded before stay in use,
     * and in-game changes are refused until it is fixed.
     */
    public boolean load() {
        if (!file.exists()) {
            plugin.saveResource("announcements.yml", false);
        }

        YamlConfiguration yml = read();
        if (yml == null) return false;
        fill(yml);
        LogUtil.info("Loaded " + announcements.size() + " announcement(s).");
        return true;
    }

    /** The file as it is now; null (and the error in the console) when it cannot be read. */
    private YamlConfiguration read() {
        YamlConfiguration yml = new YamlConfiguration();
        if (!file.exists()) return yml;
        try {
            yml.load(file);
            return yml;
        } catch (IOException | InvalidConfigurationException e) {
            LogUtil.severe("announcements.yml could not be read, so nothing in it is changed or saved over until it"
                    + " is fixed (then /da reload). Still using the " + announcements.size()
                    + " announcement(s) loaded before. The error: " + e.getMessage());
            return null;
        }
    }

    private void fill(YamlConfiguration yml) {
        Map<String, Announcement> fresh = new LinkedHashMap<>();
        ConfigurationSection root = yml.getConfigurationSection("announcements");
        if (root != null) {
            for (String id : root.getKeys(false)) {
                ConfigurationSection sec = root.getConfigurationSection(id);
                if (sec == null) continue;
                fresh.put(id, Announcement.fromConfig(id, sec));
            }
        }
        announcements.clear();
        announcements.putAll(fresh);
    }

    private Change change(Function<YamlConfiguration, Change> edit) {
        YamlConfiguration yml = read();
        if (yml == null) return Change.UNREADABLE;
        fill(yml);                                      // hand edits since the last load come along
        Change result = edit.apply(yml);
        if (result != Change.SAVED) return result;
        try {
            write(yml);
        } catch (IOException e) {
            LogUtil.severe("Failed to save announcements.yml: " + e.getMessage());
            return Change.NOT_SAVED;
        }
        onChange.run();
        return Change.SAVED;
    }

    private void write(YamlConfiguration yml) throws IOException {
        Path target = file.toPath();
        Path temp = target.resolveSibling("announcements.yml.tmp");
        Files.writeString(temp, yml.saveToString(), StandardCharsets.UTF_8);
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Adds a new announcement; EXISTS when the file has one of that id (maybe added by hand). */
    public Change add(Announcement announcement) {
        return change(yml -> {
            if (announcements.containsKey(announcement.id)) return Change.EXISTS;
            ConfigurationSection root = yml.getConfigurationSection("announcements");
            if (root == null) root = yml.createSection("announcements");
            announcement.writeTo(root.createSection(announcement.id));
            announcements.put(announcement.id, announcement);
            return Change.SAVED;
        });
    }

    /**
     * Changes the announcement as it is in the file now. {@code edit} returns false to leave it
     * (it has told the sender why).
     */
    public Change update(String id, Predicate<Announcement> edit) {
        return change(yml -> {
            Announcement announcement = announcements.get(id);
            ConfigurationSection section = yml.getConfigurationSection("announcements." + id);
            if (announcement == null || section == null) return Change.NOT_FOUND;
            if (!edit.test(announcement)) return Change.UNCHANGED;
            announcement.writeTo(section);
            return Change.SAVED;
        });
    }

    public Change remove(String id) {
        return change(yml -> {
            if (announcements.remove(id) == null) return Change.NOT_FOUND;
            yml.set("announcements." + id, null);
            return Change.SAVED;
        });
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
}
