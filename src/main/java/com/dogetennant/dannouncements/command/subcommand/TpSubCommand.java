package com.dogetennant.dannouncements.command.subcommand;

import com.dogetennant.dannouncements.announcement.TpDestinations;
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
 * Self-only teleport backing the [label](tp:...) links in announcements. A click runs as the
 * clicking player, so this has to work without op - which is exactly why it is not a free
 * teleport: a player can only reach a destination some announcement publishes, and only one
 * they'd have been shown (see TpDestinations).
 *
 * Arbitrary coordinates need dannouncements.admin.tp, which also un-hides the command from
 * /da help and tab-completion. Players without it get one flat "not available" reply whatever
 * goes wrong, so probing can't be used to map out world names.
 */
public class TpSubCommand implements SubCommand {

    private static final String ADMIN_PERMISSION = "dannouncements.admin.tp";

    /** Mirrors config.yml - see Messages.getOrDefault for why it's repeated here. */
    private static final String UNKNOWN_DESTINATION_DEFAULT =
            "<red>That teleport destination isn't available.";

    @Override public String getName() { return "tp"; }
    @Override public String getPermission() { return "dannouncements.tp"; }
    @Override public String getUsage() { return "/da tp <world> <x> <y> <z> [yaw] [pitch]"; }

    /** Nothing a player is meant to type - it's the target of a click link. */
    @Override
    public boolean isHiddenFrom(CommandSender sender) {
        return !sender.hasPermission(ADMIN_PERMISSION);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.parse(Messages.get("player-only")));
            return;
        }

        boolean admin = player.hasPermission(ADMIN_PERMISSION);
        if (args.length < 5) {
            reject(player, admin);
            return;
        }

        double x;
        double y;
        double z;
        float yaw;
        float pitch;
        try {
            x = Double.parseDouble(args[2]);
            y = Double.parseDouble(args[3]);
            z = Double.parseDouble(args[4]);
            yaw = args.length >= 6 ? Float.parseFloat(args[5]) : player.getLocation().getYaw();
            pitch = args.length >= 7 ? Float.parseFloat(args[6]) : player.getLocation().getPitch();
        } catch (NumberFormatException e) {
            reject(player, admin);
            return;
        }

        World world = Bukkit.getWorld(args[1]);
        if (world == null) {
            if (admin) sender.sendMessage(ColorUtil.parse("<red>No such world: <white>" + args[1]));
            else sender.sendMessage(ColorUtil.parse(Messages.getOrDefault("tp-unknown-destination", UNKNOWN_DESTINATION_DEFAULT)));
            return;
        }

        if (!admin) {
            TpDestinations.Result result = TpDestinations.check(player, world.getName(), x, y, z);
            if (result == TpDestinations.Result.NOT_PUBLISHED) {
                player.sendMessage(ColorUtil.parse(Messages.getOrDefault("tp-unknown-destination", UNKNOWN_DESTINATION_DEFAULT)));
                return;
            }
            if (result == TpDestinations.Result.NO_PERMISSION) {
                player.sendMessage(ColorUtil.parse(Messages.get("no-permission")));
                return;
            }
        }

        player.teleport(new Location(world, x, y, z, yaw, pitch));
    }

    /** Usage is only worth printing to someone allowed to type coordinates in the first place. */
    private void reject(Player player, boolean admin) {
        if (admin) CommandUtil.sendUsage(player, getUsage());
        else player.sendMessage(ColorUtil.parse(Messages.getOrDefault("tp-unknown-destination", UNKNOWN_DESTINATION_DEFAULT)));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) return List.of();
        if (args.length == 2) {
            List<String> worlds = new ArrayList<>();
            for (World w : Bukkit.getWorlds()) worlds.add(w.getName());
            return worlds;
        }
        return List.of();
    }
}
