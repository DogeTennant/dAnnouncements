package com.dogetennant.dannouncements.dispatch;

import com.dogetennant.dannouncements.announcement.Announcement;
import com.dogetennant.dannouncements.integration.PlaceholderHook;
import com.dogetennant.dannouncements.util.CenterUtil;
import com.dogetennant.dannouncements.util.ColorUtil;
import com.dogetennant.dannouncements.util.LinkUtil;
import com.dogetennant.dannouncements.util.LogUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnnouncementDispatcher {

    private final Plugin plugin;
    /** When each announcement last reached someone (epoch ms): its links stay usable a while after. */
    private final Map<String, Long> lastSent = new ConcurrentHashMap<>();

    public AnnouncementDispatcher(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Sends the announcement to a single player (preview/force-to-player), or broadcasts to
     * every permitted online player when {@code target} is null. Returns the recipient count.
     */
    public int dispatch(Announcement a, Player target) {
        List<Player> recipients = target != null ? List.of(target) : onlinePermitted(a);
        if (recipients.isEmpty()) return 0;
        lastSent.put(a.id, System.currentTimeMillis());

        Sound sound = resolveSound(a);

        for (Player p : recipients) {
            List<String> resolvedLines = resolveLines(a, p);

            if (a.chat.enabled) sendChat(a, p, resolvedLines);
            if (a.title.enabled) sendTitle(a, p, resolvedLines);
            if (a.actionbar.enabled) sendActionbar(a, p, resolvedLines);
            if (a.sound.enabled && sound != null) {
                p.playSound(p.getLocation(), sound, (float) a.sound.volume, (float) a.sound.pitch);
            }
        }

        if (a.bossbar.enabled) sendBossbar(a, recipients);

        return recipients.size();
    }

    /** Whether the announcement reached anyone within the last {@code window}. */
    public boolean sentWithin(String id, Duration window) {
        Long sent = lastSent.get(id);
        return sent != null && System.currentTimeMillis() - sent <= window.toMillis();
    }

    private List<Player> onlinePermitted(Announcement a) {
        List<Player> out = new ArrayList<>();
        boolean requiresPermission = a.permission != null && !a.permission.isBlank();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!requiresPermission || p.hasPermission(a.permission)) out.add(p);
        }
        return out;
    }

    private List<String> resolveLines(Announcement a, Player p) {
        List<String> resolved = new ArrayList<>(a.lines.size());
        for (String line : a.lines) {
            resolved.add(LinkUtil.autoLinkify(PlaceholderHook.resolve(p, line)));
        }
        return resolved;
    }

    private void sendChat(Announcement a, Player p, List<String> lines) {
        Component border = a.chat.border
                ? ColorUtil.parse(a.chat.borderColor + a.chat.borderChar.repeat(Math.max(1, a.chat.borderLength)))
                : null;

        if (border != null) p.sendMessage(border);
        for (String line : lines) {
            Component c = ColorUtil.parse(line);
            p.sendMessage(a.chat.center ? CenterUtil.center(c) : c);
        }
        if (border != null) p.sendMessage(border);
    }

    private void sendTitle(Announcement a, Player p, List<String> resolvedLines) {
        String titleText = a.title.text.isBlank()
                ? firstOr(resolvedLines, 0, "") : PlaceholderHook.resolve(p, a.title.text);
        String subtitleText = a.title.subtitle.isBlank()
                ? firstOr(resolvedLines, 1, "") : PlaceholderHook.resolve(p, a.title.subtitle);

        Title.Times times = Title.Times.times(
                Duration.ofMillis(a.title.fadeIn * 50L),
                Duration.ofMillis(a.title.stay * 50L),
                Duration.ofMillis(a.title.fadeOut * 50L));

        p.showTitle(Title.title(ColorUtil.parse(titleText), ColorUtil.parse(subtitleText), times));
    }

    private void sendActionbar(Announcement a, Player p, List<String> resolvedLines) {
        String text = a.actionbar.text.isBlank()
                ? firstOr(resolvedLines, 0, "") : PlaceholderHook.resolve(p, a.actionbar.text);
        p.sendActionBar(ColorUtil.parse(text));
    }

    private void sendBossbar(Announcement a, List<Player> recipients) {
        String raw = a.bossbar.text.isBlank() ? firstOr(a.lines, 0, "") : a.bossbar.text;
        BossBar.Color color = parseEnum(BossBar.Color.class, a.bossbar.color, BossBar.Color.YELLOW);
        BossBar.Overlay overlay = parseEnum(BossBar.Overlay.class, a.bossbar.overlay, BossBar.Overlay.PROGRESS);
        long hideAfterTicks = Math.max(1, a.bossbar.seconds) * 20L;

        for (Player p : recipients) {
            Component text = ColorUtil.parse(PlaceholderHook.resolve(p, raw));
            BossBar bar = BossBar.bossBar(text, 1.0f, color, overlay);
            p.showBossBar(bar);
            Bukkit.getScheduler().runTaskLater(plugin, () -> p.hideBossBar(bar), hideAfterTicks);
        }
    }

    private Sound resolveSound(Announcement a) {
        if (!a.sound.enabled) return null;
        try {
            return Sound.valueOf(a.sound.name);
        } catch (IllegalArgumentException e) {
            LogUtil.warn("Invalid sound name '" + a.sound.name + "' on announcement '" + a.id + "' - skipping sound.");
            return null;
        }
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> type, String value, T fallback) {
        try {
            return Enum.valueOf(type, value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String firstOr(List<String> lines, int index, String fallback) {
        return index < lines.size() ? lines.get(index) : fallback;
    }
}
