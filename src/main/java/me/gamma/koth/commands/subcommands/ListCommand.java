package me.gamma.koth.commands.subcommands;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.koth.KoTH;
import me.gamma.koth.config.MessagesManager;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ListCommand implements SubCommand {

    private final KoTHPlugin plugin;

    public ListCommand(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "Lista todos los KoTHs";
    }

    @Override
    public String getUsage() {
        return "/koth list";
    }

    @Override
    public String getPermission() {
        return "koth.list";
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission()) || sender.hasPermission("koth.admin");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Collection<KoTH> koths = plugin.getKothManager().getAllKoTHs();
        
        if (koths.isEmpty()) {
            sender.sendMessage(MessagesManager.colorize("&cNo hay KoTHs registrados."));
            return;
        }

        List<String> lines = plugin.getMessagesManager().getMessageList("koths-list.lines-koths");
        String entryFormat = plugin.getMessagesManager().getMessageNoPrefix("koths-list.entry-koths");
        
        int position = 1;
        List<String> entries = new ArrayList<>();
        
        for (KoTH koth : koths) {
            String status;
            if (koth.isActive()) {
                status = "&e[EN CURSO]";
            } else if (koth.isEnabled()) {
                status = "&aActivo";
            } else {
                status = "&cDesactivado";
            }
            
            String entry = entryFormat
                    .replace("{pos}", String.valueOf(position))
                    .replace("{koth}", koth.getName())
                    .replace("{status}", status);
            entries.add(entry);
            position++;
        }

        for (String line : lines) {
            if (line.contains("{entries}")) {
                for (String entry : entries) {
                    sender.sendMessage(MessagesManager.colorize(entry));
                }
            } else {
                sender.sendMessage(line);
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}