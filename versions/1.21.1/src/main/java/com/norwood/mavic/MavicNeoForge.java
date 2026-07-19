package com.norwood.mavic;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod("sbw_mavic_extender")
public class MavicNeoForge {

    private static final Logger LOG = LogUtils.getLogger();

    public MavicNeoForge(IEventBus modEventBus, ModContainer container) {
        NeoForge.EVENT_BUS.register(new DroneChunkStreamer());
        container.registerConfig(ModConfig.Type.COMMON, MavicConfig.COMMON_SPEC);
        LOG.info("[Mavic] initialized: DroneChunkStreamer registered on the game event bus (NeoForge)");
    }
}
