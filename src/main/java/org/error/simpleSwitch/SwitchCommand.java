package org.error.simpleSwitch;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SwitchCommand implements CommandExecutor, TabCompleter {

    private final SimpleSwitch plugin;
    private final WorldConfigManager worldConfigManager;

    public SwitchCommand(SimpleSwitch plugin, WorldConfigManager worldConfigManager) {
        this.plugin = plugin;
        this.worldConfigManager = worldConfigManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("simpleswitch.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /ss <create|save|load|list> [world_name]").color(NamedTextColor.RED));
            return true;
        }

        String action = args[0].toLowerCase();
        
        if (action.equals("save")) {
            if (args.length == 1) {
                handleSaveCurrent(sender);
            } else {
                handleSaveAs(sender, args[1]);
            }
            return true;
        }

        if (action.equals("list")) {
            handleList(sender);
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /ss " + action + " <world_name>").color(NamedTextColor.RED));
            return true;
        }

        String worldName = args[1];

        switch (action) {
            case "create":
                Long seed = null;
                if (args.length > 2) {
                    try {
                        seed = Long.parseLong(args[2]);
                    } catch (NumberFormatException e) {
                        seed = (long) args[2].hashCode();
                    }
                }
                handleCreate(sender, worldName, seed);
                break;
            case "load":
                handleLoad(sender, worldName);
                break;
            default:
                sender.sendMessage(Component.text("Unknown action. Use create, save, load, or list.").color(NamedTextColor.RED));
                break;
        }

        return true;
    }

    private List<String> getAvailableWorldNames() {
        return worldConfigManager.getAvailableWorlds();
    }

    private void handleList(CommandSender sender) {
        sender.sendMessage(Component.text("--- Saved Worlds ---").color(NamedTextColor.AQUA));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        List<String> worldNames = getAvailableWorldNames();
        if (worldNames.isEmpty()) {
            sender.sendMessage(Component.text("No worlds found in worlds.yml.").color(NamedTextColor.RED));
            return;
        }

        for (String name : worldNames) {
            long lastSaved = worldConfigManager.getLastSaved(name);
            String status = "Saved: " + sdf.format(new Date(lastSaved));
            sender.sendMessage(Component.text("- " + name + " (" + status + ")").color(NamedTextColor.YELLOW));
        }
    }

    private void handleCreate(CommandSender sender, String worldName, Long seed) {
        if (Bukkit.getWorld(worldName) != null) {
            sender.sendMessage(Component.text("World '" + worldName + "' already exists and is loaded!").color(NamedTextColor.RED));
            return;
        }

        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (worldFolder.exists()) {
            sender.sendMessage(Component.text("World folder '" + worldName + "' already exists! Use /ss load instead.").color(NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("Creating world '" + worldName + "' and its dimensions...").color(NamedTextColor.YELLOW));

        WorldCreator creator = new WorldCreator(worldName);
        if (seed != null) creator.seed(seed);
        World newWorld = Bukkit.createWorld(creator);
        
        WorldCreator netherCreator = new WorldCreator(worldName + "_nether").environment(World.Environment.NETHER);
        if (seed != null) netherCreator.seed(seed);
        Bukkit.createWorld(netherCreator);
        
        WorldCreator endCreator = new WorldCreator(worldName + "_the_end").environment(World.Environment.THE_END);
        if (seed != null) endCreator.seed(seed);
        Bukkit.createWorld(endCreator);

        if (newWorld != null) {
            worldConfigManager.addOrUpdateWorld(worldName, worldName);
            sender.sendMessage(Component.text("Successfully created and loaded world: " + worldName).color(NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Failed to create world: " + worldName).color(NamedTextColor.DARK_RED));
        }
    }

    private void handleSaveCurrent(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Console must specify a world name to save.").color(NamedTextColor.RED));
            return;
        }
        
        Player player = (Player) sender;
        World world = player.getWorld();
        
        sender.sendMessage(Component.text("Saving current world '" + world.getName() + "'...").color(NamedTextColor.YELLOW));
        world.save();
        worldConfigManager.addOrUpdateWorld(world.getName(), world.getName());
        sender.sendMessage(Component.text("World '" + world.getName() + "' saved successfully.").color(NamedTextColor.GREEN));
    }

    private void handleSaveAs(CommandSender sender, String newName) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Console must specify a loaded world to save (Save-As is for players only).").color(NamedTextColor.RED));
            return;
        }

        Player player = (Player) sender;
        String baseWorldName = PlayerDataManager.getBaseWorldName(player.getWorld().getName());
        
        File sourceFolder = new File(Bukkit.getWorldContainer(), baseWorldName);
        File sourceNether = new File(Bukkit.getWorldContainer(), baseWorldName + "_nether");
        File sourceEnd = new File(Bukkit.getWorldContainer(), baseWorldName + "_the_end");

        File targetFolder = new File(Bukkit.getWorldContainer(), newName);
        File targetNether = new File(Bukkit.getWorldContainer(), newName + "_nether");
        File targetEnd = new File(Bukkit.getWorldContainer(), newName + "_the_end");

        if (targetFolder.exists()) {
            sender.sendMessage(Component.text("A world folder named '" + newName + "' already exists!").color(NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("Saving current world and dimension data...").color(NamedTextColor.YELLOW));
        
        if (Bukkit.getWorld(baseWorldName) != null) Bukkit.getWorld(baseWorldName).save();
        if (Bukkit.getWorld(baseWorldName + "_nether") != null) Bukkit.getWorld(baseWorldName + "_nether").save();
        if (Bukkit.getWorld(baseWorldName + "_the_end") != null) Bukkit.getWorld(baseWorldName + "_the_end").save();
        
        sender.sendMessage(Component.text("Cloning world as '" + newName + "' in the background...").color(NamedTextColor.YELLOW));
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (sourceFolder.exists()) copyWorldFolder(sourceFolder, targetFolder);
                if (sourceNether.exists()) copyWorldFolder(sourceNether, targetNether);
                if (sourceEnd.exists()) copyWorldFolder(sourceEnd, targetEnd);
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    worldConfigManager.addOrUpdateWorld(newName, newName);
                    sender.sendMessage(Component.text("Successfully cloned world as '" + newName + "'!").color(NamedTextColor.GREEN));
                });
            } catch (Exception e) {
                sender.sendMessage(Component.text("Failed to clone world. Check console for errors.").color(NamedTextColor.DARK_RED));
                e.printStackTrace();
            }
        });
    }

    private void copyWorldFolder(File source, File target) throws java.io.IOException {
        if (source.isDirectory()) {
            // Exclude lock file and session to prevent corruption
            if (source.getName().equals("uid.dat") || source.getName().equals("session.lock")) {
                return;
            }
            if (!target.exists()) {
                target.mkdirs();
            }
            String[] children = source.list();
            if (children != null) {
                for (String child : children) {
                    copyWorldFolder(new File(source, child), new File(target, child));
                }
            }
        } else {
            if (source.getName().equals("uid.dat") || source.getName().equals("session.lock")) {
                return;
            }
            java.nio.file.Files.copy(source.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void handleLoad(CommandSender sender, String worldName) {
        String folderPath = worldConfigManager.getWorldPath(worldName);
        
        if (folderPath.endsWith("_nether") || folderPath.endsWith("_the_end")) {
            sender.sendMessage(Component.text("You cannot load a dimension directly! Load the base world instead.").color(NamedTextColor.RED));
            return;
        }

        World currentWorld = Bukkit.getWorld(folderPath);
        
        if (currentWorld == null) {
            File worldFolder = new File(Bukkit.getWorldContainer(), folderPath);
            if (!worldFolder.exists() || !new File(worldFolder, "level.dat").exists()) {
                sender.sendMessage(Component.text("World '" + folderPath + "' does not exist (no level.dat found).").color(NamedTextColor.RED));
                return;
            }

            sender.sendMessage(Component.text("Loading world '" + folderPath + "' and its dimensions...").color(NamedTextColor.YELLOW));

            WorldCreator creator = new WorldCreator(folderPath);
            currentWorld = Bukkit.createWorld(creator);
            
            if (new File(Bukkit.getWorldContainer(), folderPath + "_nether").exists()) {
                Bukkit.createWorld(new WorldCreator(folderPath + "_nether").environment(World.Environment.NETHER));
            }
            if (new File(Bukkit.getWorldContainer(), folderPath + "_the_end").exists()) {
                Bukkit.createWorld(new WorldCreator(folderPath + "_the_end").environment(World.Environment.THE_END));
            }

            if (currentWorld == null) {
                sender.sendMessage(Component.text("Failed to load world '" + folderPath + "'.").color(NamedTextColor.DARK_RED));
                return;
            }
        }

        final World targetWorld = currentWorld;

        sender.sendMessage(Component.text("Switching global server world to '" + worldName + "'...").color(NamedTextColor.GREEN));

        // Update default spawn
        Bukkit.getServer().setSpawnRadius(0);
        targetWorld.setSpawnLocation(targetWorld.getSpawnLocation()); // Just to ensure it's valid
        
        // Save current players data before teleporting!
        for (Player p : Bukkit.getOnlinePlayers()) {
            plugin.getPlayerDataManager().savePlayerData(p, p.getWorld(), p.getLocation());
        }
        
        // Find previous default world if possible to unload it later
        World previousWorld = null;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (previousWorld == null && !PlayerDataManager.getBaseWorldName(p.getWorld().getName()).equals(folderPath)) {
                previousWorld = p.getWorld();
            }
            
            org.bukkit.Location targetLoc = plugin.getPlayerDataManager().getSavedLocation(p, folderPath);
            if (targetLoc == null) {
                targetLoc = targetWorld.getSpawnLocation();
            }
            
            p.teleportAsync(targetLoc).thenAccept(success -> {
                if (success) {
                    p.sendMessage(Component.text("You have been teleported to the new server world!").color(NamedTextColor.AQUA));
                }
            });
        }

        // Wait a few ticks to ensure players are teleported before attempting unload
        final World worldToUnload = previousWorld;
        if (worldToUnload != null && !worldToUnload.equals(targetWorld)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (worldToUnload.getPlayers().isEmpty()) {
                    Bukkit.unloadWorld(worldToUnload, true);
                    sender.sendMessage(Component.text("Unloaded previous world: " + worldToUnload.getName()).color(NamedTextColor.GRAY));
                }
            }, 100L); // 5 seconds
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("simpleswitch.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("create", "save", "load", "list").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String action = args[0].toLowerCase();
            if (action.equals("load") || action.equals("save")) {
                return getAvailableWorldNames().stream()
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (action.equals("create")) {
                return Arrays.asList("<world_name>");
            }
        } else if (args.length == 3) {
            String action = args[0].toLowerCase();
            if (action.equals("create")) {
                return Arrays.asList("[seed]");
            }
        }

        return new ArrayList<>();
    }
}
