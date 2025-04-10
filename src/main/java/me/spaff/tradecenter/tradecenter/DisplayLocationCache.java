package me.spaff.tradecenter.tradecenter;

import me.spaff.tradecenter.config.Config;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DisplayLocationCache {
    // Key: "chunkX-chunkZ", Value: List of trade center locations in that chunk
    private static final Map<String, List<Location>> cache = new ConcurrentHashMap<>();
    private final Plugin plugin;

    /**
     * Constructor schedules a repeating task that updates cache every 15 seconds
     */
    public DisplayLocationCache(Plugin plugin) {
        this.plugin = plugin;

        Integer seconds = Config.readInt("trade-center.display-cache.auto-reloader-timer");
        if (seconds==null || seconds<0) return;

        Integer radius = Config.readInt("trade-center.display-cache.auto-reloader-radius");
        if (radius==null || radius<0) return;

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            updateCacheForOnlinePlayers(radius);
        }, 0L, 20L*seconds);
    }

    public void updateCacheForOnlinePlayers(int radius) {
        final int scanRadius = radius;

        Set<Chunk> uniqueChunks = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Chunk centerChunk = player.getLocation().getChunk();

            World world = centerChunk.getWorld();
            int centerX = centerChunk.getX();
            int centerZ = centerChunk.getZ();

            for (int xOffset = -scanRadius; xOffset <= scanRadius; xOffset++) {
                for (int zOffset = -scanRadius; zOffset <= scanRadius; zOffset++) {
                    Chunk currentChunk = world.getChunkAt(centerX + xOffset, centerZ + zOffset);
                    uniqueChunks.add(currentChunk);
                }
            }
        }

        int delay = 1;
        for (Chunk chunk : uniqueChunks) {
            World world = chunk.getWorld();
            // Schedule each chunk scan on a slight delay to avoid blocking the main thread all at once
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                scanChunkForTradeCenters(world, chunk);
            }, delay++);
        }
    }

    public void scanChunkForTradeCenters(World world, Chunk chunk) {
        for (int x = 0; x < 16; x++) {
            for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                for (int z = 0; z < 16; z++) {
                    if (!chunk.isLoaded()) return;
                    Block block = chunk.getBlock(x, y, z);
                    if (TradeCenter.isTradeCenter(block)) {
                        addDisplayLocation(block.getLocation());
                        plugin.getLogger().info("Display @ " + chunk.getWorld().getName() + ": " + block.getX() + ", " + block.getY() + ", " + block.getZ());
                    }
                }
            }
        }
    }

    public void buildCache() {
        List<Pair<World, Chunk>> chunksToScan = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                chunksToScan.add(ImmutablePair.of(world, chunk));
            }
        }
        
        final int totalChunks = chunksToScan.size();
        final int batchSize = 4;
        final long batchDelay = 2;

        plugin.getLogger().warning("Starting building DisplayLocactionCache for " + totalChunks + " chunks!");

        Iterator<Pair<World, Chunk>> iterator = chunksToScan.iterator();
        new BukkitRunnable() {
            int ticksElapsed = 0;
            
            @Override
            public void run() {
                int count = 0;
                while (iterator.hasNext() && count < batchSize) {
                    Pair<World, Chunk> pair = iterator.next();
                    scanChunkForTradeCenters(pair.getLeft(), pair.getRight());
                    count++;
                }
                
                ticksElapsed++;
                
                if (!iterator.hasNext()) {
                    plugin.getLogger().warning("Finished building DisplayLocactionCache in ~" + ticksElapsed + " ticks!");
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, batchDelay);
    }

    public void clearCache() {
        cache.clear();
    }

    public static void addDisplayLocation(Location loc) {
        String key = getChunkKey(loc);
        cache.computeIfAbsent(key, k -> new ArrayList<>()).add(loc);
    }

    public static void removeDisplayLocation(Location loc) {
        String key = getChunkKey(loc);
        List<Location> locations = cache.get(key);
        if (locations != null) {
            locations.removeIf(savedLoc -> savedLoc.equals(loc));
            if (locations.isEmpty()) {
                cache.remove(key);
            }
        }
    }

    public static void removeDisplayLocationsByChunk(String key) {
        cache.remove(key);
    }

    public static List<Location> getDisplayLocationsByChunk(String chunkKey) {
        return cache.get(chunkKey);
    }

    // Create key from chunk coordinates
    private static String getChunkKey(Location loc) {
        return loc.getChunk().getX() + "-" + loc.getChunk().getZ();
    }
}
