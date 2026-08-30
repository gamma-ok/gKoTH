package me.gamma.koth.commands.subcommands;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.koth.KoTH;
import me.gamma.koth.config.MessagesManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CreateCommand implements SubCommand {

    private final KoTHPlugin plugin;

    public CreateCommand(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "Crea un nuevo KoTH";
    }

    @Override
    public String getUsage() {
        return "/koth create <nombre>";
    }

    @Override
    public String getPermission() {
        return "koth.create";
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
        String name = args[0];

        // Verificar si ya existe un KoTH con ese nombre
        if (plugin.getKothManager().getKoTHByName(name) != null) {
            player.sendMessage(plugin.getMessagesManager().getMessageNoPrefix("KOTH_ALREADY_EXISTS"));
            return;
        }

        KoTH koth = plugin.getKothManager().createKoTH(name);

        player.sendMessage(plugin.getMessagesManager().getMessageNoPrefix(
                "KOTH_CREATED",
                "%koth%", koth.getName(),
                "%id%", String.valueOf(koth.getId())
        ));

        player.sendMessage(plugin.getMessagesManager().getMessageNoPrefix(
                "KOTH_CREATED_HINT",
                "%koth%", koth.getName(),
                "%id%", String.valueOf(koth.getId())
        ));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}