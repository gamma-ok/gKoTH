package me.gamma.koth.commands.subcommands;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.koth.KoTH;
import me.gamma.koth.config.MessagesManager;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TpCommand implements SubCommand {

    private final KoTHPlugin plugin;

    public TpCommand(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "tp";
    }

    @Override
    public String getDescription() {
        return "Teletransporta al centro del KoTH";
    }

    @Override
    public String getUsage() {
        return "/koth tp <koth|id>";
    }

    @Override
    public String getPermission() {
        return "koth.tp";
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission()) || sender.hasPermission("koth.admin");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando solo puede ser ejecutado por un jugador.");
            return;
        }
        if (args.length < 1) {
            sender.sendMessage(MessagesManager.colorize("&cUso: " + getUsage()));
            return;
        }

        Player player = (Player) sender;
        KoTH koth = plugin.getKothManager().findKoTH(args[0]);

        if (koth == null) {
            player.sendMessage(plugin.getMessagesManager().getMessage("KOTH_NOT_FOUND"));
            return;
        }

        // Verificar que los puntos estén establecidos
        if (koth.getPoint1() == null || koth.getPoint2() == null) {
            player.sendMessage(plugin.getMessagesManager().getMessage("KOTH_NO_AREA",
                    "%koth%", koth.getName()));
            return;
        }

        // Asegurarse de que el centro esté calculado
        koth.updateCenter();

        Location center = koth.getCenter();
        if (center == null) {
            player.sendMessage(MessagesManager.colorize(
                    "&cNo se pudo calcular el centro del KoTH. Verifica que ambos puntos estén establecidos."));
            return;
        }

        // Ajustar para que el jugador aparezca en el centro exacto
        center.setY(center.getY() + 0.5);

        player.teleport(center);
        player.sendMessage(plugin.getMessagesManager().getMessage("TELEPORTED_TO_KOTH",
                "%koth%", koth.getName()));
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