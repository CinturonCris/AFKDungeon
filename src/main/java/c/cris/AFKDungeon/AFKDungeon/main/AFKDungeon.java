package c.cris.AFKDungeon.AFKDungeon.main;

import c.cris.AFKDungeon.AFKDungeon.MobDeathListener;
import c.cris.AFKDungeon.AFKDungeon.commands.AFKCommand;
import c.cris.AFKDungeon.AFKDungeon.commands.ReloadCommand;
import c.cris.AFKDungeon.AFKDungeon.tasks.MobSpawnTask;
import c.cris.AFKDungeon.AFKDungeon.utils.ColorUtil;
import c.cris.AFKDungeon.AFKDungeon.utils.ConfigManager;
import c.cris.AFKDungeon.AFKDungeon.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class AFKDungeon extends JavaPlugin {

    private ConfigManager configManager;
    private File afkDataFile;
    private FileConfiguration afkDataConfig;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        MessageManager.setupMessages(this);

        getServer().getPluginManager().registerEvents(new MobDeathListener(this), this);

        int spawnInterval = configManager.getSpawnInterval() * 20; // Convert to ticks
        new MobSpawnTask(this, configManager).runTaskTimer(this, 0, spawnInterval);
        Bukkit.getPluginManager().registerEvents(new AFKCommand(this), this);

        this.getCommand("afk").setExecutor(new AFKCommand(this));
        this.getCommand("afkdungeon").setExecutor(new ReloadCommand(this));

        getLogger().info("AFKDungeon enabled!");
    }

    @Override
    public void onDisable() {
        cleanupMobs();
        getLogger().info("AFKDungeon disabled!");
    }

    @Override
    public void onLoad() {
        createAFKDataFile();
    }

    public void reloadConfigs() {
        reloadConfig();
        configManager.setupConfig();
        MessageManager.reloadMessages();
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    private void cleanupMobs() {
        List<Location> spawnLocations = configManager.getSpawnLocations();
        double radius = 400.0;

        for (Location spawnLocation : spawnLocations) {
            if (spawnLocation != null && spawnLocation.getWorld() != null) {
                for (Entity entity : spawnLocation.getWorld().getNearbyEntities(spawnLocation, radius, radius, radius)) {
                    if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                        LivingEntity livingEntity = (LivingEntity) entity;
                        if (livingEntity.getCustomName() != null) {
                            livingEntity.remove();
                        }
                    }
                }
            }
        }
    }
    private void createAFKDataFile() {
        afkDataFile = new File(getDataFolder(), "afk-data.yml");
        if (!afkDataFile.exists()) {
            afkDataFile.getParentFile().mkdirs();
            saveResource("afk-data.yml", false);
        }
        afkDataConfig = YamlConfiguration.loadConfiguration(afkDataFile);
    }

    public FileConfiguration getAFKDataConfig() {
        return this.afkDataConfig;
    }

    public void saveAFKDataConfig() {
        try {
            afkDataConfig.save(afkDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
