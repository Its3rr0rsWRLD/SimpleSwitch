package org.error.simpleSwitch;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PlayerDataManager {

    private final SimpleSwitch plugin;
    private final File dataFolder;

    public PlayerDataManager(SimpleSwitch plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!this.dataFolder.exists()) {
            this.dataFolder.mkdirs();
        }
    }

    public static String getBaseWorldName(String worldName) {
        if (worldName.endsWith("_nether")) {
            return worldName.substring(0, worldName.length() - 7);
        } else if (worldName.endsWith("_the_end")) {
            return worldName.substring(0, worldName.length() - 8);
        }
        return worldName;
    }

    public void savePlayerData(Player player, World worldToSave) {
        savePlayerData(player, worldToSave, player.getLocation());
    }

    public void savePlayerData(Player player, World worldToSave, Location exactLocation) {
        String baseWorld = getBaseWorldName(worldToSave.getName());
        File worldFolder = new File(dataFolder, baseWorld);
        if (!worldFolder.exists()) {
            worldFolder.mkdirs();
        }

        File playerFile = new File(worldFolder, player.getUniqueId().toString() + ".yml");
        YamlConfiguration config = playerFile.exists() ? YamlConfiguration.loadConfiguration(playerFile) : new YamlConfiguration();

        config.set("inventory", player.getInventory().getContents());
        config.set("enderchest", player.getEnderChest().getContents());
        config.set("health", player.getHealth());
        config.set("food", player.getFoodLevel());
        config.set("level", player.getLevel());
        config.set("exp", player.getExp());
        config.set("gamemode", player.getGameMode().name());

        if (exactLocation != null) {
            config.set("location.world", exactLocation.getWorld().getName());
            config.set("location.x", exactLocation.getX());
            config.set("location.y", exactLocation.getY());
            config.set("location.z", exactLocation.getZ());
            config.set("location.yaw", exactLocation.getYaw());
            config.set("location.pitch", exactLocation.getPitch());
        }

        config.set("flying.allow_flight", player.getAllowFlight());
        config.set("flying.is_flying", player.isFlying());
        config.set("flying.fly_speed", player.getFlySpeed());
        config.set("flying.walk_speed", player.getWalkSpeed());

        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player data for " + player.getName() + " in world " + baseWorld);
            e.printStackTrace();
        }
    }

    public void loadPlayerData(Player player, World currentWorld) {
        String baseWorld = getBaseWorldName(currentWorld.getName());
        File worldFolder = new File(dataFolder, baseWorld);
        File playerFile = new File(worldFolder, player.getUniqueId().toString() + ".yml");

        player.getInventory().clear();
        player.getEnderChest().clear();
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setLevel(0);
        player.setExp(0f);
        player.setGameMode(Bukkit.getDefaultGameMode());
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setFlySpeed(0.1f);
        player.setWalkSpeed(0.2f);

        if (playerFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);

            if (config.contains("inventory")) {
                List<?> invList = config.getList("inventory");
                if (invList != null) {
                    player.getInventory().setContents(invList.toArray(new ItemStack[0]));
                }
            }

            if (config.contains("enderchest")) {
                List<?> enderList = config.getList("enderchest");
                if (enderList != null) {
                    player.getEnderChest().setContents(enderList.toArray(new ItemStack[0]));
                }
            }

            if (config.contains("health")) {
                player.setHealth(config.getDouble("health"));
            }

            if (config.contains("food")) {
                player.setFoodLevel(config.getInt("food"));
            }

            if (config.contains("level")) {
                player.setLevel(config.getInt("level"));
            }

            if (config.contains("exp")) {
                player.setExp((float) config.getDouble("exp"));
            }

            if (config.contains("gamemode")) {
                try {
                    player.setGameMode(GameMode.valueOf(config.getString("gamemode")));
                } catch (IllegalArgumentException ignored) {}
            }

            if (config.contains("flying.allow_flight")) {
                player.setAllowFlight(config.getBoolean("flying.allow_flight"));
                player.setFlying(config.getBoolean("flying.is_flying"));
                player.setFlySpeed((float) config.getDouble("flying.fly_speed", 0.1));
                player.setWalkSpeed((float) config.getDouble("flying.walk_speed", 0.2));
            }
        }
    }

    public Location getSavedLocation(Player player, String worldName) {
        String baseWorld = getBaseWorldName(worldName);
        File worldFolder = new File(dataFolder, baseWorld);
        File playerFile = new File(worldFolder, player.getUniqueId().toString() + ".yml");
        if (playerFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            if (config.contains("location.world")) {
                World w = Bukkit.getWorld(config.getString("location.world"));
                if (w != null) {
                    double x = config.getDouble("location.x");
                    double y = config.getDouble("location.y");
                    double z = config.getDouble("location.z");
                    float yaw = (float) config.getDouble("location.yaw");
                    float pitch = (float) config.getDouble("location.pitch");
                    return new Location(w, x, y, z, yaw, pitch);
                }
            }
        }
        return null;
    }
}
