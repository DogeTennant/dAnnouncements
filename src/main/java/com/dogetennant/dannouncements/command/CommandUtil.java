package com.dogetennant.dannouncements.command;

import com.dogetennant.dannouncements.DAnnouncements;
import com.dogetennant.dannouncements.announcement.Announcement;
import com.dogetennant.dannouncements.config.Messages;
import com.dogetennant.dannouncements.util.ColorUtil;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public final class CommandUtil {

    private CommandUtil() {}

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
