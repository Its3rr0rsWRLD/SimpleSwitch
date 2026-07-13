# SimpleSwitch

A Minecraft Bukkit/Paper plugin that allows players and server administrators to seamlessly create, save, and switch between separate worlds on the same server, with full multi-dimension support.

## Features
- Save and load different worlds on a single server instance.
- Automatically handles Overworld, Nether, and The End for each save.
- Keeps track of your exact location, inventory, and flying state per-save. 
- Prevents items or states bleeding between different saves.

## Commands
- `/ss create <name> [seed]` - Creates and loads a brand new world save, with an optional seed.
- `/ss save <name>` - Saves your current world as a new copy (Save-As).
- `/ss save` - Manually triggers a save for your current world.
- `/ss load <name>` - Unloads the current world and loads a saved world.
- `/ss list` - Lists all available saved worlds.

## Permissions
- `simpleswitch.admin` - Gives access to the `/ss` command.

## Building
This project uses Gradle. To build the plugin, run:
```sh
./gradlew build
```
The compiled jar will be in `build/libs/`.
