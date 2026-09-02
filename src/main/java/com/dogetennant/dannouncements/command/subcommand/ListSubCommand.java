package com.dogetennant.dannouncements.command.subcommand;

import com.dogetennant.dannouncements.DAnnouncements;
import com.dogetennant.dannouncements.announcement.Announcement;
import com.dogetennant.dannouncements.util.ColorUtil;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ListSubCommand implements SubCommand {

    @Override public String getName() { return "list"; }
    @Override public String getPermission() { return "dannouncements.admin.list"; }
    @Override public String getUsage() { return "/da list"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        var all = DAnnouncements.getInstance().getAnnouncementConfigLoader().getAll();
        if (all.isEmpty()) {
            sender.sendMessage(ColorUtil.parse("<gray>No announcements configured yet. Use <white>/da create <id><gray> to make one."));
            return;
        }

        sender.sendMessage(ColorUtil.parse("<gold><bold>dAnnouncements <gray>- " + all.size() + " configured"));
        for (Announcement a : all.values()) {
            String state = a.enabled ? "<green>enabled" : "<red>disabled";
            String schedule = a.schedule.isEnabled() ? a.schedule.getType().name() : "manual only";
            sender.sendMessage(ColorUtil.parse("<white>- " + a.id + " <gray>[" + state + "<gray>] <dark_gray>| <gray>"
                    + schedule + " <dark_gray>| <gray>" + a.lines.size() + " line(s)"));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
