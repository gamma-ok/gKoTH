package me.gamma.koth.koth;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.api.ClanHook;
import me.gamma.koth.config.MessagesManager;
import me.gamma.koth.database.DAO.ClanStatsDAO;
import me.gamma.koth.database.DAO.PlayerStatsDAO;
import me.gamma.koth.database.DAO.UnclaimedRewardsDAO;
import me.gamma.koth.utils.ItemSerializer;
import me.gamma.koth.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CaptureManager {

    private final KoTHPlugin plugin;
    private final KoTHManager kothManager;
    private final MessagesManager messagesManager;
    private final Map<Integer, Long> announcementCooldowns = new ConcurrentHashMap<>();
    private final Map<Integer, UUID> currentCapturers = new ConcurrentHashMap<>();

    public CaptureManager(KoTHPlugin plugin) {
        this.plugin = plugin;
        this.kothManager = plugin.getKothManager();
        this.messagesManager = plugin.getMessagesManager();
    }

    public void checkCaptures() {
        for (KoTH koth : kothManager.getActiveKoTHs()) {
            if (koth.isActive()) {
                checkPlayersInArea(koth);
            }
        }
    }

    public void tickTime() {
        for (KoTH koth : kothManager.getActiveKoTHs()) {
            if (!koth.isWaypointShown()) {
                koth.setWaypointShown(true);
                plugin.getWaypointManager().showKoTHWaypoint(koth);
            }

            koth.tickTime();

            if (!koth.isActive() && koth.isEndedByMaxTime()) {
                handleMaxTimeReached(koth);
                koth.setEndedByMaxTime(false);
                koth.setWaypointShown(false);
                plugin.getWaypointManager().removeKoTHWaypoint(koth);
                continue;
            }
            
            handleAnnouncements(koth);
        }
    }

    private void checkPlayersInArea(KoTH koth) {
        if (!koth.isActive()) return;
        
        if (koth.isBeingCaptured()) {
            Player capturer = koth.getCurrentCapturer();
            if (capturer == null || !capturer.isOnline()) {
                resetCapture(koth);
                return;
            }
            
            if (!koth.isInArea(capturer.getLocation())) {
                resetCapture(koth);
                return;
            }
            return;
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isDead()) continue;
            if (koth.getWorld() == null || !player.getWorld().equals(koth.getWorld())) continue;
            
            if (koth.isInArea(player.getLocation())) {
                startCapture(koth, player);
                break;
            }
        }
    }

    private void startCapture(KoTH koth, Player player) {
        koth.startCapture(player);
        currentCapturers.put(koth.getId(), player.getUniqueId());
        
        String message = messagesManager.getMessage("ATTEMPTING_CAPTURE",
                "%player%", player.getName(),
                "%koth%", koth.getName(),
                "%time%", TimeUtils.formatTime(koth.getCaptureTimeLeft()));
        broadcastMessage(message);
    }

    private void resetCapture(KoTH koth) {
        String playerName = null;
        
        UUID capturerUUID = currentCapturers.get(koth.getId());
        if (capturerUUID != null) {
            Player capturer = Bukkit.getPlayer(capturerUUID);
            if (capturer != null && capturer.isOnline()) {
                playerName = capturer.getName();
            } else {
                try {
                    org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(capturerUUID);
                    if (offlinePlayer != null) {
                        playerName = offlinePlayer.getName();
                    }
                } catch (Exception ignored) {}
            }
        }
        
        if (playerName == null || playerName.isEmpty()) {
            playerName = koth.getCurrentCapturerName();
        }
        
        final String finalPlayerName = playerName;
        final int timeLeft = koth.getCaptureTimeLeft();
        
        koth.resetCapture();
        currentCapturers.remove(koth.getId());
        announcementCooldowns.remove(koth.getId());
        
        if (finalPlayerName != null && !finalPlayerName.isEmpty()) {
            String message = messagesManager.getMessage("LOST_CONTROL",
                    "%player%", finalPlayerName,
                    "%koth%", koth.getName(),
                    "%time%", TimeUtils.formatTime(timeLeft));
            broadcastMessage(message);
            plugin.getLogger().info("LOST_CONTROL: " + finalPlayerName + " left " + koth.getName() + " with " + timeLeft + "s left");
        }
    }

    private void handleMaxTimeReached(KoTH koth) {
        String message = messagesManager.getMessage("MAX_RUN_TIME",
                "%koth%", koth.getName());
        broadcastMessage(message);
        plugin.getLogger().info("MAX_RUN_TIME: " + koth.getName() + " reached max time");
    }

    private void handleAnnouncements(KoTH koth) {
        if (!koth.isBeingCaptured()) return;
        
        long now = System.currentTimeMillis();
        Long lastAnnouncement = announcementCooldowns.get(koth.getId());
        
        int intervalSeconds = plugin.getConfigManager().getCaptureNotificationInterval();
        long intervalMillis = intervalSeconds * 1000L;
        
        if (lastAnnouncement == null) {
            announcementCooldowns.put(koth.getId(), now);
            return;
        }
        
        if (now - lastAnnouncement >= intervalMillis) {
            announcementCooldowns.put(koth.getId(), now);
            
            Player capturer = koth.getCurrentCapturer();
            if (capturer != null) {
                String message = messagesManager.getMessage("TIME_LEFT",
                        "%player%", capturer.getName(),
                        "%koth%", koth.getName(),
                        "%time%", TimeUtils.formatTime(koth.getCaptureTimeLeft()));
                broadcastMessage(message);
            }
        }
    }

    public void onKoTHCaptured(KoTH koth, Player capturer) {
        koth.setActive(false);
        koth.setBeingCaptured(false);
        
        if (koth.isWaypointShown()) {
            koth.setWaypointShown(false);
            plugin.getWaypointManager().removeKoTHWaypoint(koth);
        }
        
        Map<String, String> clanPlaceholders = new HashMap<>();
        if (plugin.getClanHook().isEnabled()) {
            clanPlaceholders = plugin.getClanHook().getClanPlaceholders(capturer);
        }
        
        String clanName = "";
        String clanDisplay = "";
        String clanTag = "";
        String clanPrefix = "";
        
        if (plugin.getClanHook().isEnabled() && plugin.getClanHook().hasClan(capturer)) {
            clanName = clanPlaceholders.getOrDefault("%gclan_name%", "");
            clanDisplay = clanPlaceholders.getOrDefault("%gclan_display%", clanName);
            clanTag = clanPlaceholders.getOrDefault("%gclan_tag%", clanName);
            clanPrefix = clanPlaceholders.getOrDefault("%gclan_prefix%", "");
        }
        
        long captureDuration = koth.getTotalDuration();
        String captureTimeFormatted = TimeUtils.formatTimeLong(captureDuration);
        String luckPermsPrefix = getLuckPermsPrefix(capturer);
        String resetPrefix = "§r" + luckPermsPrefix;
        
        List<String> captureMessages = messagesManager.getMessageList("CAPTURED_KOTH",
                "%player%", capturer.getName(),
                "%winner%", capturer.getName(),
                "%koth%", koth.getName(),
                "%gclan_name%", clanName,
                "%gclan_display%", clanDisplay,
                "%gclan_tag%", clanTag,
                "%gclan_prefix%", clanPrefix,
                "%capture_time%", captureTimeFormatted,
                "%luckperms_prefix%", resetPrefix);
        for (String message : captureMessages) {
            broadcastMessage(message);
        }
        
        giveRewards(koth, capturer);
        giveClanPoints(capturer);
        executeCommands(koth, capturer);
        updateStats(koth, capturer);
        
        announcementCooldowns.remove(koth.getId());
        currentCapturers.remove(koth.getId());
    }

    private String getLuckPermsPrefix(Player player) {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            return "";
        }
        
        try {
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                String prefix = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, "%luckperms_prefix%");
                String coloredPrefix = MessagesManager.colorize(prefix);
                return coloredPrefix;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting LuckPerms prefix: " + e.getMessage());
        }
        
        return "";
    }

    private void giveRewards(KoTH koth, Player winner) {
        List<ItemStack> rewards = koth.getRewards();
        
        if (rewards.isEmpty()) {
            plugin.getLogger().info("No rewards to give for KoTH: " + koth.getName());
            return;
        }
        
        plugin.getLogger().info("Giving " + rewards.size() + " rewards to " + winner.getName());
        
        String distribution = plugin.getConfigManager().getTargetDistribution();
        
        if (distribution.equalsIgnoreCase("ALL_CLAN_MEMBERS")) {
            giveRewardsToClan(koth, winner, rewards);
        } else {
            giveRewardsToPlayer(koth, winner, rewards);
        }
    }

    private void giveRewardsToPlayer(KoTH koth, Player player, List<ItemStack> rewards) {
        String fullInventoryAction = plugin.getConfigManager().getFullInventoryAction();
        
        for (ItemStack reward : rewards) {
            if (reward == null) continue;
            
            ItemStack rewardClone = reward.clone();
            
            if (player.getInventory().firstEmpty() == -1) {
                if (fullInventoryAction.equalsIgnoreCase("CLAIM_MENU")) {
                    saveUnclaimedReward(player, rewardClone);
                    player.sendMessage(messagesManager.getMessage("INVENTORY_FULL"));
                } else {
                    player.getWorld().dropItemNaturally(player.getLocation(), rewardClone);
                    player.sendMessage(messagesManager.getMessage("INVENTORY_FULL_DROP"));
                }
            } else {
                player.getInventory().addItem(rewardClone);
                plugin.getLogger().info("Gave " + rewardClone.getType() + " x" + rewardClone.getAmount() + " to " + player.getName());
            }
        }
    }

    private void giveRewardsToClan(KoTH koth, Player winner, List<ItemStack> rewards) {
        if (!plugin.getClanHook().isEnabled() || !plugin.getClanHook().hasClan(winner)) {
            giveRewardsToPlayer(koth, winner, rewards);
            return;
        }
        
        ClanHook.ClanInfo clanInfo = plugin.getClanHook().getClanInfo(winner);
        if (clanInfo == null) {
            giveRewardsToPlayer(koth, winner, rewards);
            return;
        }
        
        for (Player member : Bukkit.getOnlinePlayers()) {
            if (plugin.getClanHook().isInClan(member, clanInfo.getClanId())) {
                giveRewardsToPlayer(koth, member, rewards);
            }
        }
    }

    private void saveUnclaimedReward(Player player, ItemStack reward) {
        String base64 = ItemSerializer.itemStackToBase64(reward);
        UnclaimedRewardsDAO dao = plugin.getDatabaseManager().getUnclaimedRewardsDAO();
        dao.addUnclaimedReward(player.getUniqueId(), base64).thenAccept(success -> {
            if (!success) {
                plugin.getLogger().warning("Failed to save unclaimed reward for " + player.getName());
            }
        });
    }

    //  Método principal que decide la distribución de comandos
    private void executeCommands(KoTH koth, Player winner) {
        List<String> commands = koth.getCommands();
        if (commands.isEmpty()) {
            plugin.getLogger().info("No commands to execute for KoTH: " + koth.getName());
            return;
        }
        
        // Obtener la distribución de comandos de la config
        String commandDistribution = plugin.getConfigManager().getCommandDistribution();
        
        if (commandDistribution.equalsIgnoreCase("ALL_CLAN_MEMBERS")) {
            executeCommandsForClan(koth, winner, commands);
        } else {
            executeCommandsForPlayer(koth, winner, commands);
        }
    }

    // Ejecutar comandos solo para el ganador
    private void executeCommandsForPlayer(KoTH koth, Player winner, List<String> commands) {
        String clanName = "";
        if (plugin.getClanHook().isEnabled() && plugin.getClanHook().hasClan(winner)) {
            clanName = plugin.getClanHook().getClanName(winner);
        }
        
        for (String command : commands) {
            String executed = command
                    .replace("%winner%", winner.getName())
                    .replace("%player%", winner.getName())
                    .replace("%gclans_name%", clanName)
                    .replace("%gclan_name%", clanName)
                    .replace("%koth%", koth.getName());
            
            plugin.getLogger().info("Executing command for winner " + winner.getName() + ": " + executed);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), executed);
        }
    }

    // Ejecutar comandos para todos los miembros del clan online
    private void executeCommandsForClan(KoTH koth, Player winner, List<String> commands) {
        if (!plugin.getClanHook().isEnabled() || !plugin.getClanHook().hasClan(winner)) {
            // Si no tiene clan, ejecutar solo para el ganador
            executeCommandsForPlayer(koth, winner, commands);
            return;
        }
        
        ClanHook.ClanInfo clanInfo = plugin.getClanHook().getClanInfo(winner);
        if (clanInfo == null) {
            executeCommandsForPlayer(koth, winner, commands);
            return;
        }
        
        String clanName = clanInfo.getClanName();
        int membersCount = 0;
        
        // Ejecutar comandos para cada miembro del clan online
        for (Player member : Bukkit.getOnlinePlayers()) {
            if (plugin.getClanHook().isInClan(member, clanInfo.getClanId())) {
                for (String command : commands) {
                    String executed = command
                            .replace("%winner%", winner.getName())
                            .replace("%player%", member.getName())
                            .replace("%gclans_name%", clanName)
                            .replace("%gclan_name%", clanName)
                            .replace("%koth%", koth.getName());
                    
                    plugin.getLogger().info("Executing command for clan member " + member.getName() + ": " + executed);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), executed);
                }
                membersCount++;
            }
        }
        
        plugin.getLogger().info("Commands executed for " + membersCount + " online clan members of " + clanName);
    }

    private void giveClanPoints(Player player) {
        if (!plugin.getConfigManager().isClanIntegrationEnabled()) return;
        if (!plugin.getClanHook().isEnabled()) return;
        if (!plugin.getClanHook().hasClan(player)) return;
        
        double points = plugin.getConfigManager().getPointsPerKoth();
        plugin.getClanHook().addPointsToClan(player, points);
    }

    private void updateStats(KoTH koth, Player winner) {
        PlayerStatsDAO playerStatsDAO = plugin.getDatabaseManager().getPlayerStatsDAO();
        playerStatsDAO.incrementCaptures(winner.getUniqueId(), winner.getName());
        
        if (plugin.getClanHook().isEnabled() && plugin.getClanHook().hasClan(winner)) {
            String clanName = plugin.getClanHook().getClanName(winner);
            ClanStatsDAO clanStatsDAO = plugin.getDatabaseManager().getClanStatsDAO();
            clanStatsDAO.incrementCaptures(clanName);
        }
    }

    public void stopAllKoTHs() {
        for (KoTH koth : kothManager.getActiveKoTHs()) {
            kothManager.stopKoTH(koth);
        }
        announcementCooldowns.clear();
        currentCapturers.clear();
    }

    private void broadcastMessage(String message) {
        List<String> blacklistedWorlds = plugin.getConfigManager().getBlacklistedWorlds();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (blacklistedWorlds.contains(player.getWorld().getName())) {
                continue;
            }
            player.sendMessage(message);
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }
}