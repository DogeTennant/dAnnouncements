package com.dogetennant.dannouncements.command.subcommand;

import com.dogetennant.dannouncements.DAnnouncements;
import com.dogetennant.dannouncements.announcement.Announcement;
import com.dogetennant.dannouncements.command.CommandUtil;
import com.dogetennant.dannouncements.util.ColorUtil;
import com.dogetennant.dannouncements.util.TimeUtil;
import org.bukkit.command.CommandSender;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class InfoSubCommand implements SubCommand {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");

    @Override public String getName() { return "info"; }
    @Override public String getPermission() { return "dannouncements.admin.info"; }
    @Override public String getUsage() { return "/da info <id>"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) { CommandUtil.sendUsage(sender, getUsage()); return; }
        var found = CommandUtil.requireAnnouncement(sender, args[1]);
        if (found.isEmpty()) return;
        Announcement a = found.get();

        sender.sendMessage(ColorUtil.parse("<gold><bold>" + a.id));
        sender.sendMessage(ColorUtil.parse("<gray>Enabled: " + (a.enabled ? "<green>yes" : "<red>no")
                + " <dark_gray>| <gray>Once: " + (a.once ? "<green>yes" : "<red>no")));
        sender.sendMessage(ColorUtil.parse("<gray>Permission: <white>"
                + (a.permission == null || a.permission.isBlank() ? "everyone" : a.permission)));

        if (a.schedule.isEnabled()) {
            sender.sendMessage(ColorUtil.parse("<gray>Schedule: <white>" + describeSchedule(a)));
            DAnnouncements.getInstance().getScheduler().getNextRun(a.id).ifPresentOrElse(
                    next -> {
                        ZoneId tz = com.dogetennant.dannouncements.util.TimeUtil.resolveTimezone(
                                DAnnouncements.getInstance().getConfigManager().get().timezone);
                        String when = Instant.ofEpochMilli(next).atZone(tz).format(FORMAT);
                        sender.sendMessage(ColorUtil.parse("<gray>Next run: <white>" + when));
                    },
                    () -> sender.sendMessage(ColorUtil.parse("<gray>Next run: <white>not scheduled")));
        } else {
            sender.sendMessage(ColorUtil.parse("<gray>Schedule: <white>manual only (use /da force)"));
        }

        if (a.join.enabled) {
            String exclusions = a.join.excludeOps
                    ? ("ops" + (a.join.excludedPermissions.isEmpty() ? "" : ", " + String.join(", ", a.join.excludedPermissions)))
                    : (a.join.excludedPermissions.isEmpty() ? "none" : String.join(", ", a.join.excludedPermissions));
            sender.sendMessage(ColorUtil.parse("<gray>On join: <green>yes <dark_gray>| <gray>delay: <white>"
                    + a.join.delaySeconds + "s <dark_gray>| <gray>excludes: <white>" + exclusions));
        } else {
            sender.sendMessage(ColorUtil.parse("<gray>On join: <red>no"));
        }

        List<String> channels = new ArrayList<>();
        if (a.chat.enabled) channels.add("chat");
        if (a.title.enabled) channels.add("title");
        if (a.actionbar.enabled) channels.add("actionbar");
        if (a.bossbar.enabled) channels.add("bossbar");
        if (a.sound.enabled) channels.add("sound");
        sender.sendMessage(ColorUtil.parse("<gray>Delivery: <white>"
                + (channels.isEmpty() ? "none" : String.join(", ", channels))));

        sender.sendMessage(ColorUtil.parse("<gray>Lines (" + a.lines.size() + "):"));
        for (int i = 0; i < a.lines.size(); i++) {
            sender.sendMessage(ColorUtil.parse("<dark_gray>[" + (i + 1) + "] ").append(ColorUtil.parse(a.lines.get(i))));
        }
    }

    private String describeSchedule(Announcement a) {
        var s = a.schedule;
        return switch (s.getType()) {
            case INTERVAL -> "every " + s.getIntervalMinutes() + " minute(s)";
            case DAILY -> "daily at " + s.getEffectiveDailyTimes().stream()
                    .map(t -> time(t[0], t[1]))
                    .collect(java.util.stream.Collectors.joining(", "));
            case WEEKLY -> (s.getDayOfWeek() != null ? s.getDayOfWeek().name() : "MONDAY")
                    + " at " + time(s.getHour(), s.getMinute());
            case MONTHLY -> "day " + s.getDayOfMonth() + " at " + time(s.getHour(), s.getMinute());
            case SPECIFIC -> s.getSpecificDate() + " " + time(s.getHour(), s.getMinute());
        };
    }

    private String time(int hour, int minute) {
        return String.format("%02d:%02d", hour, minute);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return new ArrayList<>(DAnnouncements.getInstance().getAnnouncementConfigLoader().getAll().keySet());
        }
        return List.of();
    }
}
