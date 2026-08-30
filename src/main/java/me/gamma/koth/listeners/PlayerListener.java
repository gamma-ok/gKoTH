package me.gamma.koth.listeners;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.koth.KoTH;
import me.gamma.koth.utils.TimeUtils;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.List;

public class PlayerListener implements Listener {

    private final KoTHPlugin plugin;

    public PlayerListener(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getKothManager().hasActiveKoTHs()) {
            List<KoTH> activeKoTHs = plugin.getKothManager().getActiveKoTHs();
            
            for (KoTH koth : activeKoTHs) {
                int timeLeft = koth.getMaxTimeLeft();
                
                String x = "N/A", y = "N/A", z = "N/A";
                if (koth.getCenter() != null) {
                    x = String.valueOf((int) koth.getCenter().getX());
                    y = String.valueOf((int) koth.getCenter().getY());
                    z = String.valueOf((int) koth.getCenter().getZ());
                }
                
                List<String> announceMessages = plugin.getMessagesManager().getMessageList(
                    "ANNOUNCE_KOTH_ON_JOIN",
                    "%koth%", koth.getName(),
                    "%time%", TimeUtils.formatTime(timeLeft),
                    "%x%", x,
                    "%y%", y,
                    "%z%", z
                );
                
                for (String message : announceMessages) {
                    player.sendMessage(message);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        plugin.getScoreboardHandler().clearPlayerScoreboard(player);
        
        for (KoTH koth : plugin.getKothManager().getActiveKoTHs()) {
            if (koth.isBeingCaptured() && koth.getCurrentCapturer() != null &&
                koth.getCurrentCapturer().equals(player)) {
                koth.resetCapture();
                
                String message = plugin.getMessagesManager().getMessage("LOST_CONTROL",
                        "%player%", player.getName(),
                        "%koth%", koth.getName());
                broadcastMessage(message);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        
        for (KoTH koth : plugin.getKothManager().getActiveKoTHs()) {
            if (koth.isBeingCaptured() && koth.getCurrentCapturer() != null &&
                koth.getCurrentCapturer().equals(player)) {
                
                if (!koth.isInArea(player.getLocation())) {
                    koth.resetCapture();
                    
                    String message = plugin.getMessagesManager().getMessage("LOST_CONTROL",
                            "%player%", player.getName(),
                            "%koth%", koth.getName());
                    broadcastMessage(message);
                }
            }
        }
    }

    private void broadcastMessage(String message) {
        List<String> blacklistedWorlds = plugin.getConfigManager().getBlacklistedWorlds();
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (blacklistedWorlds.contains(player.getWorld().getName())) {
                continue;
            }
            player.sendMessage(message);
        }
        plugin.getServer().getConsoleSender().sendMessage(message);
    }
}