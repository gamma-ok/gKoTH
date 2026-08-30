package me.gamma.koth.koth;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.utils.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KoTH {

    private final KoTHPlugin plugin;
    
    private int id;
    private String name;
    private boolean enabled;
    private int captureTime;
    private int maxTime;
    private String worldName;
    private Location point1;
    private Location point2;
    private Location center;
    private List<String> commands;
    private List<ItemStack> rewards;
    
    private boolean active;
    private boolean beingCaptured;
    private UUID currentCapturer;
    private String currentCapturerName;
    private int captureTimeLeft;
    private int maxTimeLeft;
    private long startTime;
    private long lastCaptureTick;
    private long captureDuration;
    private long totalDuration;
    
    private boolean endedByMaxTime;

    public KoTH(int id, String name, KoTHPlugin plugin) {
        this.plugin = plugin;
        this.id = id;
        this.name = name;
        this.enabled = false;
        this.captureTime = 0;
        this.maxTime = 0;
        this.commands = new ArrayList<>();
        this.rewards = new ArrayList<>();
        this.active = false;
        this.beingCaptured = false;
        this.captureTimeLeft = 0;
        this.maxTimeLeft = 0;
        this.endedByMaxTime = false;
        this.totalDuration = 0;
    }

    public void start(int captureTime, int maxTime) {
        this.active = true;
        this.beingCaptured = false;
        this.currentCapturer = null;
        this.currentCapturerName = null;
        this.captureTime = captureTime;
        this.maxTime = maxTime;
        this.captureTimeLeft = captureTime;
        this.maxTimeLeft = maxTime;
        this.startTime = System.currentTimeMillis();
        this.lastCaptureTick = 0;
        this.endedByMaxTime = false;
        this.captureDuration = 0;
        this.totalDuration = 0;
    }

    public void stop() {
        this.active = false;
        this.beingCaptured = false;
        this.currentCapturer = null;
        this.currentCapturerName = null;
        this.captureTimeLeft = 0;
        this.maxTimeLeft = 0;
    }

    public void tickTime() {
        if (!active) return;
        
        // Reducir tiempo máximo
        if (maxTimeLeft > 0) {
            maxTimeLeft--;
            if (maxTimeLeft <= 0) {
                stop();
                this.endedByMaxTime = true;
                return;
            }
        }
        
        // Reducir tiempo de captura (solo si sigue siendo capturado
        if (beingCaptured && currentCapturer != null) {
            if (captureTimeLeft > 0) {
                captureTimeLeft--;
                
                if (captureTimeLeft <= 0) {
                    Player capturer = Bukkit.getPlayer(currentCapturer);
                    if (capturer != null) {
                        onCaptured(capturer);
                    }
                }
            }
        }
    }

    private void onCaptured(Player capturer) {
        if (startTime > 0) {
            totalDuration = (System.currentTimeMillis() - startTime) / 1000;
        }
        
        this.beingCaptured = false;
        this.currentCapturer = null;
        this.currentCapturerName = null;
        this.active = false;
        this.captureTimeLeft = 0;
        
        plugin.getCaptureManager().onKoTHCaptured(this, capturer);
    }

    public void startCapture(Player player) {
        if (!active || beingCaptured) return;
        
        this.beingCaptured = true;
        this.currentCapturer = player.getUniqueId();
        this.currentCapturerName = player.getName();
        this.captureTimeLeft = captureTime;
        this.lastCaptureTick = System.currentTimeMillis();  // Guardar cuando empezó
        this.captureDuration = 0;  // Resetear duración
    }

    public void resetCapture() {
        this.beingCaptured = false;
        this.currentCapturer = null;
        this.currentCapturerName = null;
        this.captureTimeLeft = captureTime;
    }

    public boolean isInArea(Location location) {
        if (point1 == null || point2 == null) return false;
        if (location == null) return false;
        if (location.getWorld() == null || point1.getWorld() == null || point2.getWorld() == null) return false;
        if (!location.getWorld().equals(point1.getWorld())) return false;
        if (!location.getWorld().equals(point2.getWorld())) return false;
        
        double minX = Math.min(point1.getX(), point2.getX());
        double maxX = Math.max(point1.getX(), point2.getX());
        double minY = Math.min(point1.getY(), point2.getY());
        double maxY = Math.max(point1.getY(), point2.getY());
        double minZ = Math.min(point1.getZ(), point2.getZ());
        double maxZ = Math.max(point1.getZ(), point2.getZ());
        
        return location.getX() >= minX && location.getX() <= maxX &&
               location.getY() >= minY && location.getY() <= maxY &&
               location.getZ() >= minZ && location.getZ() <= maxZ;
    }

    public void updateCenter() {
        if (point1 != null && point2 != null && 
            point1.getWorld() != null && point2.getWorld() != null &&
            point1.getWorld().equals(point2.getWorld())) {
            double x = (point1.getX() + point2.getX()) / 2.0;
            double y = (point1.getY() + point2.getY()) / 2.0;
            double z = (point1.getZ() + point2.getZ()) / 2.0;
            center = new Location(point1.getWorld(), x, y, z);
        }
    }

    public String getRewardsBase64() {
        return ItemSerializer.itemListToBase64(rewards);
    }

    public void setRewardsFromBase64(String base64) {
        if (base64 != null && !base64.isEmpty()) {
            this.rewards = ItemSerializer.itemListFromBase64(base64);
        }
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public int getCaptureTime() { return captureTime; }
    public void setCaptureTime(int captureTime) { 
        this.captureTime = captureTime;
    }
    
    public int getMaxTime() { return maxTime; }
    public void setMaxTime(int maxTime) { 
        this.maxTime = maxTime;
    }
    
    public int getDefaultCaptureTime() { return captureTime; }
    public void setDefaultCaptureTime(int defaultCaptureTime) { 
        this.captureTime = defaultCaptureTime;
    }
    public int getDefaultMaxTime() { return maxTime; }
    public void setDefaultMaxTime(int defaultMaxTime) { 
        this.maxTime = defaultMaxTime;
    }
    
    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }
    public Location getPoint1() { return point1; }
    public void setPoint1(Location point1) { 
        this.point1 = point1; 
        updateCenter();
    }
    public Location getPoint2() { return point2; }
    public void setPoint2(Location point2) { 
        this.point2 = point2; 
        updateCenter();
    }
    public Location getCenter() { return center; }
    public List<String> getCommands() { return commands; }
    public void setCommands(List<String> commands) { this.commands = commands; }
    public void addCommand(String command) { this.commands.add(command); }
    public void removeCommand(int index) { 
        if (index >= 0 && index < commands.size()) {
            commands.remove(index);
        }
    }
    public List<ItemStack> getRewards() { return rewards; }
    public void setRewards(List<ItemStack> rewards) { this.rewards = rewards; }
    public void addReward(ItemStack item) { this.rewards.add(item); }
    public void clearRewards() { this.rewards.clear(); }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isBeingCaptured() { return beingCaptured; }
    public void setBeingCaptured(boolean beingCaptured) { this.beingCaptured = beingCaptured; }
    public Player getCurrentCapturer() {  return currentCapturer != null ? Bukkit.getPlayer(currentCapturer) : null; }
    public UUID getCurrentCapturerUUID() { return currentCapturer; }
    public String getCurrentCapturerName() { return currentCapturerName; }
    public int getCaptureTimeLeft() { return captureTimeLeft; }
    public int getMaxTimeLeft() { return maxTimeLeft; }
    public long getStartTime() { return startTime; }
    
    public boolean isEndedByMaxTime() { return endedByMaxTime; }
    public void setEndedByMaxTime(boolean endedByMaxTime) { this.endedByMaxTime = endedByMaxTime; }
    
    public World getWorld() { return worldName != null ? Bukkit.getWorld(worldName) : null; }
    public long getCaptureDuration() { return captureDuration; }
    public long getTotalDuration() { return totalDuration; }

}