package me.gamma.koth.commands.subcommands;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.koth.KoTH;
import me.gamma.koth.config.MessagesManager;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InfoCommand implements SubCommand {

    private final KoTHPlugin plugin;

    public InfoCommand(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "Muestra información de un KoTH";
    }

    @Override
    public String getUsage() {
        return "/koth info <koth|id>";
    }

    @Override
    public String getPermission() {
        return "koth.info";
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission()) || sender.hasPermission("koth.admin");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(MessagesManager.colorize("&cUso: " + getUsage()));
            return;
        }

        KoTH koth = plugin.getKothManager().findKoTH(args[0]);
        if (koth == null) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("KOTH_NOT_FOUND"));
            return;
        }

        // Obtener mensajes personalizables
        List<String> lines = plugin.getMessagesManager().getMessageList("koth-info.lines-info");
        String statusEnabled = plugin.getMessagesManager().getMessageNoPrefix("koth-info.status-enabled");
        String statusDisabled = plugin.getMessagesManager().getMessageNoPrefix("koth-info.status-disabled");
        String activeYes = plugin.getMessagesManager().getMessageNoPrefix("koth-info.active-yes");
        String activeNo = plugin.getMessagesManager().getMessageNoPrefix("koth-info.active-no");
        String coordsNotAvailable = plugin.getMessagesManager().getMessageNoPrefix("koth-info.coords-not-available");
        
        // Construir valores
        String status = koth.isEnabled() ? statusEnabled : statusDisabled;
        String activeStatus = koth.isActive() ? activeYes : activeNo;
        
        // Coordenadas
        String x, y, z;
        if (koth.getCenter() != null) {
            x = String.valueOf((int) koth.getCenter().getX());
            y = String.valueOf((int) koth.getCenter().getY());
            z = String.valueOf((int) koth.getCenter().getZ());
        } else {
            x = coordsNotAvailable;
            y = coordsNotAvailable;
            z = coordsNotAvailable;
        }
        
        // Mostrar líneas con reemplazos
        for (String line : lines) {
            String processedLine = line
                    .replace("{id}", String.valueOf(koth.getId()))
                    .replace("{koth}", koth.getName())
                    .replace("{status}", status)
                    .replace("{active_status}", activeStatus)
                    .replace("{x}", x)
                    .replace("{y}", y)
                    .replace("{z}", z)
                    .replace("{commands_count}", String.valueOf(koth.getCommands().size()))
                    .replace("{rewards_count}", String.valueOf(koth.getRewards().size()));
            
            sender.sendMessage(MessagesManager.colorize(processedLine));
        }
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
        return new ArrayList<>();
    }
}