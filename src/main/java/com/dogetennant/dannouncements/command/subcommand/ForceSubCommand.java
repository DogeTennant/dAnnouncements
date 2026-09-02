package com.dogetennant.dannouncements.command.subcommand;

import com.dogetennant.dannouncements.DAnnouncements;
import com.dogetennant.dannouncements.announcement.Announcement;
import com.dogetennant.dannouncements.command.CommandUtil;
import com.dogetennant.dannouncements.config.Messages;
import com.dogetennant.dannouncements.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ForceSubCommand implements SubCommand {

    @Override public String getName() { return "force"; }
    @Override public String getPermission() { return "dannouncements.admin.force"; }
    @Override public String getUsage() { return "/da force <id> [player]"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) { CommandUtil.sendUsage(sender, getUsage()); return; }
        String id = args[1];
        var found = CommandUtil.requireAnnouncement(sender, id);
        if (found.isEmpty()) return;
        Announcement a = found.get();

        Player target = null;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(ColorUtil.parse(Messages.get("player-not-found", Map.of("name", args[2]))));
                return;
            }
        }

        int count = DAnnouncements.getInstance().getDispatcher().dispatch(a, target);
        sender.sendMessage(ColorUtil.parse(Messages.get("forced", Map.of("id", id, "count", String.valueOf(count)))));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return new ArrayList<>(DAnnouncements.getInstance().getAnnouncementConfigLoader().getAll().keySet());
        }
        if (args.length == 3) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return names;
        }
        return List.of();
    }
}
