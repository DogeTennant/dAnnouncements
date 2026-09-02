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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AnnouncementScheduler {

    private final Plugin plugin;
    private final AnnouncementConfigLoader loader;
    private final AnnouncementDispatcher dispatcher;

    private ZoneId timezone;
    private long pollIntervalTicks;

    private final Map<String, Long> nextRuns = new ConcurrentHashMap<>();
    private BukkitTask pollTask;

    public AnnouncementScheduler(Plugin plugin, AnnouncementConfigLoader loader,
                                  AnnouncementDispatcher dispatcher, MainConfig config) {
        this.plugin = plugin;
        this.loader = loader;
        this.dispatcher = dispatcher;
        this.timezone = TimeUtil.resolveTimezone(config.timezone);
        this.pollIntervalTicks = Math.max(1, config.pollIntervalTicks);
    }

    public void load() {
        nextRuns.clear();
        loader.getAll().values().forEach(this::scheduleIfActive);

        poll();
        pollTask = Bukkit.getScheduler().runTaskTimer(plugin, this::poll, pollIntervalTicks, pollIntervalTicks);
        LogUtil.info("Scheduler active | timezone: " + timezone.getId() + " | tracking "
                + nextRuns.size() + " scheduled announcement(s).");
    }

    /** Called on /da reload - picks up config/timezone changes and recalculates every next-run. */
    public void reload(MainConfig config) {
        this.timezone = TimeUtil.resolveTimezone(config.timezone);
        this.pollIntervalTicks = Math.max(1, config.pollIntervalTicks);

        if (pollTask != null) pollTask.cancel();
        nextRuns.clear();
        loader.getAll().values().forEach(this::scheduleIfActive);

        pollTask = Bukkit.getScheduler().runTaskTimer(plugin, this::poll, pollIntervalTicks, pollIntervalTicks);
        poll();
    }

    public Optional<Long> getNextRun(String id) {
        return Optional.ofNullable(nextRuns.get(id));
    }

    private void scheduleIfActive(Announcement a) {
        if (!a.enabled || a.schedule == null || !a.schedule.isEnabled()) return;
        nextRuns.put(a.id, calculateNextRun(a.schedule));
    }

    private void poll() {
        long now = System.currentTimeMillis();

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
                a.enabled = false;
                loader.put(a);
                nextRuns.remove(id);
                LogUtil.info("Announcement '" + id + "' fired once and has been auto-disabled.");
            } else {
                nextRuns.put(id, next);
            }
        }
    }

    private boolean isSameLocalDate(long epochMs1, long epochMs2) {
        LocalDate d1 = java.time.Instant.ofEpochMilli(epochMs1).atZone(timezone).toLocalDate();
        LocalDate d2 = java.time.Instant.ofEpochMilli(epochMs2).atZone(timezone).toLocalDate();
        return d1.equals(d2);
    }

    private long calculateNextRun(AnnouncementSchedule schedule) {
        ZonedDateTime now = ZonedDateTime.now(timezone);
        ZonedDateTime next = switch (schedule.getType()) {
            case INTERVAL -> now.plusMinutes(schedule.getIntervalMinutes()).withSecond(0).withNano(0);
            case DAILY -> {
                ZonedDateTime best = null;
                for (int[] slot : schedule.getEffectiveDailyTimes()) {
                    ZonedDateTime candidate = now.withHour(slot[0]).withMinute(slot[1])
                            .withSecond(0).withNano(0);
                    if (candidate.isBefore(now)) candidate = candidate.plusDays(1);
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
                yield candidate.isBefore(now) ? candidate.plusWeeks(1) : candidate;
            }
            case MONTHLY -> {
                ZonedDateTime candidate = withClampedDayOfMonth(now, schedule.getDayOfMonth())
                        .withHour(schedule.getHour()).withMinute(schedule.getMinute())
                        .withSecond(0).withNano(0);
                if (candidate.isBefore(now)) {
                    ZonedDateTime nextMonthBase = now.plusMonths(1).withDayOfMonth(1);
                    candidate = withClampedDayOfMonth(nextMonthBase, schedule.getDayOfMonth())
                            .withHour(schedule.getHour()).withMinute(schedule.getMinute())
                            .withSecond(0).withNano(0);
                }
                yield candidate;
            }
            case SPECIFIC -> {
                if (schedule.getSpecificDate() == null) yield now.plusYears(100);
                try {
                    LocalDate date = LocalDate.parse(schedule.getSpecificDate());
                    LocalDateTime ldt = LocalDateTime.of(date,
                            LocalTime.of(schedule.getHour(), schedule.getMinute()));
                    yield ldt.atZone(timezone);
                } catch (Exception e) {
                    LogUtil.warn("Invalid schedule date '" + schedule.getSpecificDate()
                            + "' (expected yyyy-MM-dd) - this announcement will never auto-fire.");
                    yield now.plusYears(100);
                }
            }
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
