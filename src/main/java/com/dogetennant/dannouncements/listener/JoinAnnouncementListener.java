package com.dogetennant.dannouncements.listener;

import com.dogetennant.dannouncements.announcement.Announcement;
import com.dogetennant.dannouncements.announcement.AnnouncementConfigLoader;
import com.dogetennant.dannouncements.dispatch.AnnouncementDispatcher;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

public class JoinAnnouncementListener implements Listener {

    private final Plugin plugin;
    private final AnnouncementConfigLoader loader;
    private final AnnouncementDispatcher dispatcher;

    public JoinAnnouncementListener(Plugin plugin, AnnouncementConfigLoader loader, AnnouncementDispatcher dispatcher) {
        this.plugin = plugin;
        this.loader = loader;
        this.dispatcher = dispatcher;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        for (Announcement a : loader.getAll().values()) {
            if (!a.enabled || !a.join.enabled) continue;
            if (!isPermitted(a, player)) continue;

            long delayTicks = Math.max(0, a.join.delaySeconds) * 20L;
            if (delayTicks <= 0) {
                dispatcher.dispatch(a, player);
            } else {
                String id = a.id;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!player.isOnline()) return;
                    // as it is now: switched off, removed or reloaded during the delay
                    loader.get(id)
                            .filter(now -> now.enabled && now.join.enabled && isPermitted(now, player))
                            .ifPresent(now -> dispatcher.dispatch(now, player));
                }, delayTicks);
            }
        }
    }

    private boolean isPermitted(Announcement a, Player player) {
        if (a.permission != null && !a.permission.isBlank() && !player.hasPermission(a.permission)) return false;
        if (a.join.excludeOps && player.isOp()) return false;
        for (String perm : a.join.excludedPermissions) {
            if (player.hasPermission(perm)) return false;
        }
        return true;
    }
}
