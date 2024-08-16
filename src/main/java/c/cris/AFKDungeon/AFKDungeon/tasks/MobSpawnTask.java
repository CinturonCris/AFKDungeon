package c.cris.AFKDungeon.AFKDungeon.tasks;

import c.cris.AFKDungeon.AFKDungeon.main.AFKDungeon;
import c.cris.AFKDungeon.AFKDungeon.utils.ColorUtil;
import c.cris.AFKDungeon.AFKDungeon.utils.ConfigManager;
import c.cris.AFKDungeon.AFKDungeon.utils.DropConfig;
import c.cris.AFKDungeon.AFKDungeon.utils.MobConfig;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.Material;
import org.bukkit.entity.Enderman;
import java.util.*;

public class MobSpawnTask extends BukkitRunnable {

    private final AFKDungeon plugin;
    private final ConfigManager configManager;
    private final Random random = new Random();
    private final List<Location> spawnLocations;

    private static int totalMobCount = 0;
    private static final int MAX_MOBS = 60;
    private static final int MAX_MOBS_PER_LOCATION = 7;

    public MobSpawnTask(AFKDungeon plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.spawnLocations = configManager.getSpawnLocations();
    }

    @Override
    public void run() {
        if (totalMobCount >= MAX_MOBS) {
            plugin.getLogger().info("Mob limit reached. No more mobs will be spawned.");
            return;
        }

        for (Location spawnLocation : spawnLocations) {
            if (spawnLocation != null && spawnLocation.getWorld() != null) {
                if (random.nextInt(80) < 40) {
                    spawnMobs(spawnLocation);
                }
            } else {
                plugin.getLogger().warning("Invalid spawn location: " + spawnLocation);
            }
        }
        cleanUpMobs();
    }

    private void spawnMobs(Location spawnLocation) {
        int playerCount = getNearbyPlayerCount(spawnLocation, 400);
        double spawnProbability = (playerCount <= 2) ? 10 : 30;
        int maxMobsToSpawn = Math.min((playerCount <= 2) ? 5 : Integer.MAX_VALUE, MAX_MOBS_PER_LOCATION);
        if (!isPlayerNearby(spawnLocation, 500)) {
            return;
        }

        Map<String, MobConfig> mobsConfig = configManager.getMobsConfig();
        for (Map.Entry<String, MobConfig> entry : mobsConfig.entrySet()) {
            String mobType = entry.getKey();
            MobConfig mobConfig = entry.getValue();
            if (random.nextInt(100) < mobConfig.getChance() * (spawnProbability / 100.0)) {
                int numberOfMobsToSpawn = Math.min(random.nextInt(3) + 1, maxMobsToSpawn);
                for (int i = 0; i < numberOfMobsToSpawn; i++) {
                    if (totalMobCount >= MAX_MOBS) {
                        return;
                    }

                    Location loc = spawnLocation.clone();
                    EntityType entityType = EntityType.valueOf(mobType.toUpperCase());
                    LivingEntity entity = (LivingEntity) loc.getWorld().spawnEntity(loc, entityType);
                    setupEntity(entity, mobConfig);
                    totalMobCount++;
                }
            }
        }
    }

