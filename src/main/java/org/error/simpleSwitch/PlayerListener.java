package org.error.simpleSwitch;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final PlayerDataManager dataManager;

    public PlayerListener(PlayerDataManager dataManager) {
        this.dataManager = dataManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        dataManager.loadPlayerData(event.getPlayer(), event.getPlayer().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        dataManager.savePlayerData(event.getPlayer(), event.getPlayer().getWorld());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        String oldBase = PlayerDataManager.getBaseWorldName(event.getFrom().getName());
        String newBase = PlayerDataManager.getBaseWorldName(event.getPlayer().getWorld().getName());

        // If the player is just switching dimensions inside the same world save, ignore it!
        if (oldBase.equals(newBase)) {
            return;
        }

        // Save the inventory they carried over to the old world
        // We pass null for location because they have already teleported and we don't want to save their NEW location into the OLD world's file.
        dataManager.savePlayerData(event.getPlayer(), event.getFrom(), null);
        // Load the inventory for the new world they just entered
        dataManager.loadPlayerData(event.getPlayer(), event.getPlayer().getWorld());
    }
}
