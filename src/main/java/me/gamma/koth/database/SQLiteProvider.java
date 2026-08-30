package me.gamma.koth.database;

import me.gamma.koth.KoTHPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLiteProvider implements DatabaseProvider {

    private final KoTHPlugin plugin;
    private Connection connection;
    private final Object connectionLock = new Object();  // Para sincronizar acceso

    public SQLiteProvider(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() throws Exception {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        File dbFile = new File(dataFolder, "koth.db");
        
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("SQLite JDBC driver not found!");
            throw e;
        }
        
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            
            if (connection == null) {
                throw new SQLException("Failed to establish SQLite connection");
            }
            
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode=WAL;");
                statement.execute("PRAGMA busy_timeout=5000;");
                statement.execute("PRAGMA synchronous=NORMAL;");
                statement.execute("PRAGMA cache_size=10000;");
                statement.execute("PRAGMA temp_store=MEMORY;");
                statement.execute("PRAGMA foreign_keys=ON;");
            }
            
            plugin.getLogger().info("SQLite database initialized: " + dbFile.getName());
        } catch (SQLException e) {
            plugin.getLogger().severe("Error connecting to SQLite: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void shutdown() {
        synchronized (connectionLock) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                    plugin.getLogger().info("SQLite connection closed.");
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error closing SQLite connection: " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }

    @Override
    public Connection getConnection() {
        synchronized (connectionLock) {
            try {
                if (connection == null || connection.isClosed()) {
                    plugin.getLogger().warning("SQLite connection was closed, reconnecting...");
                    try {
                        initialize();
                    } catch (Exception e) {
                        plugin.getLogger().severe("Failed to reconnect to SQLite: " + e.getMessage());
                        return null;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error checking SQLite connection: " + e.getMessage());
                return null;
            }
            return connection;
        }
    }
}