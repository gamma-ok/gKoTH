package me.gamma.koth.commands.subcommands;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.config.MessagesManager;
import me.gamma.koth.koth.KoTH;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WandCommand implements SubCommand {

    private final KoTHPlugin plugin;

    public WandCommand(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "wand";
    }

    @Override
    public String getDescription() {
        return "Otorga la varita de selección para un KoTH";
    }

    @Override
    public String getUsage() {
        return "/koth wand <koth|id>";
    }

    @Override
    public String getPermission() {
        return "koth.wand";
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

        String wandMaterial = plugin.getConfigManager().getWandMaterial();

        String wandName = MessagesManager.colorize(
                applyWandPlaceholders(plugin.getConfigManager().getWandName(), koth)
        );

        List<String> lore = plugin.getConfigManager().getWandLore().stream()
                .map(line -> MessagesManager.colorize(applyWandPlaceholders(line, koth)))
                .collect(Collectors.toList());

        ItemStack wand = new ItemStack(Material.valueOf(wandMaterial));
        ItemMeta meta = wand.getItemMeta();
        meta.setDisplayName(wandName);
        meta.setLore(lore);
        wand.setItemMeta(meta);

        player.getInventory().addItem(wand);

        player.sendMessage(plugin.getMessagesManager().getMessage("WAND_RECEIVED",
                "%koth%", koth.getName()));
    }

    private String applyWandPlaceholders(String text, KoTH koth) {
        if (text == null) return "";
        return text
                .replace("<koth>", koth.getName())
                .replace("<id>", String.valueOf(koth.getId()));
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