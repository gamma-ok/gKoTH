package me.gamma.koth.commands.subcommands;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.config.MessagesManager;
import me.gamma.koth.koth.KoTH;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RemoveCmdCommand implements SubCommand {

    private final KoTHPlugin plugin;

    public RemoveCmdCommand(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "removecmd";
    }

    @Override
    public String getDescription() {
        return "Elimina un comando de un KoTH";
    }

    @Override
    public String getUsage() {
        return "/koth removecmd <koth|id> <índice|comando>";
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

        // Construir el comando a buscar desde args[1] en adelante
        StringBuilder commandBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) commandBuilder.append(" ");
            commandBuilder.append(args[i]);
        }
        String searchCommand = commandBuilder.toString();

        List<String> commands = koth.getCommands();
        
        // Primero intentar eliminar por índice
        try {
            int index = Integer.parseInt(searchCommand);
            if (index >= 1 && index <= commands.size()) {
                String removedCommand = commands.get(index - 1);
                koth.removeCommand(index - 1);
                plugin.getKothManager().saveKoTH(koth);
                
                sender.sendMessage(plugin.getMessagesManager().getMessage("COMMAND_REMOVED",
                        "%koth%", koth.getName(),
                        "%command%", removedCommand));
                return;
            } else {
                sender.sendMessage(MessagesManager.colorize("&cÍndice inválido. Usa un número entre 1 y " + commands.size()));
                return;
            }
        } catch (NumberFormatException ignored) {
            // No es un número, buscar por comando exacto
        }

        // Buscar por comando exacto
        boolean removed = false;
        for (int i = 0; i < commands.size(); i++) {
            if (commands.get(i).equalsIgnoreCase(searchCommand)) {
                koth.removeCommand(i);
                plugin.getKothManager().saveKoTH(koth);
                
                sender.sendMessage(plugin.getMessagesManager().getMessage("COMMAND_REMOVED",
                        "%koth%", koth.getName(),
                        "%command%", searchCommand));
                removed = true;
                break;
            }
        }

        if (!removed) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("COMMAND_NOT_FOUND_IN_KOTH",
                    "%koth%", koth.getName()));
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
        
        if (args.length == 2) {
            KoTH koth = plugin.getKothManager().findKoTH(args[0]);
            if (koth != null) {
                List<String> suggestions = new ArrayList<>();
                List<String> commands = koth.getCommands();
                
                // Agregar índices
                for (int i = 0; i < commands.size(); i++) {
                    suggestions.add(String.valueOf(i + 1));
                }
                
                // Agregar comandos
                suggestions.addAll(commands);
                
                String partial = args[1].toLowerCase();
                return suggestions.stream()
                        .filter(s -> s.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList());
            }
        }
        
        return new ArrayList<>();
    }
}