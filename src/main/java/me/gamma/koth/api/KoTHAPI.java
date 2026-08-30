package me.gamma.koth.api;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.koth.KoTH;
import me.gamma.koth.koth.KoTHManager;
import me.gamma.koth.koth.CaptureManager;
import me.gamma.koth.koth.ScheduleManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.Collection;
import java.util.Optional;

public class KoTHAPI {

    private final KoTHPlugin plugin;
    private final KoTHManager kothManager;
    private final CaptureManager captureManager;
    private final ScheduleManager scheduleManager;

    public KoTHAPI(KoTHPlugin plugin) {
        this.plugin = plugin;
        this.kothManager = plugin.getKothManager();
        this.captureManager = plugin.getCaptureManager();
        this.scheduleManager = plugin.getScheduleManager();
    }

    // Obtiene un KoTH por su ID
    public Optional<KoTH> getKoTH(int id) {
        return Optional.ofNullable(kothManager.getKoTH(id));
    }

    // Obtiene un KoTH por su nombre
    public Optional<KoTH> getKoTH(String name) {
        return Optional.ofNullable(kothManager.getKoTHByName(name));
    }

    // Obtiene todos los KoTHs registrados
    public Collection<KoTH> getAllKoTHs() {
        return kothManager.getAllKoTHs();
    }

    // Obtiene todos los KoTHs activos
    public Collection<KoTH> getActiveKoTHs() {
        return kothManager.getActiveKoTHs();
    }

    // Verifica si hay KoTHs activos
    public boolean hasActiveKoTHs() {
        return kothManager.hasActiveKoTHs();
    }

    // Inicia un KoTH manualmente
    public boolean startKoTH(int id, int captureTime, int maxTime) {
        KoTH koth = kothManager.getKoTH(id);
        return koth != null && kothManager.startKoTH(koth, captureTime, maxTime);
    }

    // Inicia un KoTH manualmente
    public boolean startKoTH(String name, int captureTime, int maxTime) {
        KoTH koth = kothManager.getKoTHByName(name);
        return koth != null && kothManager.startKoTH(koth, captureTime, maxTime);
    }

    // Detiene un KoTH activo
    public boolean stopKoTH(int id) {
        KoTH koth = kothManager.getKoTH(id);
        return koth != null && kothManager.stopKoTH(koth);
    }

    // Detiene un KoTH activo
    public boolean stopKoTH(String name) {
        KoTH koth = kothManager.getKoTHByName(name);
        return koth != null && kothManager.stopKoTH(koth);
    }

    // Detiene todos los KoTHs activos
    public void stopAllKoTHs() {
        kothManager.stopAllKoTHs();
    }

    // Verifica si un jugador está capturando un KoTH
    public boolean isPlayerCapturing(Player player) {
        for (KoTH koth : kothManager.getActiveKoTHs()) {
            if (koth.isBeingCaptured() && koth.getCurrentCapturer() != null &&
                koth.getCurrentCapturer().equals(player)) {
                return true;
            }
        }
        return false;
    }

    // Obtiene el KoTH que un jugador está capturando
    public Optional<KoTH> getPlayerCapturingKoTH(Player player) {
        for (KoTH koth : kothManager.getActiveKoTHs()) {
            if (koth.isBeingCaptured() && koth.getCurrentCapturer() != null &&
                koth.getCurrentCapturer().equals(player)) {
                return Optional.of(koth);
            }
        }
        return Optional.empty();
    }

    // Crea un nuevo horario para un KoTH
    public ScheduleManager.Schedule createSchedule(int kothId, String day, String time, 
                                                    int captureTime, int maxTime) {
        return scheduleManager.createSchedule(kothId, day, time, captureTime, maxTime);
    }

    // Elimina un horario
    public boolean removeSchedule(int scheduleId) {
        return scheduleManager.removeSchedule(scheduleId);
    }

    // Obtiene todos los horarios
    public Collection<ScheduleManager.Schedule> getAllSchedules() {
        return scheduleManager.getAllSchedules();
    }

    // Registra un listener para eventos de KoTH
    public void registerKoTHListener(Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }
}