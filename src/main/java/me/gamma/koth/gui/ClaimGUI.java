package me.gamma.koth.gui;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.database.DAO.UnclaimedRewardsDAO;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClaimGUI {

    private final KoTHPlugin plugin;
    private final Map<UUID, Inventory> openGUIs = new HashMap<>();

    public ClaimGUI(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    public void openClaimGUI(Player player) {
        UUID uuid = player.getUniqueId();

        // Un jugador no puede tener dos menús de reclamo "en vuelo" a la vez.
        if (openGUIs.containsKey(uuid)) {
            player.sendMessage(plugin.getMessagesManager().getMessage("CLAIM_ALREADY_OPEN"));
            return;
        }

        UnclaimedRewardsDAO dao = plugin.getDatabaseManager().getUnclaimedRewardsDAO();
        List<ItemStack> rewards;

        try {
            rewards = dao.pollUnclaimedRewards(uuid).get();
        } catch (Exception e) {
            plugin.getLogger().severe("Error fetching unclaimed rewards: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(plugin.getMessagesManager().getMessage("CLAIM_ERROR"));
            return;
        }

        if (rewards == null || rewards.isEmpty()) {
            player.sendMessage(plugin.getMessagesManager().getMessage("CLAIM_NO_REWARDS"));
            return;
        }

        try {
            int size = calculateSize(rewards.size(), plugin.getConfigManager().getClaimGuiSize());
            String title = plugin.getMessagesManager().getMessageNoPrefix("gui-titles.claim");

            Inventory gui = Bukkit.createInventory(null, size, title);

            for (ItemStack reward : rewards) {
                if (reward != null) {
                    gui.addItem(reward.clone());
                }
            }

            player.openInventory(gui);
            openGUIs.put(uuid, gui);

        } catch (Exception e) {
            plugin.getLogger().severe("Error opening claim GUI, dropping rewards to avoid loss: " + e.getMessage());
            e.printStackTrace();
            for (ItemStack reward : rewards) {
                if (reward != null) {
                    player.getWorld().dropItemNaturally(player.getLocation(), reward);
                }
            }
            player.sendMessage(plugin.getMessagesManager().getMessage("CLAIM_ERROR"));
        }
    }

    public void handleClaimClose(Player player, Inventory inventory) {
        UUID uuid = player.getUniqueId();

        if (!openGUIs.containsKey(uuid)) {
            return;
        }

        openGUIs.remove(uuid);

        List<ItemStack> remainingItems = new ArrayList<>();
        for (ItemStack item : inventory.getContents()) {
            if (item != null) {
                remainingItems.add(item);
            }
        }

        if (!remainingItems.isEmpty()) {
            for (ItemStack item : remainingItems) {
                if (item != null) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            }
            player.sendMessage(plugin.getMessagesManager().getMessage("CLAIM_ITEMS_DROPPED",
                    "%count%", String.valueOf(remainingItems.size())));
        }

        player.sendMessage(plugin.getMessagesManager().getMessage("CLAIM_COMPLETED"));
    }

    public boolean isClaimGUI(UUID uuid) {
        return openGUIs.containsKey(uuid);
    }

    public void removeOpenGUI(UUID uuid) {
        openGUIs.remove(uuid);
    }

    private int calculateSize(int itemCount, int configuredSize) {
        if (configuredSize >= 9 && configuredSize <= 54 && configuredSize % 9 == 0) {
            if (itemCount <= configuredSize) {
                return configuredSize;
            }
        }
        if (itemCount <= 9) return 9;
        if (itemCount <= 18) return 18;
        if (itemCount <= 27) return 27;
        if (itemCount <= 36) return 36;
        if (itemCount <= 45) return 45;
        return 54;
    }
}