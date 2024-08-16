package c.cris.AFKDungeon.AFKDungeon.commands;

import c.cris.AFKDungeon.AFKDungeon.main.AFKDungeon;
import c.cris.AFKDungeon.AFKDungeon.utils.ColorUtil;
import c.cris.AFKDungeon.AFKDungeon.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class AFKCommand implements CommandExecutor, Listener {

    private final AFKDungeon plugin;
    private final MessageManager messageManager;
    private final Random random = new Random();

    private final List<Location> predefinedLocations = new ArrayList<>();

    private final Map<Player, Location> playerAFKStatus = new HashMap<>();
    private final Map<Player, Long> playerLastCommandTime = new HashMap<>();
    private final Map<Player, Long> playerLastMovementTime = new HashMap<>();
    private final Map<Player, Location> playerOriginalLocations = new HashMap<>();
    private static final long COMMAND_COOLDOWN = 1000L;
    private static final long AFK_TIMEOUT = 5 * 60 * 1000L;
    private final Set<Player> afkTeleportedPlayers = new HashSet<>();

    public AFKCommand(AFKDungeon plugin) {
        this.plugin = plugin;
        this.messageManager = new MessageManager();
        loadPredefinedLocations();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        loadAFKData();

        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                for (Map.Entry<Player, Long> entry : playerLastMovementTime.entrySet()) {
                    Player player = entry.getKey();
                    long lastMovementTime = entry.getValue();
                    if (currentTime - lastMovementTime >= AFK_TIMEOUT && !afkTeleportedPlayers.contains(player)) {
                        Location originalLocation = player.getLocation();
                        playerOriginalLocations.put(player, originalLocation);

                        Location randomLocation = getRandomPredefinedLocation();
                        if (randomLocation != null) {
                            markPlayerAFK(player, originalLocation, currentTime);
                            player.teleport(randomLocation);
                            String afkTimeoutMessage = MessageManager.getMessage("afk-command.timeout");
                            player.sendMessage(ColorUtil.reformatRGB(afkTimeoutMessage));

                            afkTeleportedPlayers.add(player);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (isPlayerAFK(player)) {
            if (!hasMoved(player, playerAFKStatus.get(player))) {
                String message = MessageManager.getMessage("afk-command.already-afk");
                player.sendMessage(ColorUtil.reformatRGB(message));
                return true;
            } else {
                playerAFKStatus.remove(player);
                playerLastCommandTime.remove(player);
                playerLastMovementTime.remove(player);

                Location storedLocation = playerOriginalLocations.get(player);
                if (storedLocation != null) {
                    player.teleport(storedLocation);
                    String afkReturnedMessage = MessageManager.getMessage("afk-command.returned");
                    player.sendMessage(ColorUtil.reformatRGB(afkReturnedMessage));
                    removePlayerAFKData(player);
                }
                return true;
            }
        }

        Location originalLocation = player.getLocation();
        playerOriginalLocations.put(player, originalLocation);

        Location randomLocation = getRandomPredefinedLocation();
        if (randomLocation == null) {
            String message = MessageManager.getMessage("afk-command.no-locations");
            player.sendMessage(ColorUtil.reformatRGB(message));
            return true;
        }
        markPlayerAFK(player, originalLocation, System.currentTimeMillis());
        player.teleport(randomLocation);
        String afkStartMessage = MessageManager.getMessage("afk-command.start");
        player.sendMessage(ColorUtil.reformatRGB(afkStartMessage));

        return true;
    }


    private boolean isPlayerAFK(Player player) {
        return playerAFKStatus.containsKey(player);
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (playerAFKStatus.containsKey(player)) {
            long lastCommandTime = playerLastCommandTime.getOrDefault(player, 0L);
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCommandTime < COMMAND_COOLDOWN) {
                return;
            }
            playerLastCommandTime.put(player, currentTime);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!playerAFKStatus.containsKey(player)) {
            return;
        }

        playerLastMovementTime.put(player, System.currentTimeMillis());

        if (hasMoved(player, playerAFKStatus.get(player))) {
            playerAFKStatus.remove(player);
            playerLastCommandTime.remove(player);
            playerLastMovementTime.remove(player);

            Location storedLocation = playerOriginalLocations.get(player);
            if (storedLocation != null) {
                player.teleport(storedLocation);
                String afkReturnedMessage = MessageManager.getMessage("afk-command.returned");
                player.sendMessage(ColorUtil.reformatRGB(afkReturnedMessage));
                removePlayerAFKData(player);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FileConfiguration afkDataConfig = plugin.getAFKDataConfig();

        if (afkDataConfig.contains(player.getUniqueId().toString())) {
            World world = Bukkit.getWorld(afkDataConfig.getString(player.getUniqueId() + ".original-location.world"));
            double x = afkDataConfig.getDouble(player.getUniqueId() + ".original-location.x");
            double y = afkDataConfig.getDouble(player.getUniqueId() + ".original-location.y");
            double z = afkDataConfig.getDouble(player.getUniqueId() + ".original-location.z");
            float yaw = (float) afkDataConfig.getDouble(player.getUniqueId() + ".original-location.yaw");
            float pitch = (float) afkDataConfig.getDouble(player.getUniqueId() + ".original-location.pitch");

            Location originalLocation = new Location(world, x, y, z, yaw, pitch);
            long lastMovementTime = afkDataConfig.getLong(player.getUniqueId() + ".last-movement-time");

            playerOriginalLocations.put(player, originalLocation);
            playerLastMovementTime.put(player, lastMovementTime);
            markPlayerAFK(player, originalLocation, lastMovementTime);
        }
    }

    private void markPlayerAFK(Player player, Location originalLocation, long lastMovementTime) {
        if (originalLocation == null) {
            plugin.getLogger().warning("Original location is null for player: " + player.getName());
            return;
        }

        playerAFKStatus.put(player, originalLocation);
        playerLastCommandTime.put(player, System.currentTimeMillis());
        playerLastMovementTime.put(player, lastMovementTime);

        FileConfiguration afkDataConfig = plugin.getAFKDataConfig();
        afkDataConfig.set(player.getUniqueId() + ".original-location.world", originalLocation.getWorld().getName());
        afkDataConfig.set(player.getUniqueId() + ".original-location.x", originalLocation.getX());
        afkDataConfig.set(player.getUniqueId() + ".original-location.y", originalLocation.getY());
        afkDataConfig.set(player.getUniqueId() + ".original-location.z", originalLocation.getZ());
        afkDataConfig.set(player.getUniqueId() + ".original-location.yaw", originalLocation.getYaw());
        afkDataConfig.set(player.getUniqueId() + ".original-location.pitch", originalLocation.getPitch());
        afkDataConfig.set(player.getUniqueId() + ".last-movement-time", lastMovementTime);
        plugin.saveAFKDataConfig();
    }

    private void removePlayerAFKData(Player player) {
        FileConfiguration afkDataConfig = plugin.getAFKDataConfig();
        afkDataConfig.set(player.getUniqueId().toString(), null);
        plugin.saveAFKDataConfig();
        playerOriginalLocations.remove(player);
        playerLastMovementTime.remove(player);
    }

    private boolean hasMoved(Player player, Location location) {
        Location currentLocation = player.getLocation();
        return !currentLocation.getWorld().equals(location.getWorld())
                || currentLocation.distanceSquared(location) > 0.01;
    }

    private void loadPredefinedLocations() {
        World world = Bukkit.getWorld("SpawnWorld");
        if (world == null) {
            plugin.getLogger().severe("World 'SpawnWorld' not found. Please check your configuration.");
            return;
        }

        predefinedLocations.add(new Location(world, -921, 29, 17));
        predefinedLocations.add(new Location(world, -922, 29, -5));
        predefinedLocations.add(new Location(world, -910, 29, -5));
        predefinedLocations.add(new Location(world, -921, 29, -19));

        predefinedLocations.add(new Location(world, -915, 29, 27));
        predefinedLocations.add(new Location(world, -914, 29, 40));
        predefinedLocations.add(new Location(world, -934, 29, 30));
        predefinedLocations.add(new Location(world, -935, 29, 39));

        predefinedLocations.add(new Location(world, -919, 29, 49));
        predefinedLocations.add(new Location(world, -915, 29, 71));
        predefinedLocations.add(new Location(world, -931, 29, 99));
        predefinedLocations.add(new Location(world, -904, 28, 12));

        predefinedLocations.add(new Location(world, -888, 29, -1));
        predefinedLocations.add(new Location(world, -932, 29, 49));

    }
    private void loadAFKData() {
        FileConfiguration afkDataConfig = plugin.getAFKDataConfig();
        for (String key : afkDataConfig.getKeys(false)) {
            UUID playerUUID = UUID.fromString(key);
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null || !player.isOnline()) {
                continue;
            }

            World world = Bukkit.getWorld(afkDataConfig.getString(key + ".original-location.world"));
            double x = afkDataConfig.getDouble(key + ".original-location.x");
            double y = afkDataConfig.getDouble(key + ".original-location.y");
            double z = afkDataConfig.getDouble(key + ".original-location.z");
            float yaw = (float) afkDataConfig.getDouble(key + ".original-location.yaw");
            float pitch = (float) afkDataConfig.getDouble(key + ".original-location.pitch");

            Location originalLocation = new Location(world, x, y, z, yaw, pitch);
            long lastMovementTime = afkDataConfig.getLong(key + ".last-movement-time");

            playerOriginalLocations.put(player, originalLocation);
            playerLastMovementTime.put(player, lastMovementTime);
            markPlayerAFK(player, originalLocation, lastMovementTime);
        }
    }

    private Location getRandomPredefinedLocation() {
        if (predefinedLocations.isEmpty()) {
            return null;
        }
        return predefinedLocations.get(random.nextInt(predefinedLocations.size()));
    }
}
