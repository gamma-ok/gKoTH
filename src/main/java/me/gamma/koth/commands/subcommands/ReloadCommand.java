package me.gamma.koth.commands.subcommands;

import me.gamma.koth.KoTHPlugin;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand implements SubCommand {

    private final KoTHPlugin plugin;

    public ReloadCommand(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Recarga configuraciones";
    }

    @Override
    public String getUsage() {
        return "/koth reload";
    }

    @Override
    public String getPermission() {
        return "koth.reload";
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission()) || sender.hasPermission("koth.admin");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        long start = System.currentTimeMillis();

        // Recargar configuraciones
        plugin.getConfigManager().reloadConfig();
        plugin.getMessagesManager().reloadMessages();
        plugin.getScoreboardManager().reloadScoreboard();
        plugin.getKothDataManager().reloadKoTHs();

        // Recargar KoTHs
        plugin.getKothManager().reloadKoTHs();

        // Recargar horarios
        plugin.getScheduleManager().reloadSchedules();

        long ms = System.currentTimeMillis() - start;
        sender.sendMessage(plugin.getMessagesManager().getMessage("RELOAD_COMPLETE",
                "%time%", String.valueOf(ms)));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}