package com.dogetennant.dannouncements.schedule;

import com.dogetennant.dannouncements.announcement.Announcement;
import com.dogetennant.dannouncements.announcement.AnnouncementConfigLoader;
import com.dogetennant.dannouncements.config.MainConfig;
import com.dogetennant.dannouncements.dispatch.AnnouncementDispatcher;
import com.dogetennant.dannouncements.util.LogUtil;
import com.dogetennant.dannouncements.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AnnouncementScheduler {

    /** A SPECIFIC date missed by at most this much (a restart right then) is still sent. */
    static final Duration LATE_LIMIT = Duration.ofMinutes(10);

    private final Plugin plugin;
    private final AnnouncementConfigLoader loader;
    private final AnnouncementDispatcher dispatcher;
    private final Clock clock;

    private ZoneId timezone;
    private long pollIntervalTicks;

    private final Map<String, Long> nextRuns = new ConcurrentHashMap<>();
    /** The schedule each next run was worked out for: an unchanged one keeps its countdown. */
    private final Map<String, String> scheduledAs = new HashMap<>();
    /** Schedule problems already in the console, so each is reported once. */
    private final Map<String, String> reported = new HashMap<>();
    private BukkitTask pollTask;

    public AnnouncementScheduler(Plugin plugin, AnnouncementConfigLoader loader,
                                  AnnouncementDispatcher dispatcher, MainConfig config) {
        this(plugin, loader, dispatcher, config, Clock.systemUTC());
    }

    AnnouncementScheduler(Plugin plugin, AnnouncementConfigLoader loader,
                          AnnouncementDispatcher dispatcher, MainConfig config, Clock clock) {
        this.plugin = plugin;
        this.loader = loader;
        this.dispatcher = dispatcher;
        this.clock = clock;
        this.timezone = TimeUtil.resolveTimezone(config.timezone);
        this.pollIntervalTicks = Math.max(1, config.pollIntervalTicks);
    }

    public void load() {
        nextRuns.clear();
        scheduledAs.clear();
        refresh();

        poll();
        pollTask = Bukkit.getScheduler().runTaskTimer(plugin, this::poll, pollIntervalTicks, pollIntervalTicks);
        LogUtil.info("Scheduler active | timezone: " + timezone.getId() + " | tracking "
                + nextRuns.size() + " scheduled announcement(s).");
    }

    /**
     * Called on /da reload - picks up config/timezone changes and announcement changes. A
     * countdown in progress is kept unless its schedule (or the timezone) changed.
     */
    public void reload(MainConfig config) {
        ZoneId previous = timezone;
        this.timezone = TimeUtil.resolveTimezone(config.timezone);
        this.pollIntervalTicks = Math.max(1, config.pollIntervalTicks);

        if (pollTask != null) pollTask.cancel();
        if (!timezone.equals(previous)) {
            nextRuns.clear();
            scheduledAs.clear();
        }
        reported.clear();                               // a reload shows every problem again
        try {
            refresh();
        } finally {
            pollTask = Bukkit.getScheduler().runTaskTimer(plugin, this::poll, pollIntervalTicks, pollIntervalTicks);
        }
        poll();
    }

    /**
     * Brings the next runs in line with the announcements: new or changed schedules are worked
     * out, unchanged ones keep their countdown, switched-off ones are dropped. Called after every
     * in-game change.
     */
    public void refresh() {
        long now = clock.millis();
        Set<String> active = new HashSet<>();
        List<String> missed = new ArrayList<>();
        for (Announcement a : loader.getAll().values()) {
            if (!a.enabled || a.schedule == null || !a.schedule.isEnabled()) continue;
            String problem = a.schedule.problem();
            if (problem != null) {
                if (!problem.equals(reported.put(a.id, problem))) {
                    LogUtil.warn("Announcement '" + a.id + "' is not scheduled: " + problem + ".");
                }
                continue;
            }
            reported.remove(a.id);
            String key = a.schedule.key();
            if (key.equals(scheduledAs.get(a.id)) && nextRuns.containsKey(a.id)) {
                active.add(a.id);
                continue;
            }
            long next;
            try {
                next = calculateNextRun(a.schedule);
            } catch (DateTimeException e) {
                LogUtil.warn("Announcement '" + a.id + "' is not scheduled: " + e.getMessage() + ".");
                continue;
            }
            if (a.schedule.getType() == ScheduleType.SPECIFIC && next < now - LATE_LIMIT.toMillis()) {
                missed.add(a.id);
                continue;
            }
            nextRuns.put(a.id, next);
            scheduledAs.put(a.id, key);
            active.add(a.id);
        }
        nextRuns.keySet().retainAll(active);
        scheduledAs.keySet().retainAll(active);

        for (String id : missed) {
            LogUtil.warn("Announcement '" + id + "' was due more than " + LATE_LIMIT.toMinutes()
                    + " minutes ago (the server was off or it was set in the past) - switched off without sending it.");
            switchOff(id);
        }
    }

    public Optional<Long> getNextRun(String id) {
        return Optional.ofNullable(nextRuns.get(id));
    }

    public ZoneId getTimezone() {
        return timezone;
    }

    private void poll() {
        long now = clock.millis();

        for (Map.Entry<String, Long> entry : new ArrayList<>(nextRuns.entrySet())) {
            if (entry.getValue() > now) continue;
            String id = entry.getKey();

            Optional<Announcement> found = loader.get(id);
            if (found.isEmpty()) {
                nextRuns.remove(id);
                continue;
            }

            Announcement a = found.get();
            if (!a.enabled || a.schedule == null || !a.schedule.isEnabled()) {
                nextRuns.remove(id);
                continue;
            }

            dispatcher.dispatch(a, null);

            long next = calculateNextRun(a.schedule);

            // For DAILY schedules with multiple "times" slots, only the last slot of the day
            // counts as exhausting the day - earlier slots just move on to the next slot today.
            boolean daySlotsExhausted = a.schedule.getType() != ScheduleType.DAILY
                    || !isSameLocalDate(now, next);

            boolean disableNow = a.schedule.getType() == ScheduleType.SPECIFIC
                    || (a.once && daySlotsExhausted);

            if (disableNow) {
                nextRuns.remove(id);
                scheduledAs.remove(id);
                a.enabled = false;                      // stays off even if the file cannot be saved
                switchOff(id);
                LogUtil.info("Announcement '" + id + "' fired once and has been auto-disabled.");
            } else {
                nextRuns.put(id, next);
            }
        }
    }

    private void switchOff(String id) {
        AnnouncementConfigLoader.Change change = loader.update(id, a -> {
            a.enabled = false;
            return true;
        });
        if (change != AnnouncementConfigLoader.Change.SAVED && change != AnnouncementConfigLoader.Change.NOT_FOUND) {
            LogUtil.warn("Could not switch '" + id + "' off in announcements.yml (" + change
                    + ") - it is off until the next reload.");
            loader.get(id).ifPresent(a -> a.enabled = false);
        }
    }

    private boolean isSameLocalDate(long epochMs1, long epochMs2) {
        LocalDate d1 = Instant.ofEpochMilli(epochMs1).atZone(timezone).toLocalDate();
        LocalDate d2 = Instant.ofEpochMilli(epochMs2).atZone(timezone).toLocalDate();
        return d1.equals(d2);
    }

    /** Only for schedules without a problem(). */
    private long calculateNextRun(AnnouncementSchedule schedule) {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(timezone);
        ZonedDateTime next = switch (schedule.getType()) {
            case INTERVAL -> now.plusMinutes(schedule.getIntervalMinutes()).withSecond(0).withNano(0);
            case DAILY -> {
                ZonedDateTime best = null;
                for (int[] slot : schedule.getEffectiveDailyTimes()) {
                    ZonedDateTime candidate = now.withHour(slot[0]).withMinute(slot[1])
                            .withSecond(0).withNano(0);
                    if (!candidate.isAfter(now)) candidate = candidate.plusDays(1);
                    if (best == null || candidate.isBefore(best)) best = candidate;
                }
                yield best;
            }
            case WEEKLY -> {
                DayOfWeek target = schedule.getDayOfWeek() != null
                        ? schedule.getDayOfWeek() : DayOfWeek.MONDAY;
                ZonedDateTime candidate = now.with(target)
                        .withHour(schedule.getHour()).withMinute(schedule.getMinute())
                        .withSecond(0).withNano(0);
                yield candidate.isAfter(now) ? candidate : candidate.plusWeeks(1);
            }
            case MONTHLY -> {
                ZonedDateTime candidate = withClampedDayOfMonth(now, schedule.getDayOfMonth())
                        .withHour(schedule.getHour()).withMinute(schedule.getMinute())
                        .withSecond(0).withNano(0);
                if (!candidate.isAfter(now)) {
                    ZonedDateTime nextMonthBase = now.plusMonths(1).withDayOfMonth(1);
                    candidate = withClampedDayOfMonth(nextMonthBase, schedule.getDayOfMonth())
                            .withHour(schedule.getHour()).withMinute(schedule.getMinute())
                            .withSecond(0).withNano(0);
                }
                yield candidate;
            }
            case SPECIFIC -> LocalDateTime.of(LocalDate.parse(schedule.getSpecificDate()),
                    LocalTime.of(schedule.getHour(), schedule.getMinute())).atZone(timezone);
        };
        return next.toInstant().toEpochMilli();
    }

    // Clamps to the last valid day of the target month (e.g. day-of-month 31 in February -> 28/29)
    // instead of letting withDayOfMonth() throw on months that don't have that many days.
    private static ZonedDateTime withClampedDayOfMonth(ZonedDateTime base, int dayOfMonth) {
        int clamped = Math.min(dayOfMonth, base.toLocalDate().lengthOfMonth());
        return base.withDayOfMonth(clamped);
    }

    public void shutdown() {
        if (pollTask != null) pollTask.cancel();
    }
}
