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
import java.util.regex.Pattern;

public class CreateSubCommand implements SubCommand {

    private static final Pattern VALID_ID = Pattern.compile("[a-zA-Z0-9_-]+");

    @Override public String getName() { return "create"; }
    @Override public String getPermission() { return "dannouncements.admin.create"; }
    @Override public String getUsage() { return "/da create <id>"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) { CommandUtil.sendUsage(sender, getUsage()); return; }
        String id = args[1];

        if (!VALID_ID.matcher(id).matches()) {
            sender.sendMessage(ColorUtil.parse(Messages.get("invalid-id", Map.of("id", id))));
            return;
        }

        DAnnouncements plugin = DAnnouncements.getInstance();
        if (plugin.getAnnouncementConfigLoader().exists(id)) {
            sender.sendMessage(ColorUtil.parse(Messages.get("already-exists", Map.of("id", id))));
            return;
        }

        Announcement a = Announcement.createDefault(id, plugin.getConfigManager().get());
        if (!CommandUtil.saved(sender, plugin.getAnnouncementConfigLoader().add(a), id)) return;

        sender.sendMessage(ColorUtil.parse(Messages.get("created", Map.of("id", id))));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
