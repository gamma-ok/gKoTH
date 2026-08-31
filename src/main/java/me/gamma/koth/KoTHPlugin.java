package me.gamma.koth;

import me.gamma.koth.api.ClanHook;
import me.gamma.koth.api.KoTHAPI;
import me.gamma.koth.api.KothExpansion;
import me.gamma.koth.api.WaypointManager;
import me.gamma.koth.commands.KoTHCommand;
import me.gamma.koth.config.ConfigManager;
import me.gamma.koth.config.KoTHDataManager;
import me.gamma.koth.config.MessagesManager;
import me.gamma.koth.config.ScoreboardManager;
import me.gamma.koth.database.DatabaseManager;
import me.gamma.koth.gui.ClaimGUI;
import me.gamma.koth.gui.LootViewManager;
import me.gamma.koth.gui.RewardsGUI;
import me.gamma.koth.koth.CaptureManager;
import me.gamma.koth.koth.KoTHManager;
import me.gamma.koth.koth.ScheduleManager;
import me.gamma.koth.listeners.InventoryListener;
import me.gamma.koth.listeners.PlayerListener;
import me.gamma.koth.listeners.WandListener;
import me.gamma.koth.scoreboard.ScoreboardHandler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class KoTHPlugin extends JavaPlugin {

    private static KoTHPlugin instance;
    
    private ConfigManager configManager;
    private MessagesManager messagesManager;
    private ScoreboardManager scoreboardManager;
    private KoTHDataManager kothDataManager;
    private DatabaseManager databaseManager;
    private KoTHManager kothManager;
    private CaptureManager captureManager;
    private ScheduleManager scheduleManager;
    private ScoreboardHandler scoreboardHandler;
    private ClanHook clanHook;
    private KoTHAPI kothAPI;
    private RewardsGUI rewardsGUI;
    private ClaimGUI claimGUI;
    private LootViewManager lootViewManager;
    private WaypointManager waypointManager;
    
    @Override
    public void onEnable() {
        instance = this;
        long start = System.currentTimeMillis();
        
        getLogger().info("=== KoTH Plugin Starting ===");
        
        // Cargar configuraciones
        if (!loadConfigurations()) {
            getLogger().severe("Error loading configurations!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Inicializar base de datos
        if (!initializeDatabase()) {
            getLogger().severe("Error initializing database!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Inicializar managers
        initializeManagers();
        
        // Registrar listeners
        registerListeners();
        
        // Registrar comandos
        registerCommands();
        
        // Inicializar hook con gClans
        initializeClanHook();
        
        // Inicializar WaypointManager
        waypointManager = new WaypointManager(this);
        
        // Registrar PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new KothExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered: %koth_...%");
        }
        
        // Inicializar API
        kothAPI = new KoTHAPI(this);
        
        // Iniciar tareas programadas
        startScheduledTasks();
        
        long ms = System.currentTimeMillis() - start;
        getLogger().info("=== KoTH Plugin Enabled in " + ms + "ms ===");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("=== KoTH Plugin Disabling ===");
        
        // Eliminar todos los waypoints al desactivar
        if (waypointManager != null) {
            waypointManager.removeAllWaypoints();
        }
        
        // Detener todos los KoTHs activos
        if (captureManager != null) {
            captureManager.stopAllKoTHs();
        }
        
        // Cancelar tareas programadas
        Bukkit.getScheduler().cancelTasks(this);
        
        // Cerrar base de datos
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        
        // Limpiar scoreboards
        if (scoreboardHandler != null) {
            scoreboardHandler.cleanup();
        }
        
        // Limpiar loot viewers
        if (lootViewManager != null) {
            lootViewManager.clearAll();
        }
        
        getLogger().info("=== KoTH Plugin Disabled ===");
    }
    
    private boolean loadConfigurations() {
        try {
            configManager = new ConfigManager(this);
            configManager.loadConfig();
            
            messagesManager = new MessagesManager(this);
            messagesManager.loadMessages();
            
            scoreboardManager = new ScoreboardManager(this);
            scoreboardManager.loadScoreboard();
            
            kothDataManager = new KoTHDataManager(this);
            kothDataManager.loadKoTHs();
            
            return true;
        } catch (Exception e) {
            getLogger().severe("Error loading configurations: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private boolean initializeDatabase() {
        try {
            databaseManager = new DatabaseManager(this);
            databaseManager.initialize();
            return true;
        } catch (Exception e) {
            getLogger().severe("Error initializing database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private void initializeManagers() {
        kothManager = new KoTHManager(this);
        captureManager = new CaptureManager(this);
        scheduleManager = new ScheduleManager(this);
        scoreboardHandler = new ScoreboardHandler(this);
        rewardsGUI = new RewardsGUI(this);
        claimGUI = new ClaimGUI(this);
        lootViewManager = new LootViewManager();
        
        // Cargar KoTHs desde archivo
        kothManager.loadKoTHsFromConfig();
        
        // Cargar horarios
        scheduleManager.loadSchedules();
        
        getLogger().info("Managers initialized successfully.");
    }
    
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new WandListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getLogger().info("Listeners registered successfully.");
    }
    
    private void registerCommands() {
        KoTHCommand kothCommand = new KoTHCommand(this);
        getCommand("koth").setExecutor(kothCommand);
        getCommand("koth").setTabCompleter(kothCommand);
        getLogger().info("Commands registered successfully.");
    }
    
    private void initializeClanHook() {
        clanHook = new ClanHook(this);
        if (clanHook.isEnabled()) {
            getLogger().info("gClans integration enabled.");
        } else {
            getLogger().info("gClans not found. Clan integration disabled.");
        }
    }
    
    private void startScheduledTasks() {
        getServer().getScheduler().runTaskTimer(this, () -> {
            captureManager.checkCaptures();
        }, 5L, 5L);

        getServer().getScheduler().runTaskTimer(this, () -> {
            captureManager.tickTime();
            scheduleManager.tick();
        }, 20L, 20L);

        // Tarea de actualización de scoreboards
        int updateInterval = 2;
        try {
            updateInterval = scoreboardManager.getScoreboardConfig().getInt("update-interval", 2);
            if (updateInterval < 1) updateInterval = 1;
        } catch (Exception ignored) {}

        final int finalInterval = updateInterval;
        getServer().getScheduler().runTaskTimer(this, () -> {
            scoreboardHandler.updateScoreboards();
        }, finalInterval, finalInterval);

        // Tarea de limpieza de recompensas expiradas (cada 5 minutos)
        long expirationMillis = configManager.getRewardExpirationMinutes() * 60 * 1000L;
        getServer().getScheduler().runTaskTimer(this, () -> {
            databaseManager.getUnclaimedRewardsDAO().removeExpiredRewards(expirationMillis);
        }, 6000L, 6000L);

        getLogger().info("Scheduled tasks started.");
    }
    
    public static KoTHPlugin getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public MessagesManager getMessagesManager() {
        return messagesManager;
    }
    
    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
    
    public KoTHDataManager getKothDataManager() {
        return kothDataManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public KoTHManager getKothManager() {
        return kothManager;
    }
    
    public CaptureManager getCaptureManager() {
        return captureManager;
    }
    
    public ScheduleManager getScheduleManager() {
        return scheduleManager;
    }
    
    public ScoreboardHandler getScoreboardHandler() {
        return scoreboardHandler;
    }
    
    public ClanHook getClanHook() {
        return clanHook;
    }
    
    public KoTHAPI getKothAPI() {
        return kothAPI;
    }
    
    public RewardsGUI getRewardsGUI() {
        return rewardsGUI;
    }
    
    public ClaimGUI getClaimGUI() {
        return claimGUI;
    }
    
    public LootViewManager getLootViewManager() {
        return lootViewManager;
    }
    
    public WaypointManager getWaypointManager() {
        return waypointManager;
    }
}