package com.norwood.mavic;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod("sbw_mavic_extender")
public class MavicForge {

    private static final Logger LOG = LogUtils.getLogger();

    public MavicForge() {
        MinecraftForge.EVENT_BUS.register(new DroneChunkStreamer());
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MavicConfig.COMMON_SPEC);
        LOG.info("[Mavic] initialized: DroneChunkStreamer registered on the game event bus (Forge)");
    }
}
