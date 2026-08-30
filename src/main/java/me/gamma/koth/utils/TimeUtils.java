package me.gamma.koth.utils;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class TimeUtils {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static String formatTime(long seconds) {
        long hours = TimeUnit.SECONDS.toHours(seconds);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60;
        long secs = seconds % 60;
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%02d:%02d", minutes, secs);
        }
    }

    public static String formatTimeLong(long seconds) {
        if (seconds <= 0) return "0s";
        
        long hours = TimeUnit.SECONDS.toHours(seconds);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60;
        long secs = seconds % 60;
        
        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0) {
            sb.append(String.format("%02d", minutes)).append("m ");
        }
        sb.append(String.format("%02d", secs)).append("s");
        
        return sb.toString().trim();
    }

    public static String formatTimeShort(long seconds) {
        long minutes = TimeUnit.SECONDS.toMinutes(seconds);
        long secs = seconds % 60;
        
        return String.format("%02d:%02d", minutes, secs);
    }

    public static long parseTime(String time) {
        try {
            return Long.parseLong(time);
        } catch (NumberFormatException e) {
            String[] parts = time.split(":");
            if (parts.length == 2) {
                return Integer.parseInt(parts[0]) * 60L + Integer.parseInt(parts[1]);
            } else if (parts.length == 3) {
                return Integer.parseInt(parts[0]) * 3600L + 
                       Integer.parseInt(parts[1]) * 60L + 
                       Integer.parseInt(parts[2]);
            }
            return 0;
        }
    }

    public static LocalTime parseScheduleTime(String time) {
        return LocalTime.parse(time, TIME_FORMATTER);
    }

    public static DayOfWeek parseDay(String day) {
        try {
            if (day.equalsIgnoreCase("DAILY")) {
                return null;
            }
            return DayOfWeek.valueOf(day.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String formatDay(DayOfWeek day) {
        if (day == null) return "DAILY";
        return day.name();
    }

    public static boolean isSameDay(DayOfWeek day, java.time.LocalDate date) {
        return day == null || date.getDayOfWeek() == day;
    }

    public static long getSecondsUntil(LocalTime time) {
        LocalTime now = LocalTime.now();
        long seconds = time.toSecondOfDay() - now.toSecondOfDay();
        if (seconds < 0) {
            seconds += 24 * 60 * 60;
        }
        return seconds;
    }

    public static String formatTimeLeft(long seconds) {
        if (seconds <= 0) return "00:00";
        
        long days = TimeUnit.SECONDS.toDays(seconds);
        long hours = TimeUnit.SECONDS.toHours(seconds) % 24;
        long minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60;
        long secs = seconds % 60;
        
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        sb.append(String.format("%02d:%02d", minutes, secs));
        
        return sb.toString().trim();
    }
}