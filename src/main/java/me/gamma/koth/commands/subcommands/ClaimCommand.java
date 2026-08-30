package me.gamma.koth.commands.subcommands;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.config.MessagesManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ClaimCommand implements SubCommand {

    private final KoTHPlugin plugin;

    public ClaimCommand(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "claim";
    }

    @Override
    public String getDescription() {
        return "Abre el menú de recompensas pendientes";
    }

    @Override
    public String getUsage() {
        return "/koth claim";
    }

    @Override
    public String getPermission() {
        return "koth.claim";
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

        Player player = (Player) sender;
        
        if (plugin.getClaimGUI() == null) {
            player.sendMessage(MessagesManager.colorize("&cError: ClaimGUI no está inicializado."));
            return;
        }
        
        plugin.getClaimGUI().openClaimGUI(player);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}