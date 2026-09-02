package com.dogetennant.dannouncements.command.subcommand;

import com.dogetennant.dannouncements.DAnnouncements;
import com.dogetennant.dannouncements.announcement.Announcement;
import com.dogetennant.dannouncements.command.CommandUtil;
import com.dogetennant.dannouncements.config.Messages;
import com.dogetennant.dannouncements.util.ColorUtil;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ToggleSubCommand implements SubCommand {

    @Override public String getName() { return "toggle"; }
    @Override public String getPermission() { return "dannouncements.admin.toggle"; }
    @Override public String getUsage() { return "/da toggle <id>"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) { CommandUtil.sendUsage(sender, getUsage()); return; }
        String id = args[1];
        var found = CommandUtil.requireAnnouncement(sender, id);
        if (found.isEmpty()) return;

        Announcement a = found.get();
        a.enabled = !a.enabled;

        DAnnouncements plugin = DAnnouncements.getInstance();
        plugin.getAnnouncementConfigLoader().put(a);
        plugin.getScheduler().reload(plugin.getConfigManager().get());

        sender.sendMessage(ColorUtil.parse(Messages.get("toggled",
                Map.of("id", id, "state", a.enabled ? "<green>enabled" : "<red>disabled"))));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return new ArrayList<>(DAnnouncements.getInstance().getAnnouncementConfigLoader().getAll().keySet());
        }
        return List.of();
    }
}
