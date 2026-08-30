package me.gamma.koth.commands.subcommands;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.koth.KoTH;
import me.gamma.koth.config.MessagesManager;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StopCommand implements SubCommand {

    private final KoTHPlugin plugin;

    public StopCommand(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "stop";
    }

    @Override
    public String getDescription() {
        return "Detiene forzosamente un KoTH activo";
    }

    @Override
    public String getUsage() {
        return "/koth stop <koth|id>";
    }

    @Override
    public String getPermission() {
        return "koth.stop";
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

        if (!koth.isActive()) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("KOTH_NOT_ACTIVE",
                    "%koth%", koth.getName()));
            return;
        }

        if (plugin.getKothManager().stopKoTH(koth)) {
            String message = plugin.getMessagesManager().getMessage("KOTH_ENDED",
                    "%player%", sender.getName(),
                    "%koth%", koth.getName());

            for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
                player.sendMessage(message);
            }
            plugin.getServer().getConsoleSender().sendMessage(message);
        } else {
            sender.sendMessage(MessagesManager.colorize("&cNo se pudo detener el KoTH."));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return plugin.getKothManager().getActiveKoTHs().stream()
                    .map(koth -> koth.getName())
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}