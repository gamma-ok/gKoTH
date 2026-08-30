package me.gamma.koth.database.DAO;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.database.DatabaseProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class PlayerStatsDAO {

    private final KoTHPlugin plugin;
    private final DatabaseProvider provider;
    private final ExecutorService executor;

    public PlayerStatsDAO(KoTHPlugin plugin, DatabaseProvider provider, ExecutorService executor) {
        this.plugin = plugin;
        this.provider = provider;
        this.executor = executor;
    }

    public void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS koth_player_stats (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "player_name VARCHAR(32) NOT NULL," +
                "captures_count INT DEFAULT 0" +
                ")";
        
        Connection conn = provider.getConnection();
        if (conn == null) return;
        
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe("Error creating player stats table: " + e.getMessage());
        }
        // NO cerrar conexión
    }

    public CompletableFuture<Void> incrementCaptures(UUID uuid, String playerName) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO koth_player_stats (uuid, player_name, captures_count) " +
                    "VALUES (?, ?, 1) " +
                    "ON DUPLICATE KEY UPDATE captures_count = captures_count + 1, player_name = VALUES(player_name)";
            
            if (plugin.getConfigManager().getDatabaseType().equalsIgnoreCase("SQLITE")) {
                sql = "INSERT OR REPLACE INTO koth_player_stats (uuid, player_name, captures_count) " +
                        "VALUES (?, ?, COALESCE((SELECT captures_count + 1 FROM koth_player_stats WHERE uuid = ?), 1))";
            }
            
            Connection conn = provider.getConnection();
            if (conn == null) return;
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, playerName);
                if (plugin.getConfigManager().getDatabaseType().equalsIgnoreCase("SQLITE")) {
                    ps.setString(3, uuid.toString());
                }
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error incrementing player captures: " + e.getMessage());
            }
            // NO cerrar conexión
        }, executor);
    }

    public CompletableFuture<Integer> getCaptures(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT captures_count FROM koth_player_stats WHERE uuid = ?";
            
            Connection conn = provider.getConnection();
            if (conn == null) return 0;
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getInt("captures_count");
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error getting player captures: " + e.getMessage());
            }
            return 0;
            // NO cerrar conexión
        }, executor);
    }

    public CompletableFuture<Map<String, Integer>> getTopPlayers(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Integer> top = new LinkedHashMap<>();
            String sql = "SELECT player_name, captures_count FROM koth_player_stats " +
                    "ORDER BY captures_count DESC LIMIT ?";
            
            Connection conn = provider.getConnection();
            if (conn == null) return top;
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, limit);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    top.put(rs.getString("player_name"), rs.getInt("captures_count"));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error getting top players: " + e.getMessage());
            }
            return top;
            // NO cerrar conexión
        }, executor);
    }
}