package me.gamma.koth.gui;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LootViewManager {

    private final Set<UUID> lootViewers = new HashSet<>();

    public void addLootViewer(UUID uuid) {
        lootViewers.add(uuid);
    }

    public void removeLootViewer(UUID uuid) {
        lootViewers.remove(uuid);
    }

    public boolean isLootViewer(UUID uuid) {
        return lootViewers.contains(uuid);
    }

    public void clearAll() {
        lootViewers.clear();
    }
}