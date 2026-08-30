package me.gamma.koth.commands.subcommands;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.koth.KoTH;
import me.gamma.koth.config.MessagesManager;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CmdCommand implements SubCommand {

    private final KoTHPlugin plugin;

    public CmdCommand(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "cmd";
    }

    @Override
    public String getDescription() {
        return "Añade un comando ejecutable al ganar";
    }

    @Override
    public String getUsage() {
        return "/koth cmd <koth|id> <comando>";
    }

    @Override
    public String getPermission() {
        return "koth.cmd";
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission()) || sender.hasPermission("koth.admin");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessagesManager.colorize("&cUso: " + getUsage()));
            return;
        }

        KoTH koth = plugin.getKothManager().findKoTH(args[0]);
        if (koth == null) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("KOTH_NOT_FOUND"));
            return;
        }

        // Construir comando desde args[1] en adelante
        StringBuilder commandBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) commandBuilder.append(" ");
            commandBuilder.append(args[i]);
        }
        String command = commandBuilder.toString();

        // Verificar si el comando ya existe
        if (koth.getCommands().contains(command)) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("COMMAND_ALREADY_EXISTS"));
            return;
        }

        // Añadir comando
        koth.addCommand(command);
        plugin.getKothManager().saveKoTH(koth);

        sender.sendMessage(plugin.getMessagesManager().getMessage("COMMAND_ADDED",
                "%koth%", koth.getName()));
        sender.sendMessage(MessagesManager.colorize("&7" + command));
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