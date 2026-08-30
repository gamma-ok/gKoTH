package me.gamma.koth.database;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.database.DAO.ClanStatsDAO;
import me.gamma.koth.database.DAO.PlayerStatsDAO;
import me.gamma.koth.database.DAO.UnclaimedRewardsDAO;

import java.sql.Connection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseManager {

    private final KoTHPlugin plugin;
    private DatabaseProvider provider;
    private PlayerStatsDAO playerStatsDAO;
    private ClanStatsDAO clanStatsDAO;
    private UnclaimedRewardsDAO unclaimedRewardsDAO;
    
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    public DatabaseManager(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() throws Exception {
        String dbType = plugin.getConfigManager().getDatabaseType();
        
        if (dbType.equalsIgnoreCase("MYSQL")) {
            provider = new MySQLProvider(plugin);
            plugin.getLogger().info("Using MySQL database.");
        } else {
            provider = new SQLiteProvider(plugin);
            plugin.getLogger().info("Using SQLite database.");
        }
        
        provider.initialize();
        
        Connection conn = provider.getConnection();
        if (conn == null) {
            plugin.getLogger().severe("Database connection is null after initialization!");
            throw new Exception("Database connection is null");
        }
        
        playerStatsDAO = new PlayerStatsDAO(plugin, provider, databaseExecutor);
        clanStatsDAO = new ClanStatsDAO(plugin, provider, databaseExecutor);
        unclaimedRewardsDAO = new UnclaimedRewardsDAO(plugin, provider, databaseExecutor);
        
        plugin.getLogger().info("Creating database tables...");
        
        playerStatsDAO.createTable();
        plugin.getLogger().info("Player stats table created.");
        
        clanStatsDAO.createTable();
        plugin.getLogger().info("Clan stats table created.");
        
        unclaimedRewardsDAO.createTable();
        plugin.getLogger().info("Unclaimed rewards table created.");
    }

    public void shutdown() {
        databaseExecutor.shutdown();
        if (provider != null) {
            provider.shutdown();
        }
    }

    public Connection getConnection() {
        return provider.getConnection();
    }

    public PlayerStatsDAO getPlayerStatsDAO() {
        return playerStatsDAO;
    }

    public ClanStatsDAO getClanStatsDAO() {
        return clanStatsDAO;
    }

    public UnclaimedRewardsDAO getUnclaimedRewardsDAO() {
        return unclaimedRewardsDAO;
    }

    public DatabaseProvider getProvider() {
        return provider;
    }
    
    public ExecutorService getDatabaseExecutor() {
        return databaseExecutor;
    }
}