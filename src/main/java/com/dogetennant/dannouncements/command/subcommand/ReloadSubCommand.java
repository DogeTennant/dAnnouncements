package com.dogetennant.dannouncements.command.subcommand;

import com.dogetennant.dannouncements.DAnnouncements;
import com.dogetennant.dannouncements.config.Messages;
import com.dogetennant.dannouncements.util.ColorUtil;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public class ReloadSubCommand implements SubCommand {

    @Override public String getName() { return "reload"; }
    @Override public String getPermission() { return "dannouncements.admin.reload"; }
    @Override public String getUsage() { return "/da reload"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        DAnnouncements plugin = DAnnouncements.getInstance();
        plugin.getConfigManager().load();
        plugin.getAnnouncementConfigLoader().load();
        plugin.getScheduler().reload(plugin.getConfigManager().get());

        int count = plugin.getAnnouncementConfigLoader().getAll().size();
        sender.sendMessage(ColorUtil.parse(Messages.get("reloaded", Map.of("count", String.valueOf(count)))));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
