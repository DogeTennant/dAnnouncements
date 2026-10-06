package com.dogetennant.dannouncements.command;

import com.dogetennant.dannouncements.command.subcommand.SubCommand;
import com.dogetennant.dannouncements.config.Messages;
import com.dogetennant.dannouncements.util.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DAnnouncementsCommand implements CommandExecutor, TabCompleter {

    private final CommandRegistry registry;

    public DAnnouncementsCommand(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                              @NotNull String label, String[] args) {
        if (args.length == 0) {
            registry.get("help").ifPresent(help -> help.execute(sender, args));
            return true;
        }

        Optional<SubCommand> subOpt = registry.get(args[0]);
        if (subOpt.isEmpty()) {
            sender.sendMessage(ColorUtil.parse(Messages.get("unknown-command")));
            return true;
        }

        SubCommand sub = subOpt.get();
        if (!sender.hasPermission(sub.getPermission())) {
            sender.sendMessage(ColorUtil.parse(Messages.get("no-permission")));
            return true;
        }

        sub.execute(sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String label, String[] args) {
        if (args.length == 1) {
            return registry.getAll().stream()
                    .filter(sub -> sender.hasPermission(sub.getPermission()) && !sub.isHiddenFrom(sender))
                    .map(SubCommand::getName)
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length >= 2) {
            return registry.get(args[0])
                    .filter(sub -> sender.hasPermission(sub.getPermission()))
                    .map(sub -> sub.tabComplete(sender, args))
                    .orElse(List.of());
        }
        return List.of();
    }
}
