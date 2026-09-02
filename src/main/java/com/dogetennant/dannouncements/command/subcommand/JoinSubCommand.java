package com.dogetennant.dannouncements.command.subcommand;

import com.dogetennant.dannouncements.DAnnouncements;
import com.dogetennant.dannouncements.announcement.Announcement;
import com.dogetennant.dannouncements.command.CommandUtil;
import com.dogetennant.dannouncements.util.ColorUtil;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class JoinSubCommand implements SubCommand {

    @Override public String getName() { return "join"; }
    @Override public String getPermission() { return "dannouncements.admin.join"; }
    @Override public String getUsage() { return "/da join <id> <on|off|delay> [seconds]"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) { CommandUtil.sendUsage(sender, getUsage()); return; }
        String id = args[1];
        String action = args[2].toLowerCase();

        var found = CommandUtil.requireAnnouncement(sender, id);
        if (found.isEmpty()) return;
        Announcement a = found.get();

        switch (action) {
            case "on" -> {
                a.join.enabled = true;
                if (args.length >= 4) {
                    Integer secs = parseSeconds(sender, args[3]);
                    if (secs == null) return;
                    a.join.delaySeconds = secs;
                }
                save(a);
                sender.sendMessage(ColorUtil.parse("<green>Join trigger for <white>" + id + "<green> enabled"
                        + (args.length >= 4 ? " with a <white>" + a.join.delaySeconds + "s<green> delay." : ".")));
            }
            case "off" -> {
                a.join.enabled = false;
                save(a);
                sender.sendMessage(ColorUtil.parse("<green>Join trigger for <white>" + id + "<green> disabled."));
            }
            case "delay" -> {
                if (args.length < 4) { CommandUtil.sendUsage(sender, "/da join <id> delay <seconds>"); return; }
                Integer secs = parseSeconds(sender, args[3]);
                if (secs == null) return;
                a.join.delaySeconds = secs;
                save(a);
                sender.sendMessage(ColorUtil.parse("<green>Join delay for <white>" + id
                        + "<green> set to <white>" + secs + "s<green>."));
            }
            default -> CommandUtil.sendUsage(sender, getUsage());
        }
    }

    private Integer parseSeconds(CommandSender sender, String raw) {
        try {
            int val = Integer.parseInt(raw);
            if (val < 0) throw new NumberFormatException();
            return val;
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtil.parse("<red>Delay must be a non-negative whole number of seconds."));
            return null;
        }
    }

    private void save(Announcement a) {
        DAnnouncements.getInstance().getAnnouncementConfigLoader().put(a);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return new ArrayList<>(DAnnouncements.getInstance().getAnnouncementConfigLoader().getAll().keySet());
        }
        if (args.length == 3) {
            return new ArrayList<>(List.of("on", "off", "delay"));
        }
        return List.of();
    }
}
