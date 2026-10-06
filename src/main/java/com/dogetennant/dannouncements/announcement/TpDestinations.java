package com.dogetennant.dannouncements.announcement;

import com.dogetennant.dannouncements.DAnnouncements;
import com.dogetennant.dannouncements.util.LinkUtil;
import org.bukkit.entity.Player;

/**
 * The gate behind /da tp: a player can only be sent somewhere an announcement actually
 * publishes through a tp: link, and only if that announcement's permission would have let
 * them see it in the first place. Without this, a command that has to stay usable without op
 * (click links run as the clicking player) is a free teleport to any coordinates in any
 * loaded world.
 *
 * Read off the live announcement model on every teleport rather than cached, so /da reload
 * and in-game /da line edits take effect immediately. Announcements are few and teleports
 * happen at click rate, so the scan is cheap.
 */
public final class TpDestinations {

    public enum Result {
        /** Published by an announcement this player is allowed to see. */
        ALLOWED,
        /** No announcement offers this destination - hand-typed, or left over from an edited line. */
        NOT_PUBLISHED,
        /** Published, but only by announcements gated behind a permission this player lacks. */
        NO_PERMISSION
    }

    private TpDestinations() {}

    public static Result check(Player player, String world, double x, double y, double z) {
        boolean published = false;

        for (Announcement a : DAnnouncements.getInstance().getAnnouncementConfigLoader().getAll().values()) {
            for (String line : a.lines) {
                for (String target : LinkUtil.extractTpTargets(line)) {
                    if (!matches(target, world, x, y, z)) continue;
                    published = true;
                    if (canSee(player, a)) return Result.ALLOWED;
                }
            }
        }

        return published ? Result.NO_PERMISSION : Result.NOT_PUBLISHED;
    }

    private static boolean canSee(Player player, Announcement a) {
        return a.permission == null || a.permission.isBlank() || player.hasPermission(a.permission);
    }

    /** Yaw and pitch are only a facing, so they take no part in the match. */
    private static boolean matches(String target, String world, double x, double y, double z) {
        String[] parts = target.split(" ");
        if (parts.length < 4) return false;
        if (!parts[0].equals(world)) return false;
        try {
            return Double.compare(Double.parseDouble(parts[1]), x) == 0
                    && Double.compare(Double.parseDouble(parts[2]), y) == 0
                    && Double.compare(Double.parseDouble(parts[3]), z) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
