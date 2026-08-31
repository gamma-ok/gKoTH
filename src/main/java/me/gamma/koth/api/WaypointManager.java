package me.gamma.koth.api;

import com.lunarclient.apollo.event.ApolloListener;
import com.lunarclient.apollo.event.EventBus;
import com.lunarclient.apollo.Apollo;
import com.lunarclient.apollo.common.location.ApolloBlockLocation;
import com.lunarclient.apollo.event.Listen;
import com.lunarclient.apollo.event.player.ApolloRegisterPlayerEvent;
import com.lunarclient.apollo.module.waypoint.Waypoint;
import com.lunarclient.apollo.module.waypoint.WaypointModule;
import com.lunarclient.apollo.module.waypoint.WaypointTextStyle;
import com.lunarclient.apollo.player.ApolloPlayer;
import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.koth.KoTH;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.awt.Color;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class WaypointManager implements ApolloListener {

    private final KoTHPlugin plugin;
    private final WaypointModule waypointModule;
    private boolean enabled;
    
    // Mapa de KoTHs activos con waypoints mostrados
    private final Map<Integer, String> activeWaypoints = new ConcurrentHashMap<>();

    public WaypointManager(KoTHPlugin plugin) {
        this.plugin = plugin;

        boolean apolloFound = Bukkit.getPluginManager().getPlugin("Apollo-Bukkit") != null ||
                              Bukkit.getPluginManager().getPlugin("Apollo") != null;

        this.enabled = apolloFound;

        if (enabled) {
            this.waypointModule = Apollo.getModuleManager().getModule(WaypointModule.class);
            if (waypointModule != null) {
                plugin.getLogger().info("Lunar Client Apollo WaypointModule integration enabled.");
                EventBus.getBus().register(this);
            } else {
                plugin.getLogger().warning("Lunar Client Apollo found but WaypointModule is not available.");
                this.enabled = false;
            }
        } else {
            this.waypointModule = null;
            plugin.getLogger().info("Lunar Client Apollo not found. Waypoints disabled.");
        }
    }
    
    @Listen
    public void onApolloRegister(ApolloRegisterPlayerEvent event) {
        Object rawPlayer = event.getPlayer().getPlayer();
        if (!(rawPlayer instanceof Player)) return;

        Player player = (Player) rawPlayer;
        sendActiveWaypointsToPlayer(player);
    }

    public boolean isEnabled() {
        return enabled;
    }

    private int getGroundY(Location location) {
        Location groundLocation = location.clone();
        
        // Buscar hacia abajo el primer bloque sólido
        for (int y = location.getBlockY(); y > 0; y--) {
            groundLocation.setY(y);
            Material type = groundLocation.getBlock().getType();
            if (type != Material.AIR) {
                return y + 1; // Un bloque arriba del suelo
            }
        }
        
        // Si no encuentra suelo, usar la Y original
        return location.getBlockY();
    }

    // Construye un waypoint con la configuración estándar
    private Waypoint buildWaypoint(String waypointName, Location center, Color color) {
        int groundY = getGroundY(center);
        
        return Waypoint.builder()
                .name(waypointName)
                .location(ApolloBlockLocation.builder()
                        .world(center.getWorld().getName())
                        .x((int) center.getX())
                        .y(groundY)
                        .z((int) center.getZ())
                        .build())
                .color(color)
                .preventRemoval(false)
                .hidden(false)
                .showBeam(false)
                .highlightBlock(false)
                .textStyle(WaypointTextStyle.builder()
                        .showText(true)
                        .labelScale(1.2F)
                        .textShadow(true)
                        .showDistance(true)
                        .build())
                .build();
    }

    // Muestra un waypoint del KoTH a todos los jugadores online con Lunar Client
    public void showKoTHWaypoint(KoTH koth) {
        if (!enabled || waypointModule == null) return;
        if (koth.getCenter() == null) return;
        
        Location center = koth.getCenter();
        String waypointName = "KoTH: " + koth.getName();
        
        // Guardar en el mapa de activos
        activeWaypoints.put(koth.getId(), waypointName);
        
        Waypoint waypoint = buildWaypoint(waypointName, center, new Color(255, 0, 0)); // Rojo
        
        // Enviar a todos los jugadores online
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendWaypointToPlayer(player, waypoint);
        }
        
        plugin.getLogger().info("KoTH waypoint displayed: " + waypointName);
    }

    // Elimina el waypoint del KoTH para todos los jugadores
    public void removeKoTHWaypoint(KoTH koth) {
        if (!enabled || waypointModule == null) return;
        
        String waypointName = activeWaypoints.remove(koth.getId());
        if (waypointName == null) return;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeWaypointFromPlayer(player, waypointName);
        }
        
        plugin.getLogger().info("KoTH waypoint removed: " + waypointName);
    }

    // Elimina todos los waypoints activos
    public void removeAllWaypoints() {
        if (!enabled || waypointModule == null) return;
        
        for (String waypointName : activeWaypoints.values()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                removeWaypointFromPlayer(player, waypointName);
            }
        }
        activeWaypoints.clear();
    }

    // Envía un waypoint a un jugador específico
    private void sendWaypointToPlayer(Player player, Waypoint waypoint) {
        Optional<ApolloPlayer> apolloPlayerOpt = Apollo.getPlayerManager().getPlayer(player.getUniqueId());
        apolloPlayerOpt.ifPresent(apolloPlayer -> {
            waypointModule.displayWaypoint(apolloPlayer, waypoint);
        });
    }

    // Elimina un waypoint de un jugador específico
    private void removeWaypointFromPlayer(Player player, String waypointName) {
        Optional<ApolloPlayer> apolloPlayerOpt = Apollo.getPlayerManager().getPlayer(player.getUniqueId());
        apolloPlayerOpt.ifPresent(apolloPlayer -> {
            waypointModule.removeWaypoint(apolloPlayer, waypointName);
        });
    }

    // Envía un waypoint a un jugador cuando se une al servidor
    public void sendActiveWaypointsToPlayer(Player player) {
        if (!enabled || waypointModule == null) return;
        if (activeWaypoints.isEmpty()) return;
        
        for (Map.Entry<Integer, String> entry : activeWaypoints.entrySet()) {
            KoTH koth = plugin.getKothManager().getKoTH(entry.getKey());
            if (koth != null && koth.getCenter() != null) {
                Location center = koth.getCenter();
                
                Waypoint waypoint = buildWaypoint(entry.getValue(), center, new Color(255, 0, 0)); // Rojo
                
                sendWaypointToPlayer(player, waypoint);
            }
        }
    }
}