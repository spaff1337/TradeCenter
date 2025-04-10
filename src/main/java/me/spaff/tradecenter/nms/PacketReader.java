package me.spaff.tradecenter.nms;

import io.netty.channel.*;
import me.spaff.tradecenter.Main;
import me.spaff.tradecenter.tradecenter.TradeCenter;
import me.spaff.tradecenter.utils.ReflectionUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.UUID;

public class PacketReader {
    private static Channel channel;

    public static void injectPlayer(Player player) {
        UUID uuid = player.getUniqueId();

        ChannelDuplexHandler channelDuplexHandler = new ChannelDuplexHandler() {
            @Override
            public void channelRead(ChannelHandlerContext channelHandlerContext, Object packet) throws Exception {
                super.channelRead(channelHandlerContext, packet);
            }

            @Override
            public void write(ChannelHandlerContext channelHandlerContext, Object packet, ChannelPromise promise) throws Exception {
                // Listening for a chunk with light packet, so we can check what chunk what player
                // is loading, and spawn trade center models for that player
                if (packet instanceof ClientboundLevelChunkWithLightPacket lightPacket) {
                    // Construct a key from the chunk coordinates in the packet
                    String chunkKey = lightPacket.getX() + "-" + lightPacket.getZ();
                    List<Location> tradeCenterLocations = me.spaff.tradecenter.tradecenter.DisplayLocationCache.getDisplayLocationsByChunk(chunkKey);
                    
                    // If there is no registered trade center in this chunk, skip processing
                    if (tradeCenterLocations != null && !tradeCenterLocations.isEmpty()) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                for (Location loc : tradeCenterLocations) {
                                    if (TradeCenter.isTradeCenter(loc.getBlock())) {
                                        TradeCenter tradeCenter = new TradeCenter(loc);
                                        tradeCenter.clearModel(player);
                                        tradeCenter.spawnModel(player);
                                    } else {
                                        me.spaff.tradecenter.tradecenter.DisplayLocationCache.removeDisplayLocation(loc);
                                    }
                                }
                            }
                        }.runTaskLater(Main.getInstance(), 5);
                    }
                }
                super.write(channelHandlerContext, packet, promise);
            }
        };

        // ServerCommonPacketListenerImpl: e -> connection
        // Connection: n -> channel

        Connection conn = null;
        
        // Get player connection
        ServerCommonPacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;

        // Get connection field
        conn = (Connection) ReflectionUtils.getField(ServerCommonPacketListenerImpl.class, "e", connection);

        // Get channel field
        channel = (Channel) ReflectionUtils.getField(Connection.class, "n", conn);

        String pipelineName = Main.getInstance().getName() + uuid.toString();
        ChannelPipeline pipeline = channel.pipeline();

        if (isInjected(player))
            pipeline.remove(pipelineName);

        pipeline.addBefore("packet_handler", pipelineName, channelDuplexHandler);
    }

    public static void uninjectPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        if (channel == null)
            return;

        String pipelineName = Main.getInstance().getName() + uuid.toString();

        channel.eventLoop().submit(() -> {
            if (channel.pipeline().get(pipelineName) != null)
                channel.pipeline().remove(pipelineName);
            return null;
        });
    }

    public static void pipelineHandler(Player player) {
        if (isInjected(player))
            uninjectPlayer(player);
        else
            injectPlayer(player);
    }

    public static void pipelineHandler() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            pipelineHandler(player);
        }
    }

    private static boolean isInjected(Player player) {
        UUID uuid = player.getUniqueId();
        String pipelineName = Main.getInstance().getName() + uuid.toString();

        if (channel == null)
            return false;

        ChannelPipeline pipeline = channel.pipeline();
        if (pipeline.get(pipelineName) != null)
            return true;

        return false;
    }
}