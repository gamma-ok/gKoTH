package me.gamma.koth.listeners;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.config.MessagesManager;
import me.gamma.koth.koth.KoTH;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WandListener implements Listener {

    private final KoTHPlugin plugin;
    private final Map<UUID, WandSelection> selections = new HashMap<>();

    public WandListener(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWandUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || !isWand(item)) return;
        if (!player.hasPermission("koth.admin")) return;
        
        event.setCancelled(true);
        
        // Obtener el KoTH del wand
        KoTH koth = getKoTHFromWand(item);
        if (koth == null) {
            player.sendMessage(MessagesManager.colorize("&cEste wand no está asociado a ningún KoTH."));
            return;
        }
        
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // Establecer punto 1
            setPoint1(player, koth, event.getClickedBlock().getLocation());
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Establecer punto 2
            setPoint2(player, koth, event.getClickedBlock().getLocation());
        }
    }

    private boolean isWand(ItemStack item) {
        String wandMaterial = plugin.getConfigManager().getWandMaterial();
        String wandNamePrefix = MessagesManager.colorize(plugin.getConfigManager().getWandName());
        
        if (item.getType() != Material.valueOf(wandMaterial)) return false;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
        
        String displayName = item.getItemMeta().getDisplayName();
        return displayName.startsWith(wandNamePrefix);
    }

    private KoTH getKoTHFromWand(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        
        List<String> lore = item.getItemMeta().getLore();
        if (lore == null || lore.isEmpty()) return null;
        
        // Buscar "ID: X" en el lore
        for (String line : lore) {
            String stripped = line.replaceAll("§[0-9a-fA-F]", "");
            if (stripped.contains("ID:")) {
                try {
                    int id = Integer.parseInt(stripped.split(":")[1].trim());
                    return plugin.getKothManager().getKoTH(id);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private void setPoint1(Player player, KoTH koth, Location location) {
        koth.setPoint1(location);
        koth.setWorldName(location.getWorld().getName());
        plugin.getKothManager().saveKoTH(koth);
        
        player.sendMessage(MessagesManager.colorize(
            "&aPunto 1 de &6" + koth.getName() + " &aestablecido en: &e" + 
            location.getBlockX() + ", " + 
            location.getBlockY() + ", " + 
            location.getBlockZ()
        ));
        
        // Verificar si ambos puntos están establecidos
        if (koth.getPoint1() != null && koth.getPoint2() != null) {
            koth.updateCenter();
            player.sendMessage(MessagesManager.colorize(
                "&aCentro del KoTH &6" + koth.getName() + " &acalculado automáticamente."
            ));
        }
    }

    private void setPoint2(Player player, KoTH koth, Location location) {
        koth.setPoint2(location);
        koth.setWorldName(location.getWorld().getName());
        plugin.getKothManager().saveKoTH(koth);
        
        player.sendMessage(MessagesManager.colorize(
            "&aPunto 2 de &6" + koth.getName() + " &aestablecido en: &e" + 
            location.getBlockX() + ", " + 
            location.getBlockY() + ", " + 
            location.getBlockZ()
        ));
        
        // Verificar si ambos puntos están establecidos
        if (koth.getPoint1() != null && koth.getPoint2() != null) {
            koth.updateCenter();
            player.sendMessage(MessagesManager.colorize(
                "&aCentro del KoTH &6" + koth.getName() + " &acalculado automáticamente."
            ));
        }
    }

    public static class WandSelection {
        private Location point1;
        private Location point2;

        public Location getPoint1() { return point1; }
        public void setPoint1(Location point1) { this.point1 = point1; }
        public Location getPoint2() { return point2; }
        public void setPoint2(Location point2) { this.point2 = point2; }
        public boolean isComplete() { return point1 != null && point2 != null; }
    }
}