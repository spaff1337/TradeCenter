package me.spaff.tradecenter.cmd;

import me.spaff.tradecenter.Constants;
import me.spaff.tradecenter.Main;
import me.spaff.tradecenter.TCColors;
import me.spaff.tradecenter.config.Config;
import me.spaff.tradecenter.tradecenter.DisplayLocationCache;
import me.spaff.tradecenter.tradecenter.TradeCenter;
import me.spaff.tradecenter.utils.BukkitUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class TCCommands implements CommandExecutor {
    private final String prefix = TCColors.YELLOW + "[TradeCenter] ";
    private DisplayLocationCache displayCache;

    public TCCommands(DisplayLocationCache displayCache) {
        this.displayCache = displayCache;
    }

    private void sendHelp(CommandSender sender) {
        BukkitUtils.sendMessage(sender, "");
        BukkitUtils.sendMessage(sender, TCColors.YELLOW + "                TradeCenter");
        BukkitUtils.sendMessage(sender, TCColors.BRIGHT_YELLOW + "- /tc give <player> - gives player a trade");
        BukkitUtils.sendMessage(sender, TCColors.BRIGHT_YELLOW  + "center place item.");
        BukkitUtils.sendMessage(sender, TCColors.BRIGHT_YELLOW  + "- /tc reload - reloads the config.");
        BukkitUtils.sendMessage(sender, TCColors.BRIGHT_YELLOW  + "- /tc buildcache - adds loaded trade center block locations to cache.");
        BukkitUtils.sendMessage(sender, TCColors.BRIGHT_YELLOW  + "- /tc clearcache - clears the cache.");
        BukkitUtils.sendMessage(sender, "");
    }

    private void sendPluginMessage(CommandSender sender, String message) {
        BukkitUtils.sendMessage(sender, prefix + message);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) && !(sender instanceof ConsoleCommandSender)) return false;

        if (args.length == 0) {
            sendHelp(sender);
            return false;
        }

        if (!sender.hasPermission(Constants.COMMAND_PERMISSION)) {
            BukkitUtils.sendMessage(sender, Config.readString("trade-center.message.execute-cmd-no-permission"));
            return false;
        }

        //---------------- Commands ----------------//

        if (args[0].equalsIgnoreCase("give")) {
            if (args.length == 2) {
                Player otherPlayer = Bukkit.getPlayer(args[1]);
                if (otherPlayer == null) {
                    sendPluginMessage(sender, "&cInvalid player!");
                    return false;
                }

                otherPlayer.getInventory().addItem(TradeCenter.getTradeCenterItem());
                sendPluginMessage(sender, "&7Gave " + otherPlayer.getName() + " trade center item.");
            }
            else if (sender instanceof Player) {
                Player player = (Player) sender;
                player.getInventory().addItem(TradeCenter.getTradeCenterItem());
                sendPluginMessage(sender, "&7Gave yourself trade center item.");
            }
        }
        else if (args[0].equalsIgnoreCase("reload")) {
            HashMap<String, Object> oldData = Config.getRawData();
            Config.reload();

            if (oldData == null || oldData.isEmpty() || Config.getRawData() == null || Config.getRawData().isEmpty()) {
                sendPluginMessage(sender, "&cSomething went wrong when reloading the config!");
            }
            else
                sendPluginMessage(sender, "&7Config reloaded successfully!");
        }
        else if (args[0].equalsIgnoreCase("version")) {
            sendPluginMessage(sender, "&7Version: &fv" + Main.version);
        }
        else if (args[0].equalsIgnoreCase("buildcache")) {
            sendPluginMessage(sender, "&7Building DisplayLocactionCache..!" );
            displayCache.buildCache();
        }
        else if (args[0].equalsIgnoreCase("clearcache")) {
            sendPluginMessage(sender, "&7Clearing DisplayLocactionCache..!" );
            displayCache.clearCache();
        }
        else
            sendHelp(sender);

        return true;
    }
}