    private int getNearbyPlayerCount(Location location, double radius) {
        int count = 0;
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distance(location) <= radius) {
                count++;
            }
        }
        return count;
    }

    private boolean isPlayerNearby(Location location, double radius) {
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distance(location) <= radius) {
                return true;
            }
        }
        return false;
    }

    private void setupEntity(LivingEntity entity, MobConfig mobConfig) {
        if (entity == null) {
            plugin.getLogger().warning("Entity is null, skipping setup.");
            return;
        }

        String formattedName = ColorUtil.reformatRGB(mobConfig.getName());
        entity.setCustomName(formattedName);
        entity.setCustomNameVisible(true);

        entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(1.0);
        entity.setHealth(1.0);

        if (entity instanceof Mob) {
            Mob mob = (Mob) entity;
            mob.setAI(true);
            mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.1);
        }

        entity.getEquipment().setHelmet(new ItemStack(Material.AIR));
        entity.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
        entity.getEquipment().setItemInOffHand(new ItemStack(Material.AIR));
        entity.getEquipment().setChestplate(new ItemStack(Material.AIR));
        entity.getEquipment().setLeggings(new ItemStack(Material.AIR));
        entity.getEquipment().setBoots(new ItemStack(Material.AIR));

        if (entity instanceof Zombie) {
            Zombie zombie = (Zombie) entity;
            zombie.setBaby(false);
            zombie.setCanPickupItems(false);
            zombie.setRemoveWhenFarAway(false);
            zombie.getEquipment().setHelmet(createColoredLeatherHelmet());
        } else if (entity instanceof Skeleton) {
            Skeleton skeleton = (Skeleton) entity;
            skeleton.getEquipment().setHelmet(createColoredLeatherHelmet());
        } else if (entity instanceof Enderman) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 0, false, false));
        }

        applyDrops(entity, mobConfig.getDrops());

        startMobBehavior(entity);

        removeNearbyUnnamedMobs(entity, 500);
    }

    private void applyDrops(LivingEntity entity, Map<String, DropConfig> drops) {
        if (entity == null) {
            return;
        }

        // Clear all item entities near the mob
        List<Entity> nearbyEntities = new ArrayList<>(entity.getWorld().getNearbyEntities(entity.getLocation(), 5, 5, 5));
        for (Entity nearbyEntity : nearbyEntities) {
            if (nearbyEntity instanceof Item) {
                nearbyEntity.remove();
            }
        }

        // Apply custom drops
        for (DropConfig dropConfig : drops.values()) {
            if (random.nextInt(100) < dropConfig.getChance()) {
                if (dropConfig.getType().equalsIgnoreCase("item")) {
                    Material material = Material.getMaterial(dropConfig.getId());
                    if (material != null) {
                        ItemStack itemStack = new ItemStack(material);
                        entity.getWorld().dropItemNaturally(entity.getLocation(), itemStack);
                    } else {
                        plugin.getLogger().warning("Invalid material ID: " + dropConfig.getId());
                    }
                } else if (dropConfig.getType().equalsIgnoreCase("command")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), dropConfig.getCommand().replace("{player}", entity.getKiller() != null ? entity.getKiller().getName() : "unknown"));
                }
            }
        }
    }

    private ItemStack createColoredLeatherHelmet() {
        DyeColor[] colors = {DyeColor.RED, DyeColor.BLUE, DyeColor.GREEN, DyeColor.YELLOW, DyeColor.PURPLE, DyeColor.WHITE};
        DyeColor color = colors[random.nextInt(colors.length)];

        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta meta = (LeatherArmorMeta) helmet.getItemMeta();
        if (meta != null) {
            meta.setColor(color.getColor());
            helmet.setItemMeta(meta);
        } else {
            plugin.getLogger().warning("Failed to create LeatherArmorMeta for helmet");
        }

        return helmet;
    }

    private void startMobBehavior(LivingEntity entity) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (entity.isDead()) {
                    totalMobCount--;
                    cancel();
                    return;
                }

                Player nearestPlayer = getNearestPlayer(entity);
                if (nearestPlayer != null) {
                    ((Mob) entity).setTarget(nearestPlayer);

                    double distance = entity.getLocation().distance(nearestPlayer.getLocation());

                    if (distance <= 5) {
                        nearestPlayer.damage(1.0, entity);
                        nearestPlayer.setVelocity(new Vector(0, 0, 0));
                        entity.getWorld().spawnParticle(Particle.FLAME, entity.getLocation(), 80);
                        entity.getWorld().playSound(entity.getLocation(), Sound.ITEM_DYE_USE, 1.0f, 1.0f);
                        nearestPlayer.swingMainHand();
                        applyDropsToPlayerInventory(nearestPlayer, entity);
                        entity.setHealth(0);
                    }
                } else {
                    entity.getWorld().spawnParticle(Particle.FLAME, entity.getLocation(), 80);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }


    private Player getNearestPlayer(LivingEntity entity) {
        Player nearestPlayer = null;
        double closestDistance = Double.MAX_VALUE;

        for (Player player : entity.getWorld().getPlayers()) {
            double distance = player.getLocation().distance(entity.getLocation());
            if (distance < closestDistance) {
                closestDistance = distance;
                nearestPlayer = player;
            }
        }

        return nearestPlayer;
    }

    private void applyDropsToPlayerInventory(Player player, LivingEntity entity) {
        if (entity == null) {
            return;
        }

        Map<String, DropConfig> drops = configManager.getMobsConfig().get(entity.getType().name().toLowerCase()).getDrops();
        for (DropConfig dropConfig : drops.values()) {
            if (random.nextInt(100) < dropConfig.getChance()) {
                if (dropConfig.getType().equalsIgnoreCase("item")) {
                    Material material = Material.getMaterial(dropConfig.getId());
                    if (material != null) {
                        ItemStack itemStack = new ItemStack(material);
                        player.getInventory().addItem(itemStack);
                    } else {
                        plugin.getLogger().warning("Invalid material ID: " + dropConfig.getId());
                    }
                } else if (dropConfig.getType().equalsIgnoreCase("command")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), dropConfig.getCommand().replace("{player}", player.getName()));
                }
            }
        }
    }

    private void cleanUpMobs() {
        for (Entity entity : Bukkit.getWorlds().get(0).getEntities()) {
            if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                LivingEntity livingEntity = (LivingEntity) entity;
                if (livingEntity.getHealth() <= 0) {
                    livingEntity.remove();
                    totalMobCount--;
                }
            }
        }
    }

    private void removeNearbyUnnamedMobs(Entity entity, double radius) {
        for (Entity nearbyEntity : entity.getNearbyEntities(radius, radius, radius)) {
            if (nearbyEntity instanceof LivingEntity && !(nearbyEntity instanceof Player)) {
                LivingEntity livingEntity = (LivingEntity) nearbyEntity;
                if (livingEntity.getCustomName() == null) {
                    livingEntity.remove();
                    totalMobCount--;
                }
            }
        }
    }
}
