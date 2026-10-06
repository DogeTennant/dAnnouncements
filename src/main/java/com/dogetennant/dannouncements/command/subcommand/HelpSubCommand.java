package com.dogetennant.dannouncements.command.subcommand;

import com.dogetennant.dannouncements.command.CommandRegistry;
import com.dogetennant.dannouncements.util.ColorUtil;
import org.bukkit.command.CommandSender;

import java.util.List;

public class HelpSubCommand implements SubCommand {

    private final CommandRegistry registry;

    public HelpSubCommand(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override public String getName() { return "help"; }
    @Override public String getPermission() { return "dannouncements.command"; }
    @Override public String getUsage() { return "/da help"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(ColorUtil.parse("<dark_gray><strikethrough>                                                  "));
        sender.sendMessage(ColorUtil.parse("<gold><bold>dAnnouncements <gray>- commands"));
        for (SubCommand sub : registry.getAll()) {
            if (!sender.hasPermission(sub.getPermission()) || sub.isHiddenFrom(sender)) continue;
            sender.sendMessage(ColorUtil.parse("<yellow>" + sub.getUsage()));
        }
        sender.sendMessage(ColorUtil.parse("<dark_gray><strikethrough>                                                  "));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
