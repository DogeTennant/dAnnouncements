package com.dogetennant.dannouncements.schedule;

import org.bukkit.configuration.ConfigurationSection;

import java.time.DayOfWeek;
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
            try { dayOfWeek = DayOfWeek.valueOf(day.toUpperCase()); } catch (Exception ignored) {}
        }

        int intervalMinutes = section.getInt("interval-minutes", 60);
        int dayOfMonth = section.getInt("day-of-month", 1);
        String specificDate = section.getString("date");

        List<int[]> times = new ArrayList<>();
        for (String raw : section.getStringList("times")) {
            times.add(parseTime(raw));
        }

        return new AnnouncementSchedule(enabled, type, parsedTime[0], parsedTime[1], intervalMinutes,
                dayOfWeek, dayOfMonth, specificDate, times);
    }

    public void writeTo(ConfigurationSection section) {
        section.set("enabled", enabled);
        section.set("type", type.name());
        section.set("time", String.format("%02d:%02d", hour, minute));
        section.set("interval-minutes", intervalMinutes);
        if (dayOfWeek != null) section.set("day", dayOfWeek.name());
        section.set("day-of-month", dayOfMonth);
        if (specificDate != null) section.set("date", specificDate);
        if (!times.isEmpty()) {
            List<String> out = new ArrayList<>();
            for (int[] t : times) out.add(String.format("%02d:%02d", t[0], t[1]));
            section.set("times", out);
        }
    }

    private static int[] parseTime(String time) {
        String[] parts = time.split(":");
        int hour = parts.length > 0 ? safeInt(parts[0], 9) : 9;
        int minute = parts.length > 1 ? safeInt(parts[1], 0) : 0;
        return new int[]{hour, minute};
    }

    private static int safeInt(String s, int fallback) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return fallback; }
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
