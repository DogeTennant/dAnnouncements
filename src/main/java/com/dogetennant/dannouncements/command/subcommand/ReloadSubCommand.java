package com.dogetennant.dannouncements.command.subcommand;

import com.dogetennant.dannouncements.DAnnouncements;
import com.dogetennant.dannouncements.config.Messages;
import com.dogetennant.dannouncements.util.ColorUtil;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public class ReloadSubCommand implements SubCommand {

    /** Added after 1.0.0 - see Messages.getOrDefault for why the wording is here too. */
    private static final String RELOAD_UNREADABLE = "<red>announcements.yml has an error (see the console) -"
            + " still using the <white>{count}<red> announcement(s) loaded before.";

    @Override public String getName() { return "reload"; }
    @Override public String getPermission() { return "dannouncements.admin.reload"; }
    @Override public String getUsage() { return "/da reload"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        DAnnouncements plugin = DAnnouncements.getInstance();
        plugin.getConfigManager().load();
        boolean read = plugin.getAnnouncementConfigLoader().load();
        plugin.getScheduler().reload(plugin.getConfigManager().get());

        int count = plugin.getAnnouncementConfigLoader().getAll().size();
        if (!read) {
            sender.sendMessage(ColorUtil.parse(Messages.getOrDefault("reload-unreadable", RELOAD_UNREADABLE,
                    Map.of("count", String.valueOf(count)))));
            return;
        }
        sender.sendMessage(ColorUtil.parse(Messages.get("reloaded", Map.of("count", String.valueOf(count)))));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
