package org.error.simpleSwitch;

import org.bukkit.plugin.java.JavaPlugin;

public final class SimpleSwitch extends JavaPlugin {

    private PlayerDataManager playerDataManager;
    private WorldConfigManager worldConfigManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.worldConfigManager = new WorldConfigManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this.playerDataManager), this);

        SwitchCommand command = new SwitchCommand(this, this.worldConfigManager);
        getCommand("ss").setExecutor(command);
        getCommand("ss").setTabCompleter(command);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (this.playerDataManager != null) {
            for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                this.playerDataManager.savePlayerData(player, player.getWorld());
            }
        }
    }

    public PlayerDataManager getPlayerDataManager() {
        return this.playerDataManager;
    }
}
