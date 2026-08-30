package me.gamma.koth.api;

import me.gamma.clans.Clans;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import me.gamma.koth.KoTHPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class ClanHook {

    private final KoTHPlugin plugin;
    private Clans clansPlugin;
    private boolean enabled;

    public ClanHook(KoTHPlugin plugin) {
        this.plugin = plugin;
        this.enabled = false;
        
        Plugin clans = Bukkit.getPluginManager().getPlugin("gClans");
        if (clans instanceof Clans) {
            this.clansPlugin = (Clans) clans;
            this.enabled = true;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Clan getClanOfPlayer(Player player) {
        if (!enabled || player == null) return null;
        try {
            return clansPlugin.getClanManager().getClanOfPlayer(player.getUniqueId());
        } catch (Exception e) {
            return null;
        }
    }

    public Clan getClanOfPlayer(UUID uuid) {
        if (!enabled || uuid == null) return null;
        try {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                return getClanOfPlayer(player);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public String getClanName(Player player) {
        Clan clan = getClanOfPlayer(player);
        return clan != null ? clan.getName() : "N/A";
    }

    public String getClanName(UUID uuid) {
        Clan clan = getClanOfPlayer(uuid);
        return clan != null ? clan.getName() : "N/A";
    }

    public boolean hasClan(Player player) {
        if (!enabled || player == null) return false;
        try {
            ClanPlayer cp = clansPlugin.getClanManager().getPlayer(player.getUniqueId());
            return cp != null && cp.hasClan();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasClan(UUID uuid) {
        if (!enabled || uuid == null) return false;
        try {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                return hasClan(player);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public String getClanId(Player player) {
        Clan clan = getClanOfPlayer(player);
        return clan != null ? clan.getId() : null;
    }

    public boolean isInClan(Player player, String clanId) {
        if (!enabled || player == null || clanId == null) return false;
        Clan clan = getClanOfPlayer(player);
        return clan != null && clan.getId().equals(clanId);
    }

    public void addPointsToClan(Player player, double points) {
        if (!enabled || player == null) return;
        
        Clan clan = getClanOfPlayer(player);
        if (clan == null) return;
        
        try {
            double currentPoints = clan.getTotalPoints();
            clan.setTotalPoints(currentPoints + points);
            clansPlugin.getStorageProvider().updateClan(clan);
            plugin.getLogger().info("Added " + points + " points to clan " + clan.getName());
        } catch (Exception e) {
            plugin.getLogger().warning("Error adding points to clan: " + e.getMessage());
        }
    }

    public ClanInfo getClanInfo(Player player) {
        if (!enabled || player == null) return null;
        
        Clan clan = getClanOfPlayer(player);
        if (clan == null) return null;
        
        return new ClanInfo(clan.getId(), clan.getName(), clan.getTotalPoints());
    }

    public double getClanPoints(Player player) {
        Clan clan = getClanOfPlayer(player);
        return clan != null ? clan.getTotalPoints() : 0.0;
    }

    public int getClanMemberCount(Player player) {
        Clan clan = getClanOfPlayer(player);
        return clan != null ? clan.getMemberCount() : 0;
    }

    public java.util.Map<String, String> getClanPlaceholders(Player player) {
        java.util.Map<String, String> placeholders = new java.util.HashMap<>();
        
        if (!enabled || player == null) {
            placeholders.put("%gclan_name%", "N/A");
            placeholders.put("%gclan_name_raw%", "N/A");
            placeholders.put("%gclan_display%", "N/A");
            placeholders.put("%gclan_tag%", "N/A");
            placeholders.put("%gclan_prefix%", "");
            placeholders.put("%gclan_points%", "0");
            placeholders.put("%gclan_clan_points%", "0");
            placeholders.put("%gclan_clan_kills%", "0");
            placeholders.put("%gclan_clan_deaths%", "0");
            placeholders.put("%gclan_clan_level%", "0");
            placeholders.put("%gclan_clan_members%", "0");
            placeholders.put("%gclan_clan_slots%", "0");
            return placeholders;
        }
        
        ClanPlayer cp = clansPlugin.getClanManager().getPlayer(player.getUniqueId());
        Clan clan = getClanOfPlayer(player);
        
        if (clan == null) {
            placeholders.put("%gclan_name%", "N/A");
            placeholders.put("%gclan_name_raw%", "N/A");
            placeholders.put("%gclan_display%", "N/A");
            placeholders.put("%gclan_tag%", "N/A");
            placeholders.put("%gclan_prefix%", "");
            placeholders.put("%gclan_points%", cp != null ? String.valueOf(cp.getPoints()) : "0");
            placeholders.put("%gclan_clan_points%", "0");
            placeholders.put("%gclan_clan_kills%", "0");
            placeholders.put("%gclan_clan_deaths%", "0");
            placeholders.put("%gclan_clan_level%", "0");
            placeholders.put("%gclan_clan_members%", "0");
            placeholders.put("%gclan_clan_slots%", "0");
            return placeholders;
        }
        
        // Placeholders del clan
        placeholders.put("%gclan_name%", clan.getName());
        placeholders.put("%gclan_name_raw%", clan.getName());
        placeholders.put("%gclan_display%", clan.hasCustomPrefix() ? clan.getColoredPrefix() : clan.getName());
        placeholders.put("%gclan_tag%", clan.hasCustomPrefix() ? clan.getColoredPrefix() + "§f" + clan.getName() : clan.getName());
        placeholders.put("%gclan_prefix%", clan.hasCustomPrefix() ? clan.getColoredPrefix() : "");
        placeholders.put("%gclan_points%", cp != null ? formatPoints(cp.getPoints()) : "0");
        placeholders.put("%gclan_clan_points%", formatPoints(clan.getTotalPoints()));
        placeholders.put("%gclan_clan_kills%", String.valueOf(clan.getTotalKills()));
        placeholders.put("%gclan_clan_deaths%", String.valueOf(clan.getTotalDeaths()));
        placeholders.put("%gclan_clan_level%", String.valueOf(clan.getLevel()));
        placeholders.put("%gclan_clan_members%", String.valueOf(clan.getMemberCount()));
        placeholders.put("%gclan_clan_slots%", String.valueOf(clan.getSlots()));
        placeholders.put("%gclan_kills%", cp != null ? String.valueOf(cp.getKills()) : "0");
        placeholders.put("%gclan_deaths%", cp != null ? String.valueOf(cp.getDeaths()) : "0");
        placeholders.put("%gclan_kd%", cp != null ? String.valueOf(cp.getKDRatio()) : "0.0");
        
        return placeholders;
    }
    
    private String formatPoints(double points) {
        if (points == Math.floor(points) && !Double.isInfinite(points)) {
            return String.valueOf((long) points);
        }
        return String.format("%.1f", points);
    }

    public static class ClanInfo {
        private final String clanId;
        private final String clanName;
        private final double clanPoints;

        public ClanInfo(String clanId, String clanName, double clanPoints) {
            this.clanId = clanId;
            this.clanName = clanName;
            this.clanPoints = clanPoints;
        }

        public String getClanId() { return clanId; }
        public String getClanName() { return clanName; }
        public double getClanPoints() { return clanPoints; }
    }
}