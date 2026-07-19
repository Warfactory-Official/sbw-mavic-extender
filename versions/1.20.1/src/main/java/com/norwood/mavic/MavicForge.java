package com.norwood.mavic;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod("sbw_mavic_extender")
public class MavicForge {

    private static final Logger LOG = LogUtils.getLogger();

    public MavicForge() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
        MinecraftForge.EVENT_BUS.register(new DroneChunkStreamer());
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MavicConfig.COMMON_SPEC);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            MinecraftForge.EVENT_BUS.register(new MavicClientDebug());
        }
        LOG.info("[Mavic] initialized: DroneChunkStreamer registered on the game event bus (Forge)");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(MavicAddonCompat::applyOverrides);
    }
}
