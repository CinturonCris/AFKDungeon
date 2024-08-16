package c.cris.AFKDungeon.AFKDungeon.utils;

import c.cris.AFKDungeon.AFKDungeon.main.AFKDungeon;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class MessageManager {

    private static File messagesFile;
    private static FileConfiguration messagesConfig;

    public static void setupMessages(AFKDungeon plugin) {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public static void reloadMessages() {
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public static String getMessage(String key) {
        if (messagesConfig == null) {
            return "Message configuration not loaded.";
        }

        String message = messagesConfig.getString("messages." + key);
        if (message != null) {
            return ChatColor.translateAlternateColorCodes('&', message);
        }
        return "Message not found for key: " + key;
    }

    public static void saveMessages() {
        try {
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
