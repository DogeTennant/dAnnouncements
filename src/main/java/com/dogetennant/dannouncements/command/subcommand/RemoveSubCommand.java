package com.dogetennant.dannouncements.command.subcommand;

import com.dogetennant.dannouncements.DAnnouncements;
import com.dogetennant.dannouncements.command.CommandUtil;
import com.dogetennant.dannouncements.config.Messages;
import com.dogetennant.dannouncements.util.ColorUtil;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RemoveSubCommand implements SubCommand {

    @Override public String getName() { return "remove"; }
    @Override public String getPermission() { return "dannouncements.admin.remove"; }
    @Override public String getUsage() { return "/da remove <id>"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) { CommandUtil.sendUsage(sender, getUsage()); return; }
        String id = args[1];
        if (CommandUtil.requireAnnouncement(sender, id).isEmpty()) return;

        if (!CommandUtil.saved(sender, DAnnouncements.getInstance().getAnnouncementConfigLoader().remove(id), id)) return;

        sender.sendMessage(ColorUtil.parse(Messages.get("removed", Map.of("id", id))));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return new ArrayList<>(DAnnouncements.getInstance().getAnnouncementConfigLoader().getAll().keySet());
        }
        return List.of();
    }
}
