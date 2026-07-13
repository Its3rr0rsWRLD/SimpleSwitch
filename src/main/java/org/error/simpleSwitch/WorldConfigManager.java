package org.error.simpleSwitch;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WorldConfigManager {

    private final SimpleSwitch plugin;
    private final File configFile;
    private YamlConfiguration config;

    public WorldConfigManager(SimpleSwitch plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "worlds.yml");
        loadConfig();
    }

    private void loadConfig() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create worlds.yml");
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save worlds.yml");
            e.printStackTrace();
        }
    }

    public void addOrUpdateWorld(String name, String path) {
        config.set("worlds." + name + ".path", path);
        config.set("worlds." + name + ".last_saved", System.currentTimeMillis());
        saveConfig();
    }

    public List<String> getAvailableWorlds() {
        List<String> worlds = new ArrayList<>();
        if (config.contains("worlds")) {
            Set<String> keys = config.getConfigurationSection("worlds").getKeys(false);
            worlds.addAll(keys);
        }
        return worlds;
    }

    public String getWorldPath(String name) {
        return config.getString("worlds." + name + ".path", name);
    }

    public long getLastSaved(String name) {
        return config.getLong("worlds." + name + ".last_saved", 0L);
    }
}
