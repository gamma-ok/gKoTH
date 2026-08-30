package me.gamma.koth.config;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.koth.ScheduleManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KoTHDataManager {

    private final KoTHPlugin plugin;
    private FileConfiguration kothsConfig;
    private File kothsFile;
    
    private FileConfiguration schedulesConfig;
    private File schedulesFile;

    public KoTHDataManager(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadKoTHs() {
        kothsFile = new File(plugin.getDataFolder(), "koths.yml");
        if (!kothsFile.exists()) {
            plugin.saveResource("koths.yml", false);
        }
        kothsConfig = YamlConfiguration.loadConfiguration(kothsFile);
        
        schedulesFile = new File(plugin.getDataFolder(), "schedules.yml");
        if (!schedulesFile.exists()) {
            plugin.saveResource("schedules.yml", false);
        }
        schedulesConfig = YamlConfiguration.loadConfiguration(schedulesFile);
    }

    public void reloadKoTHs() {
        loadKoTHs();
    }

    public Map<Integer, me.gamma.koth.koth.KoTH> loadAllKoTHs() {
        Map<Integer, me.gamma.koth.koth.KoTH> koths = new HashMap<>();
        
        ConfigurationSection kothsSection = kothsConfig.getConfigurationSection("koths");
        if (kothsSection == null) return koths;
        
        for (String key : kothsSection.getKeys(false)) {
            try {
                int id = Integer.parseInt(key);
                ConfigurationSection kothSection = kothsSection.getConfigurationSection(key);
                if (kothSection == null) continue;
                
                me.gamma.koth.koth.KoTH koth = loadKoTHFromSection(id, kothSection);
                if (koth != null) {
                    koths.put(id, koth);
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid KoTH ID: " + key);
            }
        }
        
        return koths;
    }

    private me.gamma.koth.koth.KoTH loadKoTHFromSection(int id, ConfigurationSection section) {
        String name = section.getString("name", "KoTH-" + id);
        me.gamma.koth.koth.KoTH koth = new me.gamma.koth.koth.KoTH(id, name, plugin);  
        
        koth.setEnabled(section.getBoolean("enabled", false));
        koth.setDefaultCaptureTime(section.getInt("default-capture-time", 300));
        koth.setDefaultMaxTime(section.getInt("default-max-time", 1800));
        
        String worldName = section.getString("world");
        koth.setWorldName(worldName);
        
        // Cargar puntos
        Location point1 = loadLocation(section, "point1", worldName);
        Location point2 = loadLocation(section, "point2", worldName);
        koth.setPoint1(point1);
        koth.setPoint2(point2);
        
        // Cargar comandos
        List<String> commands = section.getStringList("commands");
        koth.setCommands(commands);
        
        // Cargar recompensas
        String rewardsBase64 = section.getString("rewards", "");
        if (rewardsBase64 != null && !rewardsBase64.isEmpty()) {
            try {
                koth.setRewardsFromBase64(rewardsBase64);
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading rewards for KoTH " + name + ": " + e.getMessage());
            }
        }
        
        return koth;
    }

    private Location loadLocation(ConfigurationSection section, String path, String worldName) {
        if (worldName == null || worldName.isEmpty()) return null;
        
        if (!section.contains(path + ".x")) return null;
        
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        
        double x = section.getDouble(path + ".x", 0);
        double y = section.getDouble(path + ".y", 0);
        double z = section.getDouble(path + ".z", 0);
        
        return new Location(world, x, y, z);
    }

    public void saveKoTH(me.gamma.koth.koth.KoTH koth) {
        String path = "koths." + koth.getId();
        
        kothsConfig.set(path + ".id", koth.getId());
        kothsConfig.set(path + ".name", koth.getName());
        kothsConfig.set(path + ".enabled", koth.isEnabled());
        kothsConfig.set(path + ".default-capture-time", koth.getDefaultCaptureTime());
        kothsConfig.set(path + ".default-max-time", koth.getDefaultMaxTime());
        kothsConfig.set(path + ".world", koth.getWorldName());
        
        // Guardar puntos
        if (koth.getPoint1() != null) {
            kothsConfig.set(path + ".point1.x", koth.getPoint1().getX());
            kothsConfig.set(path + ".point1.y", koth.getPoint1().getY());
            kothsConfig.set(path + ".point1.z", koth.getPoint1().getZ());
        }
        
        if (koth.getPoint2() != null) {
            kothsConfig.set(path + ".point2.x", koth.getPoint2().getX());
            kothsConfig.set(path + ".point2.y", koth.getPoint2().getY());
            kothsConfig.set(path + ".point2.z", koth.getPoint2().getZ());
        }
        
        // Guardar comandos
        kothsConfig.set(path + ".commands", koth.getCommands());
        
        // Guardar recompensas
        kothsConfig.set(path + ".rewards", koth.getRewardsBase64());
        
        saveKothsFile();
    }

    public void deleteKoTH(int id) {
        kothsConfig.set("koths." + id, null);
        saveKothsFile();
    }

    private void saveKothsFile() {
        try {
            kothsConfig.save(kothsFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving koths.yml: " + e.getMessage());
        }
    }

    public Map<Integer, ScheduleManager.Schedule> loadAllSchedules() {
        Map<Integer, ScheduleManager.Schedule> schedules = new HashMap<>();
        
        ConfigurationSection schedulesSection = schedulesConfig.getConfigurationSection("schedules");
        if (schedulesSection == null) return schedules;
        
        for (String key : schedulesSection.getKeys(false)) {
            try {
                int id = Integer.parseInt(key);
                ConfigurationSection scheduleSection = schedulesSection.getConfigurationSection(key);
                if (scheduleSection == null) continue;
                
                int kothId = scheduleSection.getInt("koth-id");
                String day = scheduleSection.getString("day", "DAILY");
                String time = scheduleSection.getString("time", "00:00");
                int captureTime = scheduleSection.getInt("capture-time", 300);
                int maxTime = scheduleSection.getInt("max-time", 1800);
                
                ScheduleManager.Schedule schedule = new ScheduleManager.Schedule(
                    id, kothId, day, time, captureTime, maxTime
                );
                schedules.put(id, schedule);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid schedule ID: " + key);
            }
        }
        
        return schedules;
    }

    public void saveSchedule(ScheduleManager.Schedule schedule) {
        String path = "schedules." + schedule.getId();
        
        schedulesConfig.set(path + ".id", schedule.getId());
        schedulesConfig.set(path + ".koth-id", schedule.getKothId());
        schedulesConfig.set(path + ".day", schedule.getDayString());
        schedulesConfig.set(path + ".time", schedule.getTimeString());
        schedulesConfig.set(path + ".capture-time", schedule.getCaptureTime());
        schedulesConfig.set(path + ".max-time", schedule.getMaxTime());
        
        saveSchedulesFile();
    }

    public void deleteSchedule(int id) {
        schedulesConfig.set("schedules." + id, null);
        saveSchedulesFile();
    }

    private void saveSchedulesFile() {
        try {
            schedulesConfig.save(schedulesFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving schedules.yml: " + e.getMessage());
        }
    }

    public FileConfiguration getKothsConfig() {
        return kothsConfig;
    }

    public FileConfiguration getSchedulesConfig() {
        return schedulesConfig;
    }
}