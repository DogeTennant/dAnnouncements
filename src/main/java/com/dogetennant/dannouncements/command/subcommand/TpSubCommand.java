package com.dogetennant.dannouncements.command.subcommand;

import com.dogetennant.dannouncements.command.CommandUtil;
import com.dogetennant.dannouncements.config.Messages;
import com.dogetennant.dannouncements.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Self-only, coordinate-only teleport
 */
public class TpSubCommand implements SubCommand {

    @Override public String getName() { return "tp"; }
    @Override public String getPermission() { return "dannouncements.tp"; }
    @Override public String getUsage() { return "/da tp <world> <x> <y> <z> [yaw] [pitch]"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.parse(Messages.get("player-only")));
            return;
        }
        if (args.length < 5) { CommandUtil.sendUsage(sender, getUsage()); return; }

        World world = Bukkit.getWorld(args[1]);
        if (world == null) {
            sender.sendMessage(ColorUtil.parse("<red>No such world: <white>" + args[1]));
            return;
        }

        try {
            double x = Double.parseDouble(args[2]);
            double y = Double.parseDouble(args[3]);
            double z = Double.parseDouble(args[4]);
            float yaw = args.length >= 6 ? Float.parseFloat(args[5]) : player.getLocation().getYaw();
            float pitch = args.length >= 7 ? Float.parseFloat(args[6]) : player.getLocation().getPitch();

            player.teleport(new Location(world, x, y, z, yaw, pitch));
        } catch (NumberFormatException e) {
            CommandUtil.sendUsage(sender, getUsage());
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> worlds = new ArrayList<>();
            for (World w : Bukkit.getWorlds()) worlds.add(w.getName());
            return worlds;
        }
        return List.of();
    }
}
