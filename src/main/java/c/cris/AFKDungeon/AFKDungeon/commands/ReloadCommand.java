package c.cris.AFKDungeon.AFKDungeon.commands;

import c.cris.AFKDungeon.AFKDungeon.main.AFKDungeon;
import c.cris.AFKDungeon.AFKDungeon.utils.ColorUtil;
import c.cris.AFKDungeon.AFKDungeon.utils.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    private final AFKDungeon plugin;

    public ReloadCommand(AFKDungeon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfigs();
            String successMessage = MessageManager.getMessage("reload.success");
            sender.sendMessage(ColorUtil.reformatRGB(successMessage));
            return true;
        }
        return false;
    }
}
