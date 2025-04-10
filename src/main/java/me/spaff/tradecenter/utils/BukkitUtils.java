package me.spaff.tradecenter.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

public class BukkitUtils {
    public static void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(StringUtils.getColoredText(message));
    }

    public static boolean isSameLocation(Location loc1, Location loc2) {
        // Loc1
        double x1 = loc1.getX();
        double y1 = loc1.getY();
        double z1 = loc1.getZ();

        World world1 = loc1.getWorld();

        // Loc2
        double x2 = loc1.getX();
        double y2 = loc1.getY();
        double z2 = loc1.getZ();

        World world2 = loc2.getWorld();

        if (x1 == x2 && y1 == y2 && z1 == z2 && world1.getName().equals(world2.getName()))
            return true;
        return false;
    }
}
