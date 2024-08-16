package c.cris.AFKDungeon.AFKDungeon.utils;

import c.cris.AFKDungeon.AFKDungeon.main.AFKDungeon;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final AFKDungeon plugin;

    public ConfigManager(AFKDungeon plugin) {
        this.plugin = plugin;
        setupConfig();
    }

    public void setupConfig() {
        plugin.saveDefaultConfig();
    }

    public int getSpawnInterval() {
        FileConfiguration config = plugin.getConfig();
        return config.getInt("spawn-interval", 25);
    }

    public List<Location> getSpawnLocations() {
        List<Location> locations = new ArrayList<>();
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection spawnSection = config.getConfigurationSection("spawn-locations");

        if (spawnSection != null) {
            for (String key : spawnSection.getKeys(false)) {
                String worldName = spawnSection.getString(key + ".world");
                double x = spawnSection.getDouble(key + ".x");
                double y = spawnSection.getDouble(key + ".y");
                double z = spawnSection.getDouble(key + ".z");
                double yaw = spawnSection.getDouble(key + ".yaw", 0);
                double pitch = spawnSection.getDouble(key + ".pitch", 0);

                if (worldName != null) {
                    Location location = new Location(Bukkit.getWorld(worldName), x, y, z, (float) yaw, (float) pitch);
                    locations.add(location);
                } else {
                    plugin.getLogger().warning("World name is null for spawn location: " + key);
                }
            }
        } else {
            plugin.getLogger().warning("No spawn locations found in config.");
        }
        return locations;
    }

    public Map<String, MobConfig> getMobsConfig() {
        Map<String, MobConfig> mobsConfig = new HashMap<>();
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection mobsSection = config.getConfigurationSection("mobs");

        if (mobsSection != null) {
            for (String key : mobsSection.getKeys(false)) {
                String mobType = key.toLowerCase();
                ConfigurationSection mobSection = mobsSection.getConfigurationSection(key);
                if (mobSection != null) {
                    int chance = mobSection.getInt("chance", 0);
                    String name = mobSection.getString("name", "");
                    name = ColorUtil.reformatRGB(name);

                    Map<String, DropConfig> drops = new HashMap<>();
                    List<String> commands = mobSection.getStringList("commands"); // Asegúrate de obtener los comandos de aquí
                    ConfigurationSection dropsSection = mobSection.getConfigurationSection("drops");

                    if (dropsSection != null) {
                        for (String dropKey : dropsSection.getKeys(false)) {
                            String type = dropsSection.getString(dropKey + ".type", "");
                            String id = dropsSection.getString(dropKey + ".id", "");
                            int dropChance = dropsSection.getInt(dropKey + ".chance", 0);
                            String command = dropsSection.getString(dropKey + ".command", "");

                            DropConfig dropConfig = new DropConfig(type, id, dropChance, command);
                            drops.put(dropKey, dropConfig);
                        }
                    }

                    MobConfig mobConfig = new MobConfig(chance, name, drops, commands);
                    mobsConfig.put(mobType, mobConfig);
                } else {
                    plugin.getLogger().warning("Invalid mob configuration section for mob: " + key);
                }
            }
        } else {
            plugin.getLogger().warning("No mobs found in config.");
        }
        return mobsConfig;
    }
}