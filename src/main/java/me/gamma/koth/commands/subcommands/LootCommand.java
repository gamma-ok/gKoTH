package me.gamma.koth.commands.subcommands;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.koth.KoTH;
import me.gamma.koth.config.MessagesManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LootCommand implements SubCommand {

    private final KoTHPlugin plugin;

    public LootCommand(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "loot";
    }

    @Override
    public String getDescription() {
        return "Muestra las recompensas de un KoTH (solo lectura)";
    }

    @Override
    public String getUsage() {
        return "/koth loot <koth|id>";
    }

    @Override
    public String getPermission() {
        return "koth.loot";
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

        // Crear inventario de solo lectura
        int size = plugin.getConfigManager().getRewardsGuiSize();
        
        // Usar título personalizable de messages.yml
        String title = plugin.getMessagesManager().getMessageNoPrefix("gui-titles.loot",
                "%koth%", koth.getName());
        
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        for (ItemStack reward : koth.getRewards()) {
            if (reward != null) {
                gui.addItem(reward.clone());
            }
        }
        
        // Abrir el inventario en modo solo lectura
        player.openInventory(gui);
        
        // Marcar este inventario como solo lectura
        plugin.getLootViewManager().addLootViewer(player.getUniqueId());
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