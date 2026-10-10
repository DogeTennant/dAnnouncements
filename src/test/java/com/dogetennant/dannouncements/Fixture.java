package com.dogetennant.dannouncements;

import com.dogetennant.dannouncements.announcement.Announcement;
import com.dogetennant.dannouncements.config.MainConfig;
import com.dogetennant.dannouncements.util.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** A mocked server and plugin around a temporary data folder; tasks run later are collected. */
public final class Fixture {

    public final DAnnouncements plugin = mock(DAnnouncements.class);
    public final Server server = mock(Server.class);
    public final BukkitScheduler scheduler = mock(BukkitScheduler.class);
    public final List<Runnable> later = new ArrayList<>();
    /** The repeating tasks (the scheduler's poll). */
    public final List<Runnable> timers = new ArrayList<>();
    public final File folder;

    public Fixture(Path dataFolder) throws Exception {
        folder = dataFolder.toFile();
        when(plugin.getDataFolder()).thenReturn(folder);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("dAnnouncements-test"));
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());     // messages: the built-in wording
        when(server.getLogger()).thenReturn(Logger.getLogger("server-test"));
        when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTaskTimer(any(Plugin.class), any(Runnable.class), anyLong(), anyLong())).thenAnswer(call -> {
            timers.add(call.getArgument(1, Runnable.class));
            return mock(BukkitTask.class);
        });
        when(scheduler.runTaskLater(any(Plugin.class), any(Runnable.class), anyLong())).thenAnswer(call -> {
            later.add(call.getArgument(1, Runnable.class));
            return mock(BukkitTask.class);
        });
        setStatic(Bukkit.class, "server", server);
        setStatic(DAnnouncements.class, "instance", plugin);
        LogUtil.init(plugin);
    }

    /** announcements.yml with these announcements (indented under "announcements:"). */
    public Path announcements(String body) throws IOException {
        Path file = folder.toPath().resolve("announcements.yml");
        Files.writeString(file, "announcements:\n" + body.stripIndent().indent(2), StandardCharsets.UTF_8);
        return file;
    }

    public static MainConfig config() {
        MainConfig config = new MainConfig();
        config.timezone = "UTC";
        config.pollIntervalTicks = 20;
        config.defaultChatBorderChar = "-";
        config.defaultChatBorderColor = "";
        config.defaultBossbarColor = "YELLOW";
        config.defaultBossbarOverlay = "PROGRESS";
        config.defaultSoundName = "ENTITY_PLAYER_LEVELUP";
        return config;
    }

    /** An announcement as announcements.yml would have it under its id. */
    public static Announcement announcement(String id, String yaml) throws InvalidConfigurationException {
        YamlConfiguration section = new YamlConfiguration();
        section.loadFromString(yaml);
        return Announcement.fromConfig(id, section);
    }

    private static void setStatic(Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
}
