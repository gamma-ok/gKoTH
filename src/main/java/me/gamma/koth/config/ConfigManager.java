package me.gamma.koth.config;

import me.gamma.koth.KoTHPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ConfigManager {

    private final KoTHPlugin plugin;
    private FileConfiguration config;
    private File configFile;
    
    // Settings
    private String timeZone;
    private long checkIntervalTicks;
    private String wandMaterial;
    private String wandName;
    private List<String> wandLore;
    private int rewardsGuiSize;
    private boolean scoreboardEnabled;
    private int captureNotificationInterval;
    private String commandDistribution;
    
    // Rewards
    private String targetDistribution;
    private String fullInventoryAction;
    private long rewardExpirationMinutes;
    
    // Clan Integration
    private boolean clanIntegrationEnabled;
    private double pointsPerKoth;
    private String hookPluginName;
    private int claimGuiSize;
    
    // Database
    private String databaseType;
    private String mysqlHost;
    private int mysqlPort;
    private String mysqlDatabase;
    private String mysqlUsername;
    private String mysqlPassword;
    private int mysqlPoolSize;
    
    // Broadcast
    private List<String> blacklistedWorlds;

    public ConfigManager(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        try (InputStreamReader reader = new InputStreamReader(
                plugin.getResource("config.yml"), StandardCharsets.UTF_8)) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(reader);
            config.setDefaults(defaultConfig);
            config.options().copyDefaults(true);
            config.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not load default config: " + e.getMessage());
        }
        
        loadValues();
    }

    public void reloadConfig() {
        loadConfig();
    }

    private void loadValues() {
        // Settings
        timeZone = config.getString("settings.time-zone", "America/Lima");
        scoreboardEnabled = config.getBoolean("scoreboard.enabled", true);
        checkIntervalTicks = config.getLong("settings.check-interval-ticks", 20L);
        wandMaterial = config.getString("settings.wand-material", "GOLD_AXE");
        wandName = config.getString("settings.wand-name", "&6&lKoTH Selection Wand");
        wandLore = config.getStringList("settings.wand-lore");
        rewardsGuiSize = config.getInt("settings.rewards-gui-size", 27);
        captureNotificationInterval = config.getInt("settings.capture-notification-interval", 15);
        claimGuiSize = config.getInt("settings.claim-gui-size", 27);
        commandDistribution = config.getString("rewards.command-distribution", "WINNER_ONLY");
        
        // Rewards
        targetDistribution = config.getString("rewards.target-distribution", "WINNER_ONLY");
        fullInventoryAction = config.getString("rewards.full-inventory-action", "CLAIM_MENU");
        rewardExpirationMinutes = config.getLong("rewards.expiration-minutes", 30L);
        
        // Clan Integration
        clanIntegrationEnabled = config.getBoolean("clan-integration.enabled", true);
        pointsPerKoth = config.getDouble("clan-integration.points-per-koth", 50.0);
        hookPluginName = config.getString("clan-integration.hook-plugin-name", "gClans");
        
        // Database
        databaseType = config.getString("database.type", "SQLITE").toUpperCase();
        mysqlHost = config.getString("database.mysql.host", "localhost");
        mysqlPort = config.getInt("database.mysql.port", 3306);
        mysqlDatabase = config.getString("database.mysql.database", "koth_db");
        mysqlUsername = config.getString("database.mysql.username", "root");
        mysqlPassword = config.getString("database.mysql.password", "");
        mysqlPoolSize = config.getInt("database.mysql.pool-size", 10);
        
        // Broadcast
        blacklistedWorlds = config.getStringList("settings.blacklisted-worlds");
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Could not save config: " + e.getMessage());
        }
    }

    // Getters
    public String getTimeZone() { return timeZone; }
    public long getCheckIntervalTicks() { return checkIntervalTicks; }
    public String getWandMaterial() { return wandMaterial; }
    public String getWandName() { return wandName; }
    public List<String> getWandLore() { return wandLore; }
    public int getRewardsGuiSize() { return rewardsGuiSize; }
    public boolean isScoreboardEnabled() { return scoreboardEnabled; }
    public String getTargetDistribution() { return targetDistribution; }
    public String getFullInventoryAction() { return fullInventoryAction; }
    public long getRewardExpirationMinutes() { return rewardExpirationMinutes; }
    public boolean isClanIntegrationEnabled() { return clanIntegrationEnabled; }
    public double getPointsPerKoth() { return pointsPerKoth; }
    public String getHookPluginName() { return hookPluginName; }
    public String getDatabaseType() { return databaseType; }
    public String getMysqlHost() { return mysqlHost; }
    public int getMysqlPort() { return mysqlPort; }
    public String getMysqlDatabase() { return mysqlDatabase; }
    public String getMysqlUsername() { return mysqlUsername; }
    public String getMysqlPassword() { return mysqlPassword; }
    public int getMysqlPoolSize() { return mysqlPoolSize; }
    public List<String> getBlacklistedWorlds() { return blacklistedWorlds; }
    public FileConfiguration getConfig() { return config; }
    public int getCaptureNotificationInterval() { return captureNotificationInterval; }
    public int getClaimGuiSize() { return claimGuiSize; }
    public String getCommandDistribution() { return commandDistribution; }
}