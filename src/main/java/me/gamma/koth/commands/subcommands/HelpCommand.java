package me.gamma.koth.commands.subcommands;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.config.MessagesManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class HelpCommand implements SubCommand {

    private final KoTHPlugin plugin;

    public HelpCommand(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Muestra la ayuda del plugin";
    }

    @Override
    public String getUsage() {
        return "/koth help [página]";
    }

    @Override
    public String getPermission() {
        return "koth.player";
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission()) || isAdmin(sender);
    }

    private boolean isAdmin(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return true; // consola ve todo
        }
        return sender.hasPermission("koth.admin") ||
               sender.hasPermission("koth.create") ||
               sender.hasPermission("koth.remove") ||
               sender.hasPermission("koth.enable") ||
               sender.hasPermission("koth.reload") ||
               sender.hasPermission("koth.wand") ||
               sender.hasPermission("koth.rewards") ||
               sender.hasPermission("koth.cmd") ||
               sender.hasPermission("koth.start") ||
               sender.hasPermission("koth.stop") ||
               sender.hasPermission("koth.tp") ||
               sender.hasPermission("koth.info") ||
               sender.hasPermission("koth.schedule.admin");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        boolean admin = isAdmin(sender);

        if (!admin) {
            // Jugadores normales: una sola página con comandos básicos.
            List<String> basicLines = plugin.getMessagesManager().getMessageList("help.basic");
            for (String line : basicLines) {
                sender.sendMessage(line);
            }
            return;
        }

        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage(MessagesManager.colorize("&cNúmero de página inválido."));
                return;
            }
        }

        int maxPages = 3;
        if (page < 1 || page > maxPages) {
            sender.sendMessage(MessagesManager.colorize("&cPágina inválida. Usa /koth help 1-" + maxPages));
            return;
        }

        List<String> helpLines = plugin.getMessagesManager().getMessageList(
            "help.page-" + page,
            "{page}", String.valueOf(page),
            "{max}", String.valueOf(maxPages)
        );

        for (String line : helpLines) {
            sender.sendMessage(line);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && isAdmin(sender)) {
            List<String> pages = new ArrayList<>();
            pages.add("1");
            pages.add("2");
            pages.add("3");
            return pages;
        }
        return new ArrayList<>();
    }
}