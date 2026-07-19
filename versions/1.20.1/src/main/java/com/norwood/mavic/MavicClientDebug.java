package com.norwood.mavic;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

public class MavicClientDebug {

    private static final Logger LOG = LogUtils.getLogger();

    private boolean hadDrone;
    private boolean hadChunk;
    private long ticks;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !MavicConfig.verboseLogging()) {
            return;
        }
        this.ticks++;
        Minecraft mc = Minecraft.getInstance();
        Entity drone = MavicClient.getViewedDrone();
        boolean nowDrone = drone != null;
        boolean nowChunk = false;
        ChunkPos dc = null;
        int loaded = -1;
        if (mc.level != null) {
            loaded = mc.level.getChunkSource().getLoadedChunksCount();
            if (nowDrone) {
                dc = drone.chunkPosition();
                nowChunk = mc.level.hasChunk(dc.x, dc.z);
            }
        }

        if (nowDrone != this.hadDrone) {
            LOG.info("[Mavic/client] viewed-drone {}", nowDrone
                    ? "PRESENT @ " + dc + " (camera basis)" : "GONE (client lost drone entity)");
            this.hadDrone = nowDrone;
        }
        if (nowDrone && nowChunk != this.hadChunk) {
            LOG.info("[Mavic/client] drone chunk {} client-loaded={} loadedChunks={}", dc, nowChunk, loaded);
            this.hadChunk = nowChunk;
        }
        if (nowDrone && this.ticks % 40 == 0) {
            LOG.info("[Mavic/client] heartbeat: drone @ {} chunkLoaded={} loadedChunks={} camType={}",
                    dc, nowChunk, loaded, mc.options.getCameraType());
        }
    }
}
