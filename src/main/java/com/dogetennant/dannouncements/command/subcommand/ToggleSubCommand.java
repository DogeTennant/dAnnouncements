package com.dogetennant.dannouncements.command.subcommand;

import com.dogetennant.dannouncements.DAnnouncements;
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
        if (CommandUtil.requireAnnouncement(sender, id).isEmpty()) return;

        var loader = DAnnouncements.getInstance().getAnnouncementConfigLoader();
        if (!CommandUtil.saved(sender, loader.update(id, a -> {
            a.enabled = !a.enabled;
            return true;
        }), id)) return;
        boolean enabled = loader.get(id).map(a -> a.enabled).orElse(false);

        sender.sendMessage(ColorUtil.parse(Messages.get("toggled",
                Map.of("id", id, "state", enabled ? "<green>enabled" : "<red>disabled"))));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return new ArrayList<>(DAnnouncements.getInstance().getAnnouncementConfigLoader().getAll().keySet());
        }
        return List.of();
    }
}
