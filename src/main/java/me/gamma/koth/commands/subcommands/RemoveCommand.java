package me.gamma.koth.commands.subcommands;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.koth.KoTH;
import me.gamma.koth.config.MessagesManager;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RemoveCommand implements SubCommand {

    private final KoTHPlugin plugin;

    public RemoveCommand(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public String getDescription() {
        return "Elimina un KoTH";
    }

    @Override
    public String getUsage() {
        return "/koth remove <koth|id>";
    }

    @Override
    public String getPermission() {
        return "koth.remove";
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

        if (plugin.getKothManager().removeKoTH(koth.getId())) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("KOTH_REMOVED",
                    "%koth%", koth.getName()));
        } else {
            sender.sendMessage(plugin.getMessagesManager().getMessage("KOTH_REMOVE_FAILED"));
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