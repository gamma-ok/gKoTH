package me.gamma.koth.database.DAO;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.database.DatabaseProvider;
import me.gamma.koth.utils.ItemSerializer;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class UnclaimedRewardsDAO {

    private final KoTHPlugin plugin;
    private final DatabaseProvider provider;
    private final ExecutorService executor;

    public UnclaimedRewardsDAO(KoTHPlugin plugin, DatabaseProvider provider, ExecutorService executor) {
        this.plugin = plugin;
        this.provider = provider;
        this.executor = executor;
    }

    public void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS koth_unclaimed_rewards (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "uuid VARCHAR(36) NOT NULL," +
                "item_base64 TEXT NOT NULL," +
                "created_at BIGINT NOT NULL" +
                ")";

        Connection conn = provider.getConnection();
        if (conn == null) {
            plugin.getLogger().severe("Database connection is null!");
            return;
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe("Error creating unclaimed rewards table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public CompletableFuture<Boolean> addUnclaimedReward(UUID uuid, String itemBase64) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO koth_unclaimed_rewards (uuid, item_base64, created_at) VALUES (?, ?, ?)";

            Connection conn = provider.getConnection();
            if (conn == null) {
                plugin.getLogger().severe("addUnclaimedReward: connection is null for " + uuid);
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, itemBase64);
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
                return true;
            } catch (Exception e) {
                plugin.getLogger().severe("Error adding unclaimed reward: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, executor);
    }

    public CompletableFuture<List<ItemStack>> getUnclaimedRewards(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<ItemStack> rewards = new ArrayList<>();
            String sql = "SELECT id, item_base64 FROM koth_unclaimed_rewards WHERE uuid = ? ORDER BY created_at";

            Connection conn = provider.getConnection();
            if (conn == null) {
                plugin.getLogger().severe("getUnclaimedRewards: connection is null for " + uuid);
                return rewards;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long rowId = rs.getLong("id");
                        String base64 = rs.getString("item_base64");

                        try {
                            ItemStack item = ItemSerializer.itemStackFromBase64(base64);
                            if (item != null) {
                                rewards.add(item);
                            }
                        } catch (Exception itemEx) {
                            plugin.getLogger().warning("Could not deserialize reward id=" + rowId + ": " + itemEx.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error getting unclaimed rewards: " + e.getMessage());
                e.printStackTrace();
            }
            return rewards;
        }, executor);
    }

    /**
     * Lee y BORRA en una sola transacción las recompensas pendientes de un jugador.
     * Se usa al abrir el ClaimGUI: en el momento en que el jugador ve los items,
     * ya no existen en la BD — evita que puedan quedar "duplicados" entre la BD
     * y el inventario del menú si algo sale mal después.
     */
    public CompletableFuture<List<ItemStack>> pollUnclaimedRewards(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<ItemStack> rewards = new ArrayList<>();

            Connection conn = provider.getConnection();
            if (conn == null) {
                plugin.getLogger().severe("pollUnclaimedRewards: connection is null for " + uuid);
                return rewards;
            }

            boolean previousAutoCommit = true;
            try {
                previousAutoCommit = conn.getAutoCommit();
                conn.setAutoCommit(false);

                String selectSql = "SELECT id, item_base64 FROM koth_unclaimed_rewards WHERE uuid = ? ORDER BY created_at";
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            long rowId = rs.getLong("id");
                            String base64 = rs.getString("item_base64");
                            try {
                                ItemStack item = ItemSerializer.itemStackFromBase64(base64);
                                if (item != null) {
                                    rewards.add(item);
                                }
                            } catch (Exception itemEx) {
                                plugin.getLogger().warning("Could not deserialize reward id=" + rowId + ": " + itemEx.getMessage());
                            }
                        }
                    }
                }

                String deleteSql = "DELETE FROM koth_unclaimed_rewards WHERE uuid = ?";
                try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                    ps.setString(1, uuid.toString());
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error polling unclaimed rewards for " + uuid + ": " + e.getMessage());
                e.printStackTrace();
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    plugin.getLogger().warning("Rollback failed: " + rollbackEx.getMessage());
                }
                // Si algo falló, no entregamos items "a medias": mejor que sigan
                // en la BD (rollback) a arriesgar una duplicación.
                rewards.clear();
            } finally {
                try {
                    conn.setAutoCommit(previousAutoCommit);
                } catch (SQLException ex) {
                    plugin.getLogger().warning("No se pudo restaurar autoCommit: " + ex.getMessage());
                }
            }

            return rewards;
        }, executor);
    }

    public CompletableFuture<Void> clearUnclaimedRewards(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM koth_unclaimed_rewards WHERE uuid = ?";

            Connection conn = provider.getConnection();
            if (conn == null) {
                plugin.getLogger().severe("clearUnclaimedRewards: connection is null for " + uuid);
                return;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().severe("Error clearing unclaimed rewards: " + e.getMessage());
                e.printStackTrace();
            }
        }, executor);
    }

    public CompletableFuture<Void> removeExpiredRewards(long expirationMillis) {
        return CompletableFuture.runAsync(() -> {
            long cutoff = System.currentTimeMillis() - expirationMillis;
            String sql = "DELETE FROM koth_unclaimed_rewards WHERE created_at < ?";

            Connection conn = provider.getConnection();
            if (conn == null) {
                plugin.getLogger().severe("removeExpiredRewards: connection is null.");
                return;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, cutoff);
                int removed = ps.executeUpdate();
                if (removed > 0) {
                    plugin.getLogger().info("Removed " + removed + " expired unclaimed rewards.");
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error removing expired rewards: " + e.getMessage());
                e.printStackTrace();
            }
        }, executor);
    }
}