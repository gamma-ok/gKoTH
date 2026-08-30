package me.gamma.koth.api;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.database.DAO.ClanStatsDAO;
import me.gamma.koth.database.DAO.PlayerStatsDAO;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class KothExpansion extends PlaceholderExpansion {

    private final KoTHPlugin plugin;

    public KothExpansion(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "koth";
    }

    @Override
    public String getAuthor() {
        return "FrunaMC";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        // Placeholder: %koth_captures% - capturas del jugador
        if (identifier.equals("captures")) {
            if (player == null) return "0";
            return getPlayerCaptures(player.getUniqueId());
        }

        // Placeholder: %koth_captures_raw% - capturas del jugador sin formato
        if (identifier.equals("captures_raw")) {
            if (player == null) return "0";
            return getPlayerCaptures(player.getUniqueId());
        }

        // Placeholder: %koth_clan_captures% - capturas del clan del jugador
        if (identifier.equals("clan_captures")) {
            if (player == null) return "0";
            return getClanCaptures(player);
        }

        // Placeholder: %koth_top_player_captures_1% hasta %koth_top_player_captures_10%
        if (identifier.startsWith("top_player_captures_")) {
            try {
                int pos = Integer.parseInt(identifier.substring("top_player_captures_".length()));
                return getTopPlayerCaptures(pos);
            } catch (NumberFormatException e) {
                return "0";
            }
        }

        // Placeholder: %koth_top_player_1% hasta %koth_top_player_10%
        if (identifier.startsWith("top_player_")) {
            try {
                int pos = Integer.parseInt(identifier.substring("top_player_".length()));
                return getTopPlayerName(pos);
            } catch (NumberFormatException e) {
                return "N/A";
            }
        }

        // Placeholder: %koth_top_clan_captures_1% hasta %koth_top_clan_captures_10%
        if (identifier.startsWith("top_clan_captures_")) {
            try {
                int pos = Integer.parseInt(identifier.substring("top_clan_captures_".length()));
                return getTopClanCaptures(pos);
            } catch (NumberFormatException e) {
                return "0";
            }
        }

        // Placeholder: %koth_top_clan_1% hasta %koth_top_clan_10%
        if (identifier.startsWith("top_clan_")) {
            try {
                int pos = Integer.parseInt(identifier.substring("top_clan_".length()));
                return getTopClanName(pos);
            } catch (NumberFormatException e) {
                return "N/A";
            }
        }

        // Placeholder: %koth_active% - nombre del KoTH activo
        if (identifier.equals("active")) {
            me.gamma.koth.koth.KoTH active = plugin.getKothManager().getMostRecentActiveKoTH();
            return active != null ? active.getName() : "Ninguno";
        }

        // Placeholder: %koth_active_time% - tiempo restante del KoTH activo
        if (identifier.equals("active_time")) {
            me.gamma.koth.koth.KoTH active = plugin.getKothManager().getMostRecentActiveKoTH();
            return active != null ? me.gamma.koth.utils.TimeUtils.formatTime(active.getMaxTimeLeft()) : "00:00";
        }

        // Placeholder: %koth_capturing% - nombre del jugador que está capturando
        if (identifier.equals("capturing")) {
            me.gamma.koth.koth.KoTH active = plugin.getKothManager().getMostRecentActiveKoTH();
            if (active != null && active.isBeingCaptured()) {
                Player capturer = active.getCurrentCapturer();
                return capturer != null ? capturer.getName() : "N/A";
            }
            return "N/A";
        }

        return null;
    }

    private String getPlayerCaptures(java.util.UUID uuid) {
        try {
            PlayerStatsDAO dao = plugin.getDatabaseManager().getPlayerStatsDAO();
            CompletableFuture<Integer> future = dao.getCaptures(uuid);
            Integer captures = future.get(5, TimeUnit.SECONDS);
            return captures != null ? String.valueOf(captures) : "0";
        } catch (Exception e) {
            return "0";
        }
    }

    private String getClanCaptures(OfflinePlayer player) {
        if (!plugin.getClanHook().isEnabled()) return "0";

        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer == null) return "0";

        String clanName = plugin.getClanHook().getClanName(onlinePlayer);
        if (clanName == null || clanName.equals("N/A") || clanName.isEmpty()) return "0";

        try {
            ClanStatsDAO dao = plugin.getDatabaseManager().getClanStatsDAO();
            CompletableFuture<Integer> future = dao.getCaptures(clanName);
            Integer captures = future.get(5, TimeUnit.SECONDS);
            return captures != null ? String.valueOf(captures) : "0";
        } catch (Exception e) {
            return "0";
        }
    }

    private String getTopPlayerName(int pos) {
        if (pos < 1 || pos > 10) return "N/A";
        try {
            PlayerStatsDAO dao = plugin.getDatabaseManager().getPlayerStatsDAO();
            CompletableFuture<Map<String, Integer>> future = dao.getTopPlayers(10);
            Map<String, Integer> top = future.get(5, TimeUnit.SECONDS);

            if (top == null || top.isEmpty()) return "N/A";

            int index = pos - 1;
            if (index >= top.size()) return "N/A";

            String[] names = top.keySet().toArray(new String[0]);
            return names[index];
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting top player name: " + e.getMessage());
            return "N/A";
        }
    }

    private String getTopPlayerCaptures(int pos) {
        if (pos < 1 || pos > 10) return "0";
        try {
            PlayerStatsDAO dao = plugin.getDatabaseManager().getPlayerStatsDAO();
            CompletableFuture<Map<String, Integer>> future = dao.getTopPlayers(10);
            Map<String, Integer> top = future.get(5, TimeUnit.SECONDS);

            if (top == null || top.isEmpty()) return "0";

            int index = pos - 1;
            if (index >= top.size()) return "0";

            Integer[] values = top.values().toArray(new Integer[0]);
            return String.valueOf(values[index]);
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting top player captures: " + e.getMessage());
            return "0";
        }
    }

    private String getTopClanName(int pos) {
        if (pos < 1 || pos > 10) return "N/A";
        try {
            ClanStatsDAO dao = plugin.getDatabaseManager().getClanStatsDAO();
            CompletableFuture<Map<String, Integer>> future = dao.getTopClans(10);
            Map<String, Integer> top = future.get(5, TimeUnit.SECONDS);

            if (top == null || top.isEmpty()) return "N/A";

            int index = pos - 1;
            if (index >= top.size()) return "N/A";

            String[] names = top.keySet().toArray(new String[0]);
            return names[index];
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting top clan name: " + e.getMessage());
            return "N/A";
        }
    }

    private String getTopClanCaptures(int pos) {
        if (pos < 1 || pos > 10) return "0";
        try {
            ClanStatsDAO dao = plugin.getDatabaseManager().getClanStatsDAO();
            CompletableFuture<Map<String, Integer>> future = dao.getTopClans(10);
            Map<String, Integer> top = future.get(5, TimeUnit.SECONDS);

            if (top == null || top.isEmpty()) return "0";

            int index = pos - 1;
            if (index >= top.size()) return "0";

            Integer[] values = top.values().toArray(new Integer[0]);
            return String.valueOf(values[index]);
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting top clan captures: " + e.getMessage());
            return "0";
        }
    }
}