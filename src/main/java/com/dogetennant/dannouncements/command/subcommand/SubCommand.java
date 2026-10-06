package com.dogetennant.dannouncements.command.subcommand;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface SubCommand {

    String getName();

    String getPermission();

    String getUsage();

    void execute(CommandSender sender, String[] args);

    List<String> tabComplete(CommandSender sender, String[] args);

    /**
     * Keeps a subcommand out of /da help and tab-completion for this sender. For ones that are
     * invoked by a click link rather than typed, listing them just advertises syntax - and
     * world names - to players who have no business typing them.
     */
    default boolean isHiddenFrom(CommandSender sender) {
        return false;
    }
}
