package me.gamma.koth.koth;

import me.gamma.koth.KoTHPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class KoTHManager {

    private final KoTHPlugin plugin;
    private final Map<Integer, KoTH> koths = new ConcurrentHashMap<>();
    private final Map<String, Integer> kothNameIndex = new ConcurrentHashMap<>();

    public KoTHManager(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadKoTHsFromConfig() {
        koths.clear();
        kothNameIndex.clear();
        
        Map<Integer, KoTH> loadedKoTHs = plugin.getKothDataManager().loadAllKoTHs();
        for (KoTH koth : loadedKoTHs.values()) {
            koths.put(koth.getId(), koth);
            kothNameIndex.put(koth.getName().toLowerCase(), koth.getId());
        }
        
        plugin.getLogger().info("Loaded " + koths.size() + " KoTHs.");
    }

    public KoTH createKoTH(String name) {
        int nextId = getNextAvailableId();
        KoTH koth = new KoTH(nextId, name, plugin);
        koths.put(nextId, koth);
        kothNameIndex.put(name.toLowerCase(), nextId);
        plugin.getKothDataManager().saveKoTH(koth);
        return koth;
    }

    public boolean removeKoTH(int id) {
        KoTH koth = koths.remove(id);
        if (koth != null) {
            kothNameIndex.remove(koth.getName().toLowerCase());
            plugin.getKothDataManager().deleteKoTH(id);
            return true;
        }
        return false;
    }

    public KoTH getKoTH(int id) {
        return koths.get(id);
    }

    public KoTH getKoTHByName(String name) {
        Integer id = kothNameIndex.get(name.toLowerCase());
        return id != null ? koths.get(id) : null;
    }

    public KoTH findKoTH(String input) {
        try {
            int id = Integer.parseInt(input);
            KoTH koth = getKoTH(id);
            if (koth != null) return koth;
        } catch (NumberFormatException ignored) {}
        
        KoTH koth = getKoTHByName(input);
        if (koth != null) return koth;
        
        for (KoTH k : koths.values()) {
            if (k.getName().toLowerCase().contains(input.toLowerCase())) {
                return k;
            }
        }
        
        return null;
    }

    public Collection<KoTH> getAllKoTHs() {
        return Collections.unmodifiableCollection(koths.values());
    }

    public List<KoTH> getEnabledKoTHs() {
        List<KoTH> enabled = new ArrayList<>();
        for (KoTH koth : koths.values()) {
            if (koth.isEnabled()) {
                enabled.add(koth);
            }
        }
        return enabled;
    }

    public boolean hasActiveKoTHs() {
        for (KoTH koth : koths.values()) {
            if (koth.isActive()) {
                return true;
            }
        }
        return false;
    }

    public KoTH getMostRecentActiveKoTH() {
        KoTH mostRecent = null;
        long latestStart = 0;
        
        for (KoTH koth : koths.values()) {
            if (koth.isActive() && koth.getStartTime() > latestStart) {
                mostRecent = koth;
                latestStart = koth.getStartTime();
            }
        }
        
        return mostRecent;
    }

    public List<KoTH> getActiveKoTHs() {
        List<KoTH> active = new ArrayList<>();
        for (KoTH koth : koths.values()) {
            if (koth.isActive()) {
                active.add(koth);
            }
        }
        return active;
    }

    public void stopAllKoTHs() {
        for (KoTH koth : koths.values()) {
            if (koth.isActive()) {
                koth.stop();
            }
        }
    }

    public boolean startKoTH(KoTH koth, int captureTime, int maxTime) {
        if (koth == null) return false;
        if (koth.isActive()) return false;
        if (!koth.isEnabled()) return false;
        if (koth.getPoint1() == null || koth.getPoint2() == null) return false;
        if (captureTime <= 0 || maxTime <= 0) return false;  // Validar tiempos positivos
        
        koth.start(captureTime, maxTime);
        return true;
    }

    public boolean stopKoTH(KoTH koth) {
        if (koth == null || !koth.isActive()) return false;

        if (koth.isWaypointShown()) {
            koth.setWaypointShown(false);
            plugin.getWaypointManager().removeKoTHWaypoint(koth);
        }

        koth.stop();
        return true;
    }

    public void saveKoTH(KoTH koth) {
        plugin.getKothDataManager().saveKoTH(koth);
    }

    public void saveAllKoTHs() {
        for (KoTH koth : koths.values()) {
            plugin.getKothDataManager().saveKoTH(koth);
        }
    }

    private int getNextAvailableId() {
        int maxId = 0;
        for (int id : koths.keySet()) {
            if (id > maxId) {
                maxId = id;
            }
        }
        return maxId + 1;
    }

    public void reloadKoTHs() {
        stopAllKoTHs();
        loadKoTHsFromConfig();
    }
}