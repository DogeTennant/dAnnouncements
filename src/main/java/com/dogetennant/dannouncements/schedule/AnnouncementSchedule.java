package com.dogetennant.dannouncements.schedule;

import org.bukkit.configuration.ConfigurationSection;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnnouncementSchedule {

    private boolean enabled;
    private ScheduleType type;
    private int hour;
    private int minute;
    private int intervalMinutes;      // INTERVAL only
    private DayOfWeek dayOfWeek;      // WEEKLY only
    private int dayOfMonth;           // MONTHLY only
    private String specificDate;      // SPECIFIC only, "yyyy-MM-dd" (paired with hour/minute)
    private List<int[]> times;        // DAILY only: extra {hour, minute} slots beyond hour/minute above

    // As written in announcements.yml: saved back unchanged, and named when one is wrong
    private String timeText;
    private List<String> timesText;
    private String dayText;

    public AnnouncementSchedule(boolean enabled, ScheduleType type, int hour, int minute,
                                 int intervalMinutes, DayOfWeek dayOfWeek, int dayOfMonth,
                                 String specificDate, List<int[]> times) {
        this.enabled = enabled;
        this.type = type;
        this.hour = hour;
        this.minute = minute;
        this.intervalMinutes = Math.max(1, intervalMinutes);
        this.dayOfWeek = dayOfWeek;
        this.dayOfMonth = dayOfMonth;
        this.specificDate = specificDate;
        this.times = times != null ? times : Collections.emptyList();
        this.timeText = format(hour, minute);
        this.timesText = this.times.stream().map(t -> format(t[0], t[1])).toList();
        this.dayText = dayOfWeek != null ? dayOfWeek.name() : null;
    }

    public static AnnouncementSchedule fromConfig(ConfigurationSection section) {
        if (section == null) {
            return new AnnouncementSchedule(false, ScheduleType.DAILY, 9, 0, 60, null, 1, null, null);
        }

        boolean enabled = section.getBoolean("enabled", false);
        ScheduleType type;
        try {
            type = ScheduleType.valueOf(section.getString("type", "DAILY").toUpperCase());
        } catch (IllegalArgumentException e) {
            type = ScheduleType.DAILY;
        }

        String time = section.getString("time", "09:00");
        int[] parsedTime = parseTime(time);

        DayOfWeek dayOfWeek = null;
        String day = section.getString("day");
        if (day != null) {
            try { dayOfWeek = DayOfWeek.valueOf(day.trim().toUpperCase()); } catch (Exception ignored) {}
        }

        int intervalMinutes = section.getInt("interval-minutes", 60);
        int dayOfMonth = section.getInt("day-of-month", 1);
        String specificDate = section.getString("date");

        List<String> timesText = section.getStringList("times");
        List<int[]> times = new ArrayList<>();
        for (String raw : timesText) {
            times.add(parseTime(raw));
        }

        AnnouncementSchedule schedule = new AnnouncementSchedule(enabled, type, parsedTime[0], parsedTime[1],
                intervalMinutes, dayOfWeek, dayOfMonth, specificDate, times);
        schedule.timeText = time;
        schedule.timesText = List.copyOf(timesText);
        schedule.dayText = day;
        return schedule;
    }

    /** Writes into the announcement's existing "schedule" section, so the admin's own spelling stays. */
    public void writeTo(ConfigurationSection section) {
        section.set("enabled", enabled);
        section.set("type", type.name());
        section.set("time", timeText);
        section.set("interval-minutes", intervalMinutes);
        if (dayText != null) section.set("day", dayText);
        section.set("day-of-month", dayOfMonth);
        if (specificDate != null) section.set("date", specificDate);
        if (!timesText.isEmpty()) section.set("times", timesText);
    }

    /** {hour, minute}; {-1, -1} when it is not "HH:mm" (the problem() names it). */
    private static int[] parseTime(String time) {
        String[] parts = time.trim().split(":");
        if (parts.length != 2) return new int[]{-1, -1};
        return new int[]{safeInt(parts[0]), safeInt(parts[1])};
    }

    private static int safeInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return -1; }
    }

    private static String format(int hour, int minute) {
        return String.format("%02d:%02d", hour, minute);
    }

    private static boolean validTime(int hour, int minute) {
        return hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59;
    }

    /**
     * What makes this schedule impossible to follow, in words for the console; null when it can
     * be. An impossible one only keeps its own announcement from being scheduled.
     */
    public String problem() {
        if (type == ScheduleType.INTERVAL) return null;
        if (type == ScheduleType.DAILY && !times.isEmpty()) {
            for (int i = 0; i < times.size(); i++) {
                if (!validTime(times.get(i)[0], times.get(i)[1])) {
                    return "times entry '" + timesText.get(i) + "' is not a time of day (HH:mm, 00:00 to 23:59)";
                }
            }
            return null;
        }
        if (!validTime(hour, minute)) return "time '" + timeText + "' is not a time of day (HH:mm, 00:00 to 23:59)";
        return switch (type) {
            case WEEKLY -> dayText != null && dayOfWeek == null ? "day '" + dayText + "' is not a day of the week" : null;
            case MONTHLY -> dayOfMonth < 1 || dayOfMonth > 31 ? "day-of-month " + dayOfMonth + " is not 1 to 31" : null;
            case SPECIFIC -> {
                if (specificDate == null) yield "a SPECIFIC schedule needs a date (yyyy-MM-dd)";
                try {
                    LocalDate.parse(specificDate);
                    yield null;
                } catch (DateTimeParseException e) {
                    yield "date '" + specificDate + "' is not a date (yyyy-MM-dd)";
                }
            }
            default -> null;
        };
    }

    /** Equal for two schedules that fire at the same times: a countdown in progress can be kept. */
    public String key() {
        StringBuilder key = new StringBuilder(type.name()).append('|').append(hour).append(':').append(minute)
                .append('|').append(intervalMinutes).append('|').append(dayOfWeek).append('|').append(dayOfMonth)
                .append('|').append(specificDate);
        for (int[] t : times) key.append('|').append(t[0]).append(':').append(t[1]);
        return key.toString();
    }

    /** DAILY only: every clock time this should fire at each day, in "HH:mm" form. Falls back to
     *  the single hour/minute (via "time") when no "times" list is configured. */
    public List<int[]> getEffectiveDailyTimes() {
        if (!times.isEmpty()) return times;
        return List.of(new int[]{hour, minute});
    }

    public boolean isEnabled() { return enabled; }
    public ScheduleType getType() { return type; }
    public int getHour() { return hour; }
    public int getMinute() { return minute; }
    public int getIntervalMinutes() { return intervalMinutes; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public int getDayOfMonth() { return dayOfMonth; }
    public String getSpecificDate() { return specificDate; }
    public List<int[]> getTimes() { return times; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
