package me.gamma.koth.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class LocationUtils {

    public static String locationToString(Location location) {
        if (location == null) return "";
        return location.getWorld().getName() + "," +
               location.getX() + "," +
               location.getY() + "," +
               location.getZ() + "," +
               location.getYaw() + "," +
               location.getPitch();
    }

    public static Location stringToLocation(String locationString) {
        if (locationString == null || locationString.isEmpty()) return null;
        
        String[] parts = locationString.split(",");
        if (parts.length < 4) return null;
        
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        
        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            
            if (parts.length == 6) {
                float yaw = Float.parseFloat(parts[4]);
                float pitch = Float.parseFloat(parts[5]);
                return new Location(world, x, y, z, yaw, pitch);
            }
            
            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Location centerOf(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null) return null;
        if (!loc1.getWorld().equals(loc2.getWorld())) return null;
        
        double x = (loc1.getX() + loc2.getX()) / 2.0;
        double y = (loc1.getY() + loc2.getY()) / 2.0;
        double z = (loc1.getZ() + loc2.getZ()) / 2.0;
        
        return new Location(loc1.getWorld(), x, y, z);
    }

    public static boolean isInArea(Location location, Location point1, Location point2) {
        if (location == null || point1 == null || point2 == null) return false;
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

    public static void saveLocation(ConfigurationSection section, String path, Location location) {
        if (location == null) return;
        section.set(path + ".x", location.getX());
        section.set(path + ".y", location.getY());
        section.set(path + ".z", location.getZ());
        section.set(path + ".yaw", location.getYaw());
        section.set(path + ".pitch", location.getPitch());
    }

    public static Location loadLocation(ConfigurationSection section, String path, String worldName) {
        if (!section.contains(path + ".x")) return null;
        
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        
        double x = section.getDouble(path + ".x");
        double y = section.getDouble(path + ".y");
        double z = section.getDouble(path + ".z");
        float yaw = (float) section.getDouble(path + ".yaw", 0);
        float pitch = (float) section.getDouble(path + ".pitch", 0);
        
        return new Location(world, x, y, z, yaw, pitch);
    }

    public static String formatLocation(Location location) {
        if (location == null) return "N/A";
        return String.format("%.0f, %.0f, %.0f", location.getX(), location.getY(), location.getZ());
    }

    public static void teleportToCenter(Player player, Location point1, Location point2) {
        Location center = centerOf(point1, point2);
        if (center != null) {
            player.teleport(center);
        }
    }
}