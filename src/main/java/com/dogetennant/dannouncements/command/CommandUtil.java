package com.dogetennant.dannouncements.command;

import com.dogetennant.dannouncements.DAnnouncements;
import com.dogetennant.dannouncements.announcement.Announcement;
import com.dogetennant.dannouncements.announcement.AnnouncementConfigLoader;
import com.dogetennant.dannouncements.config.Messages;
import com.dogetennant.dannouncements.util.ColorUtil;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public final class CommandUtil {

    // Messages added after 1.0.0 - see Messages.getOrDefault for why the wording is here too
    public static final String FILE_UNREADABLE =
            "<red>announcements.yml has an error (see the console), so nothing was changed. Fix it, then /da reload.";
    public static final String FILE_NOT_SAVED = "<red>announcements.yml could not be saved (see the console).";

    private CommandUtil() {}

    /** True when the change was saved; otherwise tells the sender why not. */
    public static boolean saved(CommandSender sender, AnnouncementConfigLoader.Change change, String id) {
        switch (change) {
            case SAVED -> { return true; }
            case NOT_FOUND -> sender.sendMessage(ColorUtil.parse(Messages.get("not-found", Map.of("id", id))));
            case EXISTS -> sender.sendMessage(ColorUtil.parse(Messages.get("already-exists", Map.of("id", id))));
            case UNREADABLE -> sender.sendMessage(ColorUtil.parse(Messages.getOrDefault("file-unreadable", FILE_UNREADABLE)));
            case NOT_SAVED -> sender.sendMessage(ColorUtil.parse(Messages.getOrDefault("file-not-saved", FILE_NOT_SAVED)));
            case UNCHANGED -> { }
        }
        return false;
    }

    public static Optional<Announcement> requireAnnouncement(CommandSender sender, String id) {
        Optional<Announcement> found = DAnnouncements.getInstance().getAnnouncementConfigLoader().get(id);
        if (found.isEmpty()) {
            sender.sendMessage(ColorUtil.parse(Messages.get("not-found", Map.of("id", id))));
        }
        return found;
    }

    public static OptionalInt parseOneBasedIndex(CommandSender sender, String raw, int lineCount, String id) {
        int index;
        try {
            index = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtil.parse(Messages.get("invalid-index",
                    Map.of("index", raw, "id", id, "count", String.valueOf(lineCount)))));
            return OptionalInt.empty();
        }
        if (index < 1 || index > lineCount) {
            sender.sendMessage(ColorUtil.parse(Messages.get("invalid-index",
                    Map.of("index", raw, "id", id, "count", String.valueOf(lineCount)))));
            return OptionalInt.empty();
        }
        return OptionalInt.of(index);
    }

    public static void sendUsage(CommandSender sender, String usage) {
        sender.sendMessage(ColorUtil.parse(Messages.get("usage", Map.of("usage", usage))));
    }
}
