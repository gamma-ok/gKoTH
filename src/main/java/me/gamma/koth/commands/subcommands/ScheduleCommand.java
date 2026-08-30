package me.gamma.koth.commands.subcommands;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.config.MessagesManager;
import me.gamma.koth.koth.KoTH;
import me.gamma.koth.koth.ScheduleManager;
import me.gamma.koth.utils.TimeUtils;
import org.bukkit.command.CommandSender;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

public class ScheduleCommand implements SubCommand {

    private final KoTHPlugin plugin;

    public ScheduleCommand(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "schedule";
    }

    @Override
    public String getDescription() {
        return "Administra los horarios de KoTHs";
    }

    @Override
    public String getUsage() {
        return "/koth schedule [list|create|remove]";
    }

    @Override
    public String getPermission() {
        return "koth.schedule";
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission()) ||
               sender.hasPermission("koth.schedule.admin") ||
               sender.hasPermission("koth.admin");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            showPublicSchedule(sender);
            return;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "list":
                showAdminList(sender);
                break;
            case "create":
                createSchedule(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "remove":
                removeSchedule(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            default:
                sender.sendMessage(MessagesManager.colorize("&cUso: " + getUsage()));
                break;
        }
    }

    private void showPublicSchedule(CommandSender sender) {
        Collection<ScheduleManager.Schedule> schedules = plugin.getScheduleManager().getSortedSchedules();

        if (schedules.isEmpty()) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("NO_SCHEDULES"));
            return;
        }

        List<String> lines = plugin.getMessagesManager().getMessageList("koths-schedule.lines-koths");
        String entryFormat = plugin.getMessagesManager().getMessageNoPrefix("koths-schedule.entry-koths");

        // Obtener la zona horaria configurada
        String timeZone = plugin.getConfigManager().getTimeZone();
        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(timeZone);
        } catch (Exception e) {
            zoneId = ZoneId.systemDefault();
        }
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        // Filtrar para obtener solo el schedule más cercano por KoTH
        Map<Integer, ScheduleManager.Schedule> closestSchedules = getClosestSchedules(schedules, now);

        int position = 1;
        List<String> entries = new ArrayList<>();

        // Ordenar los schedules filtrados por tiempo restante
        List<Map.Entry<Integer, ScheduleManager.Schedule>> sortedEntries = new ArrayList<>(closestSchedules.entrySet());
        sortedEntries.sort(Comparator.comparingLong(entry -> 
            calculateTimeLeftSeconds(entry.getValue(), now)));

        for (Map.Entry<Integer, ScheduleManager.Schedule> entry : sortedEntries) {
            ScheduleManager.Schedule schedule = entry.getValue();
            KoTH koth = plugin.getKothManager().getKoTH(schedule.getKothId());
            if (koth == null) continue;

            long secondsLeft = calculateTimeLeftSeconds(schedule, now);
            String timeLeft = TimeUtils.formatTimeLong(secondsLeft);

            String dayStr = schedule.getDayString();
            String timeStr = schedule.getTimeString();

            String entryText = entryFormat
                    .replace("{pos}", String.valueOf(position))
                    .replace("{koth}", koth.getName())
                    .replace("{day}", dayStr)
                    .replace("{time}", timeStr)
                    .replace("{time_left}", timeLeft);
            entries.add(entryText);
            position++;
        }

        for (String line : lines) {
            if (line.contains("{entries}")) {
                for (String entryText : entries) {
                    sender.sendMessage(MessagesManager.colorize(entryText));
                }
            } else {
                sender.sendMessage(MessagesManager.colorize(line));
            }
        }
    }

    // Obtener solo el schedule más cercano por KoTH
    private Map<Integer, ScheduleManager.Schedule> getClosestSchedules(
            Collection<ScheduleManager.Schedule> schedules, ZonedDateTime now) {
        
        Map<Integer, ScheduleManager.Schedule> closest = new HashMap<>();
        Map<Integer, Long> closestTimeLeft = new HashMap<>();
        
        for (ScheduleManager.Schedule schedule : schedules) {
            int kothId = schedule.getKothId();
            long timeLeft = calculateTimeLeftSeconds(schedule, now);
            
            // Si no hay schedule para este KoTH, o este es más cercano
            if (!closest.containsKey(kothId) || timeLeft < closestTimeLeft.get(kothId)) {
                closest.put(kothId, schedule);
                closestTimeLeft.put(kothId, timeLeft);
            }
        }
        
        return closest;
    }

    // Método que devuelve segundos restantes para un schedule
    private long calculateTimeLeftSeconds(ScheduleManager.Schedule schedule, ZonedDateTime now) {
        DayOfWeek day = schedule.getDay();
        LocalTime time = schedule.getTime();

        ZonedDateTime nextRun;

        if (day == null) {
            // DAILY
            nextRun = now.withHour(time.getHour()).withMinute(time.getMinute()).withSecond(0);
            if (nextRun.isBefore(now) || nextRun.isEqual(now)) {
                nextRun = nextRun.plusDays(1);
            }
        } else {
            // Día específico
            int daysUntil = day.getValue() - now.getDayOfWeek().getValue();
            if (daysUntil < 0 || (daysUntil == 0 && now.toLocalTime().isAfter(time))) {
                daysUntil += 7;
            }
            nextRun = now.plusDays(daysUntil)
                    .withHour(time.getHour())
                    .withMinute(time.getMinute())
                    .withSecond(0);
        }

        return java.time.Duration.between(now, nextRun).getSeconds();
    }

    private void showAdminList(CommandSender sender) {
        if (!sender.hasPermission("koth.schedule.admin") && !sender.hasPermission("koth.admin")) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("NO_PERMISSION"));
            return;
        }

        Collection<ScheduleManager.Schedule> schedules = plugin.getScheduleManager().getAllSchedules();

        if (schedules.isEmpty()) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("NO_SCHEDULES"));
            return;
        }

        sender.sendMessage(MessagesManager.colorize("&7&m--------------------"));
        sender.sendMessage(MessagesManager.colorize("&6&lLista de Horarios"));

        for (ScheduleManager.Schedule schedule : schedules) {
            KoTH koth = plugin.getKothManager().getKoTH(schedule.getKothId());
            String kothName = koth != null ? koth.getName() : "Desconocido";

            sender.sendMessage(MessagesManager.colorize(
                "&eID: &f" + schedule.getId() +
                " &eKoTH: &f" + kothName +
                " &eDía: &f" + schedule.getDayString() +
                " &eHora: &f" + schedule.getTimeString() +
                " &eCaptura: &f" + TimeUtils.formatTime(schedule.getCaptureTime()) +
                " &eMax: &f" + TimeUtils.formatTime(schedule.getMaxTime())
            ));
        }

        sender.sendMessage(MessagesManager.colorize("&7&m--------------------"));
    }

    private void createSchedule(CommandSender sender, String[] args) {
        if (!sender.hasPermission("koth.schedule.admin") && !sender.hasPermission("koth.admin")) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("NO_PERMISSION"));
            return;
        }

        if (args.length < 5) {
            sender.sendMessage(MessagesManager.colorize(
                "&cUso: /koth schedule create <koth|id> <day|daily> <HH:MM> <cap> <max>"
            ));
            return;
        }

        KoTH koth = plugin.getKothManager().findKoTH(args[0]);
        if (koth == null) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("KOTH_NOT_FOUND"));
            return;
        }

        String day = args[1].toUpperCase();
        String time = args[2];

        if (!isValidDay(day)) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("SCHEDULE_INVALID_DAY"));
            return;
        }

        if (!time.matches("\\d{2}:\\d{2}")) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("SCHEDULE_INVALID_TIME"));
            return;
        }

        int captureTime, maxTime;
        try {
            captureTime = Integer.parseInt(args[3]);
            maxTime = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("SCHEDULE_INVALID_TIMES"));
            return;
        }

        if (plugin.getScheduleManager().scheduleExists(koth.getId(), day, time)) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("DUPLICATE_SCHEDULE",
                    "%koth%", koth.getName(),
                    "%day%", day,
                    "%time%", time));
            return;
        }

        ScheduleManager.Schedule schedule = plugin.getScheduleManager().createSchedule(
            koth.getId(), day, time, captureTime, maxTime
        );

        if (schedule == null) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("DUPLICATE_SCHEDULE",
                    "%koth%", koth.getName(),
                    "%day%", day,
                    "%time%", time));
            return;
        }

        sender.sendMessage(plugin.getMessagesManager().getMessage("SCHEDULE_CREATED",
                "%id%", String.valueOf(schedule.getId())));

        sender.sendMessage(MessagesManager.colorize(
            "&7KoTH: &f" + koth.getName() +
            " &7Día: &f" + schedule.getDayString() +
            " &7Hora: &f" + schedule.getTimeString() +
            " &7Captura: &f" + TimeUtils.formatTime(captureTime) +
            " &7Max: &f" + TimeUtils.formatTime(maxTime)
        ));
    }

    private void removeSchedule(CommandSender sender, String[] args) {
        if (!sender.hasPermission("koth.schedule.admin") && !sender.hasPermission("koth.admin")) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("NO_PERMISSION"));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(MessagesManager.colorize("&cUso: /koth schedule remove <id>"));
            return;
        }

        int scheduleId;
        try {
            scheduleId = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessagesManager.colorize("&cID de horario inválido."));
            return;
        }

        if (plugin.getScheduleManager().removeSchedule(scheduleId)) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("SCHEDULE_REMOVED",
                    "%id%", String.valueOf(scheduleId)));
        } else {
            sender.sendMessage(plugin.getMessagesManager().getMessage("SCHEDULE_NOT_FOUND"));
        }
    }

    private boolean isValidDay(String day) {
        if (day.equals("DAILY")) return true;
        try {
            DayOfWeek.valueOf(day);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> actions = new ArrayList<>();
            actions.add("list");
            if (sender.hasPermission("koth.schedule.admin") || sender.hasPermission("koth.admin")) {
                actions.add("create");
                actions.add("remove");
            }
            return actions.stream()
                    .filter(action -> action.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            String partial = args[1].toLowerCase();
            return plugin.getKothManager().getAllKoTHs().stream()
                    .map(koth -> koth.getName())
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            List<String> days = Arrays.asList(
                "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY",
                "FRIDAY", "SATURDAY", "SUNDAY", "DAILY"
            );
            return days.stream()
                    .filter(day -> day.startsWith(args[2].toUpperCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}