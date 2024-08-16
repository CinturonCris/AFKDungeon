package c.cris.AFKDungeon.AFKDungeon;

import c.cris.AFKDungeon.AFKDungeon.main.AFKDungeon;
import c.cris.AFKDungeon.AFKDungeon.utils.DropConfig;
import c.cris.AFKDungeon.AFKDungeon.utils.MobConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MobDeathListener implements Listener {

    private final AFKDungeon plugin;
    private final Random random;
    private final Map<Entity, Player> lastDamagerMap = new HashMap<>();

    public MobDeathListener(AFKDungeon plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof LivingEntity) {
            Player damager = (Player) event.getDamager();
            LivingEntity entity = (LivingEntity) event.getEntity();
            lastDamagerMap.put(entity, damager);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = (LivingEntity) event.getEntity();
        Player killer = lastDamagerMap.get(entity);

        // Remove the entry after the entity dies
        lastDamagerMap.remove(entity);

        if (killer != null) {
            EntityType entityType = entity.getType();
            String entityTypeString = entityType.toString().toLowerCase();

            Map<String, MobConfig> mobsConfig = plugin.getConfigManager().getMobsConfig();
            MobConfig mobConfig = mobsConfig.get(entityTypeString);

            if (mobConfig != null && isWithinSpawnLocations(entity.getLocation())) {
                // Cancel default drops
                event.getDrops().clear();

                // Apply custom drops according to configuration
                Map<String, DropConfig> drops = mobConfig.getDrops();
                for (Map.Entry<String, DropConfig> entry : drops.entrySet()) {
                    DropConfig dropConfig = entry.getValue();
                    Material material = Material.getMaterial(dropConfig.getId());
                    int chance = dropConfig.getChance();

                    if (material != null && random.nextInt(100) < chance) {
                        killer.getInventory().addItem(new ItemStack(material));
                    }
                }

                // Apply commands if specified
                if (mobConfig.getCommands() != null) {
                    for (String command : mobConfig.getCommands()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{player}", killer.getName()));
                    }
                }
            }
        }
    }

    private boolean isWithinSpawnLocations(Location location) {
        ConfigurationSection spawnLocations = plugin.getConfig().getConfigurationSection("spawn-locations");
        if (spawnLocations == null) {
            return false;
        }

        for (String key : spawnLocations.getKeys(false)) {
            ConfigurationSection locConfig = spawnLocations.getConfigurationSection(key);
            if (locConfig == null) continue;

            String worldName = locConfig.getString("world");
            double x = locConfig.getDouble("x");
            double y = locConfig.getDouble("y");
            double z = locConfig.getDouble("z");

            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            Location spawnLocation = new Location(world, x, y, z);
            if (location.getWorld().equals(spawnLocation.getWorld()) && location.distance(spawnLocation) <= 500) {
                return true;
            }
        }

        return false;
    }
}
