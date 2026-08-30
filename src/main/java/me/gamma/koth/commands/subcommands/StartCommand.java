package me.gamma.koth.commands.subcommands;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.koth.KoTH;
import me.gamma.koth.config.MessagesManager;
import me.gamma.koth.utils.TimeUtils;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StartCommand implements SubCommand {

    private final KoTHPlugin plugin;

    public StartCommand(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "start";
    }

    @Override
    public String getDescription() {
        return "Inicia manualmente un KoTH";
    }

    @Override
    public String getUsage() {
        return "/koth start <koth|id> <sec> <maxTime>";
    }

    @Override
    public String getPermission() {
        return "koth.start";
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission()) || sender.hasPermission("koth.admin");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessagesManager.colorize("&cUso: " + getUsage()));
            sender.sendMessage(MessagesManager.colorize("&cDebes especificar el tiempo de captura y el tiempo máximo."));
            return;
        }

        KoTH koth = plugin.getKothManager().findKoTH(args[0]);
        if (koth == null) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("KOTH_NOT_FOUND"));
            return;
        }

        if (!koth.isEnabled()) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("KOTH_DISABLED_CANNOT_START",
                    "%koth%", koth.getName()));
            return;
        }

        if (koth.isActive()) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("KOTH_ALREADY_ACTIVE",
                    "%koth%", koth.getName()));
            return;
        }

        // Parsear tiempos
        int captureTime;
        int maxTime;
        try {
            captureTime = Integer.parseInt(args[1]);
            maxTime = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessagesManager.colorize("&cTiempos inválidos. Usa números enteros."));
            return;
        }

        // Validar que los tiempos sean positivos
        if (captureTime <= 0 || maxTime <= 0) {
            sender.sendMessage(MessagesManager.colorize("&cLos tiempos deben ser mayores a 0."));
            return;
        }

        // Iniciar KoTH
        if (plugin.getKothManager().startKoTH(koth, captureTime, maxTime)) {
            String starterName = sender instanceof org.bukkit.entity.Player ? 
                sender.getName() : "Console";
            
            String x = "N/A", y = "N/A", z = "N/A";
            if (koth.getCenter() != null) {
                x = String.valueOf((int) koth.getCenter().getX());
                y = String.valueOf((int) koth.getCenter().getY());
                z = String.valueOf((int) koth.getCenter().getZ());
            }
            
            List<String> startMessages = plugin.getMessagesManager().getMessageList("KOTH_STARTING",
                    "%player%", starterName,
                    "%koth%", koth.getName(),
                    "%x%", x,
                    "%y%", y,
                    "%z%", z);
            
            for (String message : startMessages) {
                broadcastMessage(message);
            }
            
            sender.sendMessage(plugin.getMessagesManager().getMessage("KOTH_STARTED",
                    "%koth%", koth.getName(),
                    "%capture%", TimeUtils.formatTime(captureTime),
                    "%max%", TimeUtils.formatTime(maxTime)));
        } else {
            sender.sendMessage(plugin.getMessagesManager().getMessage("KOTH_NO_AREA",
                    "%koth%", koth.getName()));
        }
    }

    private void broadcastMessage(String message) {
        for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendMessage(message);
        }
        plugin.getServer().getConsoleSender().sendMessage(message);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return plugin.getKothManager().getAllKoTHs().stream()
                    .map(koth -> koth.getName())
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return java.util.Arrays.asList("30", "60", "120", "300");
        }
        if (args.length == 3) {
            return java.util.Arrays.asList("300", "600", "1200", "1800");
        }
        return new ArrayList<>();
    }
}