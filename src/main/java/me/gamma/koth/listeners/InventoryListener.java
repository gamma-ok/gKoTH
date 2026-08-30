package me.gamma.koth.listeners;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.gui.ClaimGUI;
import me.gamma.koth.gui.LootViewManager;
import me.gamma.koth.gui.RewardsGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class InventoryListener implements Listener {

    private final KoTHPlugin plugin;

    public InventoryListener(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        LootViewManager lootViewManager = plugin.getLootViewManager();
        if (lootViewManager.isLootViewer(player.getUniqueId())) {
            Inventory top = event.getView().getTopInventory();
            Inventory clicked = event.getClickedInventory();

            if ((clicked != null && clicked.equals(top)) || event.isShiftClick()) {
                event.setCancelled(true);
                player.sendMessage(plugin.getMessagesManager().getMessage("LOOT_READ_ONLY"));
            }
            return;
        }

        ClaimGUI claimGUI = plugin.getClaimGUI();
        if (!claimGUI.isClaimGUI(player.getUniqueId())) return;

        Inventory topInventory = event.getView().getTopInventory();
        Inventory clickedInventory = event.getClickedInventory();
        InventoryAction action = event.getAction();

        if (action == InventoryAction.DROP_ALL_CURSOR
                || action == InventoryAction.DROP_ONE_CURSOR
                || action == InventoryAction.DROP_ALL_SLOT
                || action == InventoryAction.DROP_ONE_SLOT) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessagesManager().getMessage("CLAIM_NO_DROP"));
            return;
        }

        if (clickedInventory != null && clickedInventory.equals(topInventory)) {
            boolean isPlainLeftPickup = action == InventoryAction.PICKUP_ALL
                    || action == InventoryAction.NOTHING;

            if (event.isShiftClick() || !isPlainLeftPickup) {
                event.setCancelled(true);
                player.sendMessage(plugin.getMessagesManager().getMessage("CLAIM_NO_MOVE"));
            }
            return;
        }

        if (clickedInventory != null && event.isShiftClick()) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessagesManager().getMessage("CLAIM_NO_MOVE"));
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        LootViewManager lootViewManager = plugin.getLootViewManager();
        if (lootViewManager.isLootViewer(player.getUniqueId())) {
            int topSize = event.getView().getTopInventory().getSize();
            for (int slot : event.getRawSlots()) {
                if (slot < topSize) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getMessagesManager().getMessage("LOOT_READ_ONLY"));
                    return;
                }
            }
            return;
        }

        ClaimGUI claimGUI = plugin.getClaimGUI();
        if (!claimGUI.isClaimGUI(player.getUniqueId())) return;

        int topSize = event.getView().getTopInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize) {
                event.setCancelled(true);
                player.sendMessage(plugin.getMessagesManager().getMessage("CLAIM_NO_MOVE"));
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        LootViewManager lootViewManager = plugin.getLootViewManager();
        if (lootViewManager.isLootViewer(player.getUniqueId())) {
            lootViewManager.removeLootViewer(player.getUniqueId());
            return;
        }

        ClaimGUI claimGUI = plugin.getClaimGUI();
        if (claimGUI.isClaimGUI(player.getUniqueId())) {
            claimGUI.handleClaimClose(player, event.getInventory());
            return;
        }

        RewardsGUI rewardsGUI = plugin.getRewardsGUI();
        if (rewardsGUI.isRewardsGUI(player.getUniqueId())) {
            rewardsGUI.saveRewards(player, event.getInventory());
        }
    }
}