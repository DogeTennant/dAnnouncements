package com.dogetennant.dannouncements.command.subcommand;

import com.dogetennant.dannouncements.DAnnouncements;
import com.dogetennant.dannouncements.announcement.Announcement;
import com.dogetennant.dannouncements.command.CommandUtil;
import com.dogetennant.dannouncements.util.ColorUtil;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class JoinSubCommand implements SubCommand {

    @Override public String getName() { return "join"; }
    @Override public String getPermission() { return "dannouncements.admin.join"; }
    @Override public String getUsage() { return "/da join <id> <on|off|delay> [seconds]"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) { CommandUtil.sendUsage(sender, getUsage()); return; }
        String id = args[1];
        String action = args[2].toLowerCase();

        if (CommandUtil.requireAnnouncement(sender, id).isEmpty()) return;

        // the number is checked before anything changes
        switch (action) {
            case "on" -> {
                Integer secs = null;
                if (args.length >= 4) {
                    secs = parseSeconds(sender, args[3]);
                    if (secs == null) return;
                }
                Integer delay = secs;
                if (!save(sender, id, a -> {
                    a.join.enabled = true;
                    if (delay != null) a.join.delaySeconds = delay;
                })) return;
                sender.sendMessage(ColorUtil.parse("<green>Join trigger for <white>" + id + "<green> enabled"
                        + (delay != null ? " with a <white>" + delay + "s<green> delay." : ".")));
            }
            case "off" -> {
                if (!save(sender, id, a -> a.join.enabled = false)) return;
                sender.sendMessage(ColorUtil.parse("<green>Join trigger for <white>" + id + "<green> disabled."));
            }
            case "delay" -> {
                if (args.length < 4) { CommandUtil.sendUsage(sender, "/da join <id> delay <seconds>"); return; }
                Integer secs = parseSeconds(sender, args[3]);
                if (secs == null) return;
                if (!save(sender, id, a -> a.join.delaySeconds = secs)) return;
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

    private boolean save(CommandSender sender, String id, Consumer<Announcement> edit) {
        return CommandUtil.saved(sender, DAnnouncements.getInstance().getAnnouncementConfigLoader().update(id, a -> {
            edit.accept(a);
            return true;
        }), id);
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
