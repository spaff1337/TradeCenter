package me.spaff.tradecenter.listener;

import me.spaff.tradecenter.config.Config;
import me.spaff.tradecenter.tradecenter.DisplayLocationCache;
import me.spaff.tradecenter.tradecenter.TradeCenter;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.Plugin;

public class ServerListener implements Listener {
    private final Plugin plugin;
    public DisplayLocationCache displayCache;

    public ServerListener(Plugin plugin, DisplayLocationCache displayCache) {
        this.plugin = plugin;
        this.displayCache = displayCache;
    }

    @EventHandler
    public void onBlockPistonExtendEvent(BlockPistonExtendEvent e) {
        for (Block block : e.getBlocks()) {
            if (!TradeCenter.isTradeCenter(block)) continue;
            e.setCancelled(true);
            break;
        }
    }

    @EventHandler
    public void onBlockPistonRetractEvent(BlockPistonRetractEvent e) {
        for (Block block : e.getBlocks()) {
            if (!TradeCenter.isTradeCenter(block)) continue;
            e.setCancelled(true);
            break;
        }
    }

    @EventHandler
    public void onEntityExplodeEvent(EntityExplodeEvent e) {
        for (Block block : e.blockList()) {
            if (!TradeCenter.isTradeCenter(block)) continue;
            new TradeCenter(block.getLocation()).clearData();
        } 
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        Integer radius = Config.readInt("trade-center.display-cache.chunk-listener-radius");
        if (radius==null || radius<0) return;

        Chunk centerChunk = e.getChunk();
        boolean pass = false;
        for (Entity entity : centerChunk.getEntities()) {
            if (entity instanceof Player) {
                pass = true;
                break;
            }
        } if (!pass) return;

        final int scanRadius = radius;
        World world = centerChunk.getWorld();
        int centerX = centerChunk.getX();
        int centerZ = centerChunk.getZ();
                
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Iterate over all chunks within the specified radius
            for (int xOffset = -scanRadius; xOffset <= scanRadius; xOffset++) {
                for (int zOffset = -scanRadius; zOffset <= scanRadius; zOffset++) {
                    Chunk currentChunk = world.getChunkAt(centerX + xOffset, centerZ + zOffset);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        displayCache.scanChunkForTradeCenters(world, currentChunk);
                    }, 1);
                }
            }
        }, 1);
    }
}