package me.gamma.koth.koth;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.config.MessagesManager;
import me.gamma.koth.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ScheduleManager {

    private final KoTHPlugin plugin;
    private final Map<Integer, Schedule> schedules = new ConcurrentHashMap<>();

    public ScheduleManager(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadSchedules() {
        schedules.clear();
        
        Map<Integer, Schedule> loadedSchedules = plugin.getKothDataManager().loadAllSchedules();
        schedules.putAll(loadedSchedules);
        
        plugin.getLogger().info("Loaded " + schedules.size() + " schedules.");
    }

    public void tick() {
        String timeZone = plugin.getConfigManager().getTimeZone();
        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(timeZone);
        } catch (Exception e) {
            zoneId = ZoneId.systemDefault();
        }
        
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();
        
        for (Schedule schedule : schedules.values()) {
            if (shouldStart(schedule, today, currentTime)) {
                startScheduledKoTH(schedule);
            }
        }
    }

    private boolean shouldStart(Schedule schedule, LocalDate date, LocalTime time) {
        if (schedule.getDay() == null) {
            return isTimeMatch(schedule.getTime(), time);
        } else {
            return date.getDayOfWeek() == schedule.getDay() && 
                   isTimeMatch(schedule.getTime(), time);
        }
    }

    private boolean isTimeMatch(LocalTime scheduledTime, LocalTime currentTime) {
        return scheduledTime.getHour() == currentTime.getHour() &&
               scheduledTime.getMinute() == currentTime.getMinute() &&
               currentTime.getSecond() == 0;
    }

    private void startScheduledKoTH(Schedule schedule) {
        KoTH koth = plugin.getKothManager().getKoTH(schedule.getKothId());
        if (koth == null || !koth.isEnabled() || koth.isActive()) {
            return;
        }
        
        boolean started = plugin.getKothManager().startKoTH(
            koth, 
            schedule.getCaptureTime(), 
            schedule.getMaxTime()
        );
        
        if (started) {
            MessagesManager messagesManager = plugin.getMessagesManager();
            
            String x = "N/A", y = "N/A", z = "N/A";
            if (koth.getCenter() != null) {
                x = String.valueOf((int) koth.getCenter().getX());
                y = String.valueOf((int) koth.getCenter().getY());
                z = String.valueOf((int) koth.getCenter().getZ());
            }
            
            List<String> startMessages = messagesManager.getMessageList("KOTH_STARTING",
                    "%player%", "Console",
                    "%koth%", koth.getName(),
                    "%x%", x,
                    "%y%", y,
                    "%z%", z);
            
            for (String message : startMessages) {
                broadcastMessage(message);
            }
        }
    }

    public Schedule createSchedule(int kothId, String day, String time, int captureTime, int maxTime) {
        DayOfWeek parsedDay = TimeUtils.parseDay(day);
        LocalTime parsedTime = TimeUtils.parseScheduleTime(time);
        
        if (scheduleExists(kothId, parsedDay, parsedTime)) {
            return null;
        }
        
        int nextId = getNextAvailableId();
        Schedule schedule = new Schedule(nextId, kothId, parsedDay, parsedTime, captureTime, maxTime);
        schedules.put(nextId, schedule);
        plugin.getKothDataManager().saveSchedule(schedule);
        return schedule;
    }

    // Comprueba si ya existe un schedule idéntico (mismo KoTH, mismo día,
    public boolean scheduleExists(int kothId, String dayStr, String timeStr) {
        DayOfWeek day = TimeUtils.parseDay(dayStr);
        LocalTime time = TimeUtils.parseScheduleTime(timeStr);
        return scheduleExists(kothId, day, time);
    }

    public boolean scheduleExists(int kothId, DayOfWeek day, LocalTime time) {
        for (Schedule schedule : schedules.values()) {
            if (schedule.getKothId() == kothId
                    && Objects.equals(schedule.getDay(), day)
                    && schedule.getTime().equals(time)) {
                return true;
            }
        }
        return false;
    }

    public boolean removeSchedule(int id) {
        Schedule schedule = schedules.remove(id);
        if (schedule != null) {
            plugin.getKothDataManager().deleteSchedule(id);
            return true;
        }
        return false;
    }

    public Schedule getSchedule(int id) {
        return schedules.get(id);
    }

    public Collection<Schedule> getAllSchedules() {
        return Collections.unmodifiableCollection(schedules.values());
    }

    public List<Schedule> getSortedSchedules() {
        return schedules.values().stream()
                .sorted(Comparator.comparing(Schedule::getDay, 
                        Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Schedule::getTime))
                .collect(Collectors.toList());
    }

    public void reloadSchedules() {
        loadSchedules();
    }

    private int getNextAvailableId() {
        int maxId = 0;
        for (int id : schedules.keySet()) {
            if (id > maxId) {
                maxId = id;
            }
        }
        return maxId + 1;
    }

    private void broadcastMessage(String message) {
        List<String> blacklistedWorlds = plugin.getConfigManager().getBlacklistedWorlds();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (blacklistedWorlds.contains(player.getWorld().getName())) {
                continue;
            }
            player.sendMessage(message);
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }

    public static class Schedule {
        private final int id;
        private final int kothId;
        private final DayOfWeek day;
        private final LocalTime time;
        private final int captureTime;
        private final int maxTime;

        public Schedule(int id, int kothId, String day, String time, int captureTime, int maxTime) {
            this.id = id;
            this.kothId = kothId;
            this.day = TimeUtils.parseDay(day);
            this.time = TimeUtils.parseScheduleTime(time);
            this.captureTime = captureTime;
            this.maxTime = maxTime;
        }

        public Schedule(int id, int kothId, DayOfWeek day, LocalTime time, int captureTime, int maxTime) {
            this.id = id;
            this.kothId = kothId;
            this.day = day;
            this.time = time;
            this.captureTime = captureTime;
            this.maxTime = maxTime;
        }

        public int getId() { return id; }
        public int getKothId() { return kothId; }
        public DayOfWeek getDay() { return day; }
        public LocalTime getTime() { return time; }
        public int getCaptureTime() { return captureTime; }
        public int getMaxTime() { return maxTime; }
        
        public String getDayString() {
            return day != null ? day.name() : "DAILY";
        }
        
        public String getTimeString() {
            return time.toString();
        }
    }
}