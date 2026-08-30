package me.gamma.koth.gui;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.koth.KoTH;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RewardsGUI {

    private final KoTHPlugin plugin;
    private final Map<UUID, Integer> openGUIs = new HashMap<>();

    public RewardsGUI(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    public void openRewardsGUI(Player player, KoTH koth) {
        int size = plugin.getConfigManager().getRewardsGuiSize();
        
        // Ttítulo personalizable en messages.yml
        String title = plugin.getMessagesManager().getMessageNoPrefix("gui-titles.rewards",
                "%koth%", koth.getName());
        
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        // Cargar recompensas existentes
        for (ItemStack reward : koth.getRewards()) {
            if (reward != null) {
                gui.addItem(reward.clone());
            }
        }
        
        player.openInventory(gui);
        openGUIs.put(player.getUniqueId(), koth.getId());
    }

    public void saveRewards(Player player, Inventory inventory) {
        Integer kothId = openGUIs.get(player.getUniqueId());
        if (kothId == null) return;
        
        KoTH koth = plugin.getKothManager().getKoTH(kothId);
        if (koth == null) return;
        
        // Limpiar recompensas existentes
        koth.clearRewards();
        
        // Agregar items del inventario
        for (ItemStack item : inventory.getContents()) {
            if (item != null) {
                koth.addReward(item.clone());
            }
        }
        
        // Guardar en archivo
        plugin.getKothManager().saveKoTH(koth);
        
        // Mensaje de confirmación
        player.sendMessage(plugin.getMessagesManager().getMessage("REWARDS_SAVED",
                "%koth%", koth.getName()));
        
        openGUIs.remove(player.getUniqueId());
    }

    public boolean isRewardsGUI(UUID uuid) {
        return openGUIs.containsKey(uuid);
    }

    public void removeOpenGUI(UUID uuid) {
        openGUIs.remove(uuid);
    }
